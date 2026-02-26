package com.taskqueuex.api.service;

import com.taskqueuex.common.queue.QueueConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QueueProducerService {

    private static final Logger logger = LoggerFactory.getLogger(QueueProducerService.class);

    private final StringRedisTemplate redisTemplate;

    public QueueProducerService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void enqueueJob(UUID jobId) {
        try {
            redisTemplate.opsForList().rightPush(QueueConstants.JOB_QUEUE_NAME, jobId.toString());
            logger.debug("Enqueued job {} to queue {}", jobId, QueueConstants.JOB_QUEUE_NAME);
        } catch (Exception e) {
            logger.error("Failed to enqueue job {}", jobId, e);
            throw new RuntimeException("Failed to enqueue job", e);
        }
    }
}
