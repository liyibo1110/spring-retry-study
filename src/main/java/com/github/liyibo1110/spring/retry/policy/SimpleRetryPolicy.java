package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;

import java.util.function.Supplier;

/**
 * 简单的retry policy实现类。
 * 针对一组指定的异常（及其子类）进行固定次数的重试，重试次数包含了首次尝试。
 * 1.3版本后无需使用此类，
 * @author liyibo
 * @date 2026-01-26 00:09
 */
public class SimpleRetryPolicy implements RetryPolicy {
    public final static int DEFAULT_MAX_ATTEMPTS = 3;

    private int maxAttempts;

    private Supplier<Integer> maxAttemptsSupplier;

    @Override
    public boolean canRetry(RetryContext context) {
        return false;
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return null;
    }

    @Override
    public void close(RetryContext context) {

    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {

    }
}
