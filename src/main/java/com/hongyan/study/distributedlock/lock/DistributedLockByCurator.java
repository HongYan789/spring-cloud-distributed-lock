package com.hongyan.study.distributedlock.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.CountDownLatch;

/**
 * Curator实现zk分布式锁工具类
 */
@Service
@Slf4j
public class DistributedLockByCurator implements InitializingBean{

    private final static String ROOT_PATH_LOCK = "rootlock";
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Autowired
    private CuratorFramework curatorFramework;

    /**
     * 获取分布式锁
     */
    public void acquireDistributedLock(String path) {
        String keyPath = "/" + ROOT_PATH_LOCK + "/" + path;
        while (true) {
            try {
                //创建临时节点
                curatorFramework
                        .create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(keyPath);
                log.info("成功获取锁定路径:{},thread:{}", keyPath,Thread.currentThread().getName());
                break;
            } catch (Exception e) {
                log.info("获取锁定路径失败:{},thread:{}", keyPath,Thread.currentThread().getName());
                log.info("重试中 .......");
                try {
                    if (countDownLatch.getCount() <= 0) {
                        countDownLatch = new CountDownLatch(1);
                    }
                    countDownLatch.await();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 释放分布式锁
     */
    public boolean releaseDistributedLock(String path) {
        try {
            String keyPath = "/" + ROOT_PATH_LOCK + "/" + path;
            if (curatorFramework.checkExists().forPath(keyPath) != null) {
                //模拟抢占到节点的线程挂掉，后续线程会出现卡死现象
//                if("http-nio-8080-exec-1".equals(Thread.currentThread().getName())){
//                    int i = 1/0;
//                }
                curatorFramework.delete().forPath(keyPath);
            }
            log.info("成功释放锁,thread:{}",Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("释放锁失败,thread:{}",Thread.currentThread().getName());
            return false;
        }
        return true;
    }

    /**
     * 创建 watcher 事件
     * 用于监听事件变更操作：例如：调用释放分布式锁操作时进行节点删除，监听器监听到删除事件发生，则将计数器countDown(将计数器减1)，抢占到资源的新线程则可以正常获取到操作
     * 模拟抢占到节点的线程挂掉后，后续线程会出现卡死现象，但是分布式部署的其他主机确可以监听到该事件，对数据进行解锁操作
     */
    private void addWatcher(String path) throws Exception {
        String keyPath;
        if (path.equals(ROOT_PATH_LOCK)) {
            keyPath = "/" + path;
        } else {
            keyPath = "/" + ROOT_PATH_LOCK + "/" + path;
        }
        final PathChildrenCache cache = new PathChildrenCache(curatorFramework, keyPath, false);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.getListenable().addListener((client, event) -> {
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                String oldPath = event.getData().getPath();
                log.info("成功释放锁定路径:{},thread:{}", oldPath,Thread.currentThread().getName());
                if (oldPath.contains(path)) {
                    //释放计数器，让当前的请求获取锁
                    countDownLatch.countDown();
                }
            }
        });
    }

    //创建父节点，并创建永久节点
    @Override
    public void afterPropertiesSet() {
        curatorFramework = curatorFramework.usingNamespace("lock-namespace");
        String path = "/" + ROOT_PATH_LOCK;
        try {
            if (curatorFramework.checkExists().forPath(path) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(path);
            }
            addWatcher(ROOT_PATH_LOCK);
            log.info("root path 的 watcher 事件创建成功");
        } catch (Exception e) {
            log.error("connect zookeeper fail，please check the log >> {}", e.getMessage(), e);
        }
    }
}
