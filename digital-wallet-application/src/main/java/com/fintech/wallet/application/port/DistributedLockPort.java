package com.fintech.wallet.application.port;

import java.util.concurrent.TimeUnit;

public interface DistributedLockPort {
    boolean tryLock(String lockKey, long timeout, TimeUnit timeUnit);
    void unlock(String lockKey);
    boolean isLocked(String lockKey);
}
