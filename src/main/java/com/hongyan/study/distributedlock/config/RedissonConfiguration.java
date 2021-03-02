package com.hongyan.study.distributedlock.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;


/**
 * @author zy
 * @version 1.0
 * @date Created in 2021-02-20 14:48
 * @description redisson分布式事务解决方案
 */
@Configuration
public class RedissonConfiguration {

    @Value("${spring.redis.cluster.nodes}")
    private String redisHost;

    @Bean
    public RedissonClient redisson() {
        Config config = new Config();
        String[] nodeAddr = Stream.of(redisHost.split(","))
                .map(url -> String.format("redis://%s", url)).toArray(String[]::new);
        config.useClusterServers()
                .addNodeAddress(nodeAddr);

        return Redisson.create(config);
    }
}

