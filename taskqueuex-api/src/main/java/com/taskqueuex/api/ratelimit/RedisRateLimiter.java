package com.taskqueuex.api.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerWindow;
    private final int windowSizeSeconds;
    private final String keyPrefix;

    public RedisRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${rate-limit.requests-per-window:100}") int requestsPerWindow,
            @Value("${rate-limit.window-size-seconds:60}") int windowSizeSeconds,
            @Value("${rate-limit.key-prefix:rate-limit:api:}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.requestsPerWindow = requestsPerWindow;
        this.windowSizeSeconds = windowSizeSeconds;
        this.keyPrefix = keyPrefix;
    }

    public boolean isAllowed(String identifier) {
        String key = keyPrefix + identifier;
        
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count == 1) {
                // First request in window, set expiration
                redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds));
            }
            
            boolean allowed = count <= requestsPerWindow;
            
            if (!allowed) {
                Long ttl = redisTemplate.getExpire(key);
                logger.debug("Rate limit exceeded for {} (count: {}, ttl: {})", identifier, count, ttl);
            }
            
            return allowed;
        } catch (Exception e) {
            logger.error("Error checking rate limit for {}", identifier, e);
            // Fail open - allow request if Redis is down
            return true;
        }
    }

    public long getRetryAfterSeconds(String identifier) {
        String key = keyPrefix + identifier;
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? ttl : windowSizeSeconds;
    }
}
