package com.hongyan.study.distributedlock.mapper;

import com.hongyan.study.distributedlock.entity.MethodLock;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

public interface MethodLockMapper {


    @Insert({"insert into method_lock set method_name=#{methodName} ,`desc`=#{desc}"})
    int save(MethodLock methodLock);

    @Delete({"delete from method_lock where method_name=#{methodName}"})
    int delete(String methodName);
}
