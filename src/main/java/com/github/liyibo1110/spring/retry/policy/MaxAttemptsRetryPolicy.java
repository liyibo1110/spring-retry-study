package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;

/**
 * 仅考虑retry次数的实现，次数包括了首次retry
 * 不建议直接使用这个实现，强烈建议按照异常分类（例如不会OutOfMemoryError进行retry）
 * @author liyibo
 * @date 2026-01-26 23:35
 */
public class MaxAttemptsRetryPolicy implements RetryPolicy {
    public final static int DEFAULT_MAX_ATTEMPTS = 3;
    private volatile int maxAttempts;

    public MaxAttemptsRetryPolicy() {
        this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public int getMaxAttempts() {
        return this.maxAttempts;
    }

    public MaxAttemptsRetryPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        return context.getRetryCount() < maxAttempts;
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new RetryContextSupport(parent);
    }

    @Override
    public void close(RetryContext context) {
        // nothing to do
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        ((RetryContextSupport)context).registerThrowable(throwable);
    }
}
