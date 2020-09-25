package com.hongyan.study.distributedlock.controller;

import com.hongyan.study.distributedlock.entity.MethodLock;
import com.hongyan.study.distributedlock.entity.R;
import com.hongyan.study.distributedlock.entity.UserInfo;
import com.hongyan.study.distributedlock.entity.UserLock;
import com.hongyan.study.distributedlock.enums.OpCode;
import com.hongyan.study.distributedlock.mapper.MethodLockMapper;
import com.hongyan.study.distributedlock.mapper.UserInfoMapper;
import com.hongyan.study.distributedlock.mapper.UserLockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;

/**
 * 采用数据库实现分布式锁controller
 * 用于模拟高并发时争抢业务锁
 */
@RestController
@RequestMapping("/database")
@Slf4j
public class DatabaseController {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private MethodLockMapper methodLockMapper;
    @Autowired
    DataSource dataSource;
    @Autowired
    private UserLockMapper userLockMapper;

    /**
     * method 1：
     * 基于数据库锁方式实现分布式锁
     * @return
     */
    @GetMapping("/lock1")
    public Boolean getLock1(){
        Boolean flag = true;
        String methodName = "lockMethod";
        //模拟加锁
        try {
            log.info(">>>>执行database加锁操作>>>>，threadName:[{}]",Thread.currentThread().getName());
            lock(methodName,"lock method by database");
        } catch (Exception e) {
            flag = false;
            log.error(">>>>执行database加锁操作失败>>>>，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
        }
        if(flag){
            try {
                //模拟业务操作
                log.info(">>>>休眠15s模拟业务处理耗时>>>>，threadName:[{}]",Thread.currentThread().getName());
                Thread.sleep(15000);
                //模拟解锁
                unLock(methodName);
                log.info(">>>>解锁成功>>>>，threadName:[{}]",Thread.currentThread().getName());
            } catch (Exception e) {
                log.error(">>>>执行database加锁操作失败>>>>，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
            }
        }
        return flag;
    }
    /**
     * 模拟加锁
     */
    private void lock(String methodName,String desc) {
        MethodLock lock = new MethodLock();
        lock.setMethodName(methodName);
        lock.setDesc(desc);
        methodLockMapper.save(lock);
    }
    /**
     * 模拟解锁
     * @param methodName
     */
    private void unLock(String methodName) {
        methodLockMapper.delete(methodName);
    }

    /**
     * method 2：
     * 基于数据库排他锁（悲观锁）实现分布式锁
     * @return
     */
    @GetMapping("/lock2")
    public R getLock2(Integer userId){
        try(Connection conn = dataSource.getConnection();){
            //设置手动提交连接
            //执行数据库排他锁加锁操作
            UserInfo userInfo = lockByExclusive(userId,conn);
            //模拟业务操作
            log.info(">>>>休眠15s模拟业务处理耗时，threadName:[{}]>>>>",Thread.currentThread().getName());
            Thread.sleep(15000);

            //执行数据库排他锁解锁操作
            unLockByExclusive(conn);
            return new R(OpCode.sucess.getCode(),OpCode.sucess.getValue(),userInfo);
        }catch (Exception e){
            log.error("数据库执行异常",e);
            return new R(OpCode.sucess.getCode(),OpCode.sucess.getValue(),new UserInfo());
        }

    }

    /**
     * 排他锁方式加锁
     */
    private UserInfo lockByExclusive(Integer userId,Connection conn){
        try {
            //设置成手动模式提交（mysql默认都是自动提交）
            conn.setAutoCommit(false);
            log.info("执行database数据库加锁操作，threadName:[{}]",Thread.currentThread().getName());
            return userInfoMapper.queryByExclusiveLock(userId);
        } catch (Exception e) {
            log.error("执行database数据库加锁操作，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
            return new UserInfo();
        }
    }

    /**
     * 排他锁方式解锁
     */
    private void unLockByExclusive(Connection conn){
        try {
            log.info("执行database数据库解锁操作，threadName:[{}]",Thread.currentThread().getName());
            //释放锁
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
                log.error("database数据库解锁失败，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
            } catch (SQLException ex) {
                log.error("database数据库解锁异常，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),ex.getMessage());
            }
//        }finally {
//            try {
//                conn.setAutoCommit(true);
//            } catch (SQLException e) {
//                log.error("database数据库设置自动提交异常，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
//            }
        }
    }


    /**
     * method 3：
     * 基于数据库乐观锁实现分布式锁
     * @return
     */
    @GetMapping("/lock3")
    public Boolean getLock3(Integer userId){
        //获取欲更新的数据
        UserLock userLock = userLockMapper.query(userId);
        log.info("查询出修改前database数据:UserLock:[{}]",userLock);
        //模拟业务操作
        try {
            log.info(">>>>休眠15s模拟业务处理耗时，threadName:[{}]>>>>",Thread.currentThread().getName());
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            log.error("业务处理异常",e);
        }
        //采用version版本号方式实现分布式锁
//        return lockByOptimisticVersion(userLock);
        //采用timeStamp时间搓方式实现分布式锁
        return lockByOptimistictimeStamp(userLock);
    }

    private Random random = new Random();
    /**
     * 采用版本号version实现的乐观锁
     * @param userLock
     */
    public Boolean lockByOptimisticVersion(UserLock userLock){
        UserLock lock = new UserLock();
        lock.setUserId(userLock.getUserId());
        lock.setUserName("modifyedByVersion"+random.nextInt(10));
        lock.setVersion(userLock.getVersion());
        Integer count = userLockMapper.updateOptimisticLockByVersion(lock);
        Boolean flag = count > 0 ? true :false;
        log.info("查询出修改后database数据:UserLock:[{}],修改操作执行:[{}]",lock,flag);
        return flag;
    }

    /**
     * 采用时间搓timeStamp实现的乐观锁
     * @param userLock
     */
    public Boolean lockByOptimistictimeStamp(UserLock userLock){
        UserLock lock = new UserLock();
        lock.setUserId(userLock.getUserId());
        lock.setUserName("modifyedByTimeStamp"+random.nextInt(10)+100);
        lock.setVersion(userLock.getVersion());
        lock.setTimestamp(userLock.getTimestamp());
        Integer count = userLockMapper.updateOptimisticLockByTimeStamp(lock);
        Boolean flag = count > 0 ? true :false;
        log.info("查询出修改后database数据:UserLock:[{}],修改操作执行:[{}]",lock,flag);
        return flag;
    }



}
