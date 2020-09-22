package com.hongyan.study.distributedlock.entity;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class RedisLockEntity {
    private String lockKey;
    private String requestId;
    private Long timeout;
    private TimeUnit unit;

    public RedisLockEntity() {
    }

    public RedisLockEntity(String lockKey, String requestId) {
        this.lockKey = lockKey;
        this.requestId = requestId;
    }

    public RedisLockEntity(String lockKey, String requestId, Long timeout, TimeUnit unit) {
        this.lockKey = lockKey;
        this.requestId = requestId;
        this.timeout = timeout;
        this.unit = unit;
    }


}
