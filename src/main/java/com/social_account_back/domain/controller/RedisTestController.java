package com.social_account_back.domain.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedisTestController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/api/redis-test")
    public String testRedis() {
        redisTemplate.opsForValue().set("test-key", "hello redis");
        return redisTemplate.opsForValue().get("test-key");
    }
}
