package com.example.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;

@Configuration
public class ProjectConfig {

    @Bean
    public static JedisPool getRedisInstance(){
        GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(200);
        config.setMaxIdle(50);
        config.setMinIdle(10);
        config.setBlockWhenExhausted(true);
        config.setMaxWait(Duration.ofSeconds(30));
        return new JedisPool(config,System.getenv().getOrDefault("REDIS_HOST", "localhost"), 6379);
    }


}
