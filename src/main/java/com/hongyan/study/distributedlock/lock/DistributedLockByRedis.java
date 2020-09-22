package com.hongyan.study.distributedlock.lock;

import com.hongyan.study.distributedlock.entity.RedisLockEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * redis加锁解锁的工具类
 * 借鉴于：https://www.jianshu.com/p/30d7212a2770
 * https://blog.csdn.net/long2010110/article/details/82911168
 * https://blog.csdn.net/weixin_38399962/article/details/82753763
 */
@Component
@Slf4j
public class DistributedLockByRedis {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 加锁，自旋重试三次
     *
     * @param redisLockEntity 锁实体
     * @return
     */
    public boolean lock(RedisLockEntity redisLockEntity) {
        boolean locked = false;
        int tryCount = 3;
        while (!locked && tryCount > 0) {
//            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), 2, TimeUnit.MINUTES);
            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), redisLockEntity.getTimeout(), redisLockEntity.getUnit());

            tryCount--;
            log.info("redis加锁:{},thread:{}",locked,Thread.currentThread().getName());
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                log.error("线程被中断:{}" ,Thread.currentThread().getId(), e);
                Thread.currentThread().interrupt();
            }
        }
        return locked;
    }

    /**
     * 加锁，实现一直自旋，直到抢占到锁
     * 抢占到锁分2种情况：
     *  1、上一个线程执行完后主动释放锁后被当前线程抢占到锁
     *  2、上一个线程执行后一直未主动释放锁，则等待redis超时时间自动到期后被当前线程抢占到
     * @param redisLockEntity
     * @return
     */
    public boolean lockSpin(RedisLockEntity redisLockEntity) {
        boolean locked = false;
        while (!locked) {
//            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), 2, TimeUnit.MINUTES);
            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), redisLockEntity.getTimeout(), redisLockEntity.getUnit());
            log.info("redis加锁:{},thread:{}",locked,Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("线程被中断:{}" ,Thread.currentThread().getId(), e);
                Thread.currentThread().interrupt();
            }
        }
        return locked;
    }

    /**
     * 非原子解锁，可能解别人锁，不安全
     *
     * @param redisLockEntity
     * @return
     */
    public boolean unlock(RedisLockEntity redisLockEntity) {
        if (redisLockEntity == null || redisLockEntity.getLockKey() == null || redisLockEntity.getRequestId() == null){
            return false;
        }
        boolean releaseLock = false;
        String requestId = (String) redisTemplate.opsForValue().get(redisLockEntity.getLockKey());
        if (redisLockEntity.getRequestId().equals(requestId)) {
            releaseLock = redisTemplate.delete(redisLockEntity.getLockKey());
        }
        log.info("redis解锁:{},thread:{}",releaseLock,Thread.currentThread().getName());
        return releaseLock;
    }

    /**
     * 使用lua脚本解锁，不会解除别人锁
     *
     * @param redisLockEntity
     * @return
     */
    public boolean unlockLua(RedisLockEntity redisLockEntity) {
        if (redisLockEntity == null || redisLockEntity.getLockKey() == null || redisLockEntity.getRequestId() == null){
            return false;
        }
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript();
        //用于解锁的lua脚本位置
        redisScript.setLocation(new ClassPathResource("redisLock.lua"));
        redisScript.setResultType(Long.class);
        //没有指定序列化方式，默认使用上面配置的
        Object result = redisTemplate.execute(redisScript, Arrays.asList(redisLockEntity.getLockKey()), redisLockEntity.getRequestId());
        boolean releaseLock = result.equals(Long.valueOf(1));
        log.info("redis解锁:{},thread:{}",releaseLock,Thread.currentThread().getName());
        return releaseLock;
    }

}
