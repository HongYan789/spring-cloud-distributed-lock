package com.hongyan.study.distributedlock.service;

import java.util.concurrent.TimeUnit;

public interface RedissonService {

    /**
     * 无指定时间 加锁
     */
    void lock(String name);

    /**
     * 指定时间 加锁
     *
     * @param leaseTime
     * @param unit
     */
    void lock(String name, long leaseTime, TimeUnit unit);

    boolean tryLock(String name);

    boolean tryLock(String name, long time, TimeUnit unit);

    boolean isLocked(String name);

    void unlock(String name);

}
