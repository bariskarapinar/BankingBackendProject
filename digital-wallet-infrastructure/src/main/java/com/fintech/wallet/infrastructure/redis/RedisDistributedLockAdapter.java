package com.fintech.wallet.infrastructure.redis;

import com.fintech.wallet.application.port.DistributedLockPort;
import com.fintech.wallet.common.exception.InfrastructureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLockAdapter implements DistributedLockPort {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOCK_VALUE_PREFIX = "lock-";

    @Override
    public boolean tryLock(String lockKey, long timeout, TimeUnit timeUnit) {
        try {
            String lockValue = LOCK_VALUE_PREFIX + UUID.randomUUID();
            Boolean isSet = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, timeout, timeUnit);
            
            if (Boolean.TRUE.equals(isSet)) {
                log.debug("Lock acquired: {}", lockKey);
                return true;
            }
            
            log.warn("Failed to acquire lock (already exists): {}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("Error acquiring distributed lock: {}", lockKey, e);
            throw new InfrastructureException(
                    "Failed to acquire distributed lock",
                    "LOCK_ACQUISITION_ERROR",
                    "Lock key: " + lockKey,
                    e
            );
        }
    }

    @Override
    public void unlock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
            log.debug("Lock released: {}", lockKey);
        } catch (Exception e) {
            log.warn("Error releasing distributed lock: {}", lockKey, e);
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        try {
            String value = redisTemplate.opsForValue().get(lockKey);
            return value != null && value.startsWith(LOCK_VALUE_PREFIX);
        } catch (Exception e) {
            log.error("Error checking lock status: {}", lockKey, e);
            return false;
        }
    }
}
