package com.hongyan.study.distributedlock.mapper;

import com.hongyan.study.distributedlock.entity.UserInfo;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserInfoMapper {

    @Select({" select user_id as userId,user_name as userName,account,password from user_info order by user_id  "})
    List<UserInfo> queryAll();

    /**
     * databse查询增加排他锁
     * @param userId
     * @return
     */
    @Select({" select user_id as userId,user_name as userName,account,password from user_info where user_id =#{userId} for update"})
    UserInfo queryByExclusiveLock(Integer userId);
}
