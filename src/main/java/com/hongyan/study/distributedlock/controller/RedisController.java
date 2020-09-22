package com.hongyan.study.distributedlock.controller;

import com.hongyan.study.distributedlock.annotation.DistributeLock;
import com.hongyan.study.distributedlock.entity.R;
import com.hongyan.study.distributedlock.entity.RedisLockEntity;
import com.hongyan.study.distributedlock.enums.LockType;
import com.hongyan.study.distributedlock.enums.OpCode;
import com.hongyan.study.distributedlock.lock.DistributedLockByRedis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 采用redis实现分布式锁controller
 * 用于模拟高并发时争抢业务锁
 */
@RestController
@RequestMapping("/redis")
@Slf4j
public class RedisController {

    @Autowired
    private DistributedLockByRedis distributedLockByRedis;

    private String redis_key = "data:::redis-lock";

    @GetMapping("/lock1")
    public Boolean getLock1(){
        Boolean flag;
        String requestId = UUID.randomUUID().toString().replace("-","");
        RedisLockEntity entity = new RedisLockEntity(redis_key,requestId);
        distributedLockByRedis.lock(entity);
        try{
            log.info("执行redis-lock1操作，thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(20000);
        }catch (Exception e){

        }finally {
            flag = distributedLockByRedis.unlockLua(entity);
        }
        return flag;
    }

    @GetMapping("/lock2")
    public Boolean getLock2(){
        Boolean flag;
        String requestId = UUID.randomUUID().toString().replace("-","");
        RedisLockEntity entity = new RedisLockEntity(redis_key,requestId);
        distributedLockByRedis.lockSpin(entity);
        try{
            log.info("执行redis-lock2操作，thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(20000);
        }catch (Exception e){

        }finally {
            flag = distributedLockByRedis.unlockLua(entity);
        }
        return flag;
    }

    @GetMapping("/annotationLock")
    @DistributeLock(type = LockType.LOCK,name = "data:::annotationLock:::${name}",timeout = 10L)
    public R getAnnotationLock(String name) throws InterruptedException {
        R r = new R(OpCode.sucess.getCode(),OpCode.sucess.getValue(),new RedisLockEntity(redis_key,UUID.randomUUID().toString().replace("-",""),10L,TimeUnit.SECONDS));
//        int i = 1/0;
        Thread.sleep(5000);
        return r;
    }
}
