package com.hongyan.study.distributedlock.service.impl;

import com.hongyan.study.distributedlock.service.RedissonService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedissonServiceImpl implements RedissonService {
    @Override
    public void lock(String name) {

    }

    @Override
    public void lock(String name, long leaseTime, TimeUnit unit) {

    }

    @Override
    public boolean tryLock(String name) {
        return false;
    }

    @Override
    public boolean tryLock(String name, long time, TimeUnit unit){
        return false;
    }

    @Override
    public boolean isLocked(String name) {
        return false;
    }

    @Override
    public void unlock(String name) {

    }
}
