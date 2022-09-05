package com.darlandi.carpoolingchallenge;

import com.darlandi.carpoolingchallenge.embeddedRedis.RedisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import redis.embedded.RedisServer;

@SpringBootApplication
public class CarPoolingChallengeApplication {
    private static final Logger logger = LoggerFactory.getLogger(CarPoolingChallengeApplication.class);
    public static void main(String[] args) {

        SpringApplication.run(CarPoolingChallengeApplication.class, args);
        redisServer();
    }

    private static void redisServer() {
        RedisServer redisServer;
        RedisProperties redisProperties = new RedisProperties(6370, "localhost");
        redisServer = new RedisServer(redisProperties.getRedisPort());
        redisServer.start();
        logger.info("Redis server started");
    }
}
