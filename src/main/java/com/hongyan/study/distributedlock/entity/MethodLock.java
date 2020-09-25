package com.hongyan.study.distributedlock.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author yan
 */
@Data
public class MethodLock {

    private Integer id;

    private String methodName;

    private String desc;

    private Date updateTime;
}
