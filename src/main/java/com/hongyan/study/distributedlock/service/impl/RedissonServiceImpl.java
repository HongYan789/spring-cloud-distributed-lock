//package com.hongyan.study.distributedlock.service.impl;
//
//import com.hongyan.study.distributedlock.service.RedissonService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * 其实RedissonService比较多余，Redisson自身已经支持加解锁，自旋等操作，无需再特意多封装
// */
//@Service
//@Slf4j
//public class RedissonServiceImpl implements RedissonService {
//    @Override
//    public void lock(String name) {
//
//    }
//
//    @Override
//    public void lock(String name, long leaseTime, TimeUnit unit) {
//
//    }
//
//    @Override
//    public boolean tryLock(String name) {
//        log.info("name:{}",name);
//        return false;
//    }
//
//    @Override
//    public boolean tryLock(String name, long time, TimeUnit unit){
//        return false;
//    }
//
//    @Override
//    public boolean isLocked(String name) {
//        return false;
//    }
//
//    @Override
//    public void unlock(String name) {
//
//    }
//}
