package com.hongyan.study.distributedlock.aop;

import com.hongyan.study.distributedlock.annotation.DistributeLock;
import com.hongyan.study.distributedlock.entity.R;
import com.hongyan.study.distributedlock.entity.RedisLockEntity;
import com.hongyan.study.distributedlock.enums.LockType;
import com.hongyan.study.distributedlock.enums.OpCode;
import com.hongyan.study.distributedlock.lock.DistributedLockByRedis;
import com.hongyan.study.distributedlock.service.RedissonService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * description 分布式锁自定义注解 切面
 *
 * @author yufw
 * date 2020/4/27 13:11
 */

@Aspect
@Component
@Slf4j
public class DistributeLockAop {

//    @Autowired
//    RedissonService redissonService;

    @Autowired
    private DistributedLockByRedis distributedLockByRedis;

    /**
     * 切入点
     */
    @Pointcut("@annotation(com.hongyan.study.distributedlock.annotation.DistributeLock)")
    public void doBusiness() {

    }

    @Around("doBusiness()")
    public R around(ProceedingJoinPoint joinPoint){
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取执行方法
        Method method = signature.getMethod();
        // 获取注解信息
        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);

        if (null == distributeLock) {
            log.error("不存在该分布式锁");
            return new R(OpCode.InvalidArgument.getCode(),OpCode.InvalidArgument.getValue());
        }
        //获取注解参数值
        String lockName = distributeLock.name(); //锁名称
        LockType type = distributeLock.type(); //锁类型
        long timeout = distributeLock.timeout(); //锁租期失效
        TimeUnit unit = distributeLock.unit(); //锁超时单位

        // 请求的参数
        Object[] args = joinPoint.getArgs();

        //查找是否包含${}字符，如果包含则进行属性值替换操作
        String regex = "\\$\\{(.+?)\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lockName);
        StringBuffer name = new StringBuffer();
        while(matcher.find()){
            log.info(String.format(">>>>>>>>>>>>需要被替换的参数为:[%s]",matcher.group(1)));
            matcher.appendReplacement(name,(String)args[0]);
            log.info(String.format(">>>>>>>>>>>>转换后的数据为:[%s]",name.toString()));
        }
        if(StringUtils.isEmpty(name.toString())){
            name.append(lockName);
        }

        String requestId = UUID.randomUUID().toString().replace("-","");
        RedisLockEntity lockEntity = new RedisLockEntity(name.toString() ,requestId,timeout,unit);
        try {
            if (type == LockType.LOCK) {
                Boolean lockFlag = distributedLockByRedis.lock(lockEntity);
                log.info("lockEntity:[{}],加锁[{}]",lockEntity,lockFlag);
            }else{
                log.info("aop加锁类型不符,无需进行加锁操作:type:[{}]",type);
            }
            Object object = joinPoint.proceed(joinPoint.getArgs());
            return (R) object;
        } catch (Throwable throwable) {
            log.error("aop处理内部异常：",throwable);
            return new R(OpCode.Internal.getCode(),OpCode.Internal.getValue());
        }finally {
            if (type == LockType.LOCK) {
                Boolean unLockFlag = distributedLockByRedis.unlockLua(lockEntity);
                log.info("lockEntity[{}],解锁[{}]",lockEntity,unLockFlag);
            }
        }
    }

    /**
     * 前事件
     *
     * @param joinPoint
     */
//    @Before("doBusiness()")
//    public void doBefore(JoinPoint joinPoint) {
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        //获取执行方法
//        Method method = signature.getMethod();
//        // 获取注解信息
//        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);
//
//        if (null != distributeLock) {
//            //获取注解参数值
//            String lockName = distributeLock.name();
//            LockType type = distributeLock.type();
//
//            if (type == LockType.LOCK) {
//                redissonService.lock(lockName);
//                log.info("lock:[{}]加锁成功",lockName);
//            }
//
//        }
//    }




    /**
     * 后事件
     *
     * @param joinPoint
     */
//    @After("doBusiness()")
//    public void doAfter(JoinPoint joinPoint) {
//
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        //获取执行方法
//        Method method = signature.getMethod();
//        // 获取注解信息
//        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);
//        //获取执行方法名
//        String methodName = method.getName();
//        //获取方法传递的参数
//        Object[] args = joinPoint.getArgs();
//
//        if (null != distributeLock) {
//            //获取注解参数值
//            String lockName = distributeLock.name();
//
//            if (null != distributeLock) {
//
//                redissonService.unlock(lockName);
//                log.info("lock:[{}]解锁成功",lockName);
//
//            }
//        }
//
//    }


}
