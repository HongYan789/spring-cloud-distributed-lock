package com.hongyan.study.distributedlock.annotation;

import com.hongyan.study.distributedlock.enums.LockType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 咱们自定义注解实现分布式锁功能
 */
@Target({ElementType.PARAMETER,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributeLock {

    /**
     * 锁类型
     *
     * @return
     */
    LockType type() default LockType.LOCK;

    /**
     * 分布式锁 名称
     *
     * @return
     */
    String name() default "data:::redis-lock";

    /**
     * 租期、超时时间
     * @return
     */
    long timeout() default -1;

    /**
     * 超时单位
     * @return
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
