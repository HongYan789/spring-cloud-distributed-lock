package com.hongyan.study.distributedlock.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author yan
 */
@Data
public class UserLock {

    private Long userId;

    private String userName;

    private Integer version;

    private Date timestamp;
}
