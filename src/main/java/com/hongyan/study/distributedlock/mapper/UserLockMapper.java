package com.hongyan.study.distributedlock.mapper;

import com.hongyan.study.distributedlock.entity.UserInfo;
import com.hongyan.study.distributedlock.entity.UserLock;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserLockMapper {

    /**
     * databse查询增加乐观锁
     * @param userId
     * @return
     */
    @Select({" select user_id as userId,user_name as userName,`version`,`timestamp` from user_lock where user_id =#{userId}"})
    UserLock query(Integer userId);

    /**
     * 根据主键id+版本号实现乐观锁，其中版本号为上一步select查询出来的version，如果与数据库中相等则会修改成功，否则修改失败
     * @param userLock
     * @return
     */
    @Update({" update user_lock set user_name=#{userName},`version`=`version`+1 where user_id=#{userId} and `version`=#{version} "})
    Integer updateOptimisticLockByVersion(UserLock userLock);

    /**
     * 根据主键id+时间搓实现乐观锁，其中时间搓为上一步select查询出来的time，如果与数据库中相等则会修改成功，否则修改失败
     * @param userLock
     * @return
     */
    @Update({" update user_lock set user_name=#{userName} where user_id=#{userId} and `timestamp`=#{timestamp} "})
    Integer updateOptimisticLockByTimeStamp(UserLock userLock);
}
