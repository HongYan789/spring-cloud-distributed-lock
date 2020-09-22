package com.hongyan.study.distributedlock.controller;

import com.hongyan.study.distributedlock.lock.DistributedLockByCurator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采用zk实现分布式锁controller
 * 用于模拟高并发时争抢业务锁
 */
@RestController
@RequestMapping("/curator")
@Slf4j
public class CuratorController {


    @Autowired
    private DistributedLockByCurator distributedLockByZookeeper;

    private final static String PATH = "test";

    @GetMapping("/lock1")
    public Boolean getLock1() {
        Boolean flag;
        distributedLockByZookeeper.acquireDistributedLock(PATH);
        try {
            log.info("执行lock1操作，更新zk root,thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            flag = distributedLockByZookeeper.releaseDistributedLock(PATH);
        }
        return flag;
    }

    @GetMapping("/lock2")
    public Boolean getLock2() {
        Boolean flag;
        distributedLockByZookeeper.acquireDistributedLock(PATH);
        try {
            log.info("执行lock2操作，更新zk root,thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            flag = distributedLockByZookeeper.releaseDistributedLock(PATH);
        }
        return flag;
    }
}
