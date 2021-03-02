package com.hongyan.study.distributedlock.controller;

import com.hongyan.study.distributedlock.service.RedissonService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 采用redisson实现分布式锁controller
 * 用于模拟高并发时争抢业务锁
 */
@RestController
@RequestMapping("/redisson")
@Slf4j
public class RedissonController {

    @Resource
    private Redisson redisson;

//    @Autowired
//    private RedissonService redissonService;


    @GetMapping("/lock1")
    public Boolean getLock1() throws InterruptedException{
        ExecutorService pool = Executors.newFixedThreadPool(10);
        Runnable task = () -> {
            RLock lock = redisson.getLock("1");
            try {
                if (!lock.tryLock(5L, 10L, TimeUnit.SECONDS)) {
                    throw new RuntimeException("获取锁失败");
                }
                IntStream.range(1, 10).forEach(i -> System.out.println(Thread.currentThread().getName() + " " + i));
            } catch (Exception e) {
                throw new RuntimeException("获取锁失败" + e.getMessage());
            } finally {
                lock.unlock();
            }
        };
        pool.submit(task);
        pool.submit(task);
        TimeUnit.SECONDS.sleep(10);
        return true;
    }

//    @GetMapping("/lock2")
//    public Boolean getLock2() throws InterruptedException{
//        log.info("get lock2");
//        redissonService.tryLock("aaaaa");
//        return true;
//    }


}
