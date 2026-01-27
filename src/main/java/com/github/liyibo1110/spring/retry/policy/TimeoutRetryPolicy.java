package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;

/**
 * 仅在未超时的前提下才能retry，计时会在open方法调用时启动
 * @author liyibo
 * @date 2026-01-26 23:27
 */
public class TimeoutRetryPolicy implements RetryPolicy {
    public static final long DEFAULT_TIMEOUT = 1000;
    /** 超时时间 */
    private long timeout;

    public TimeoutRetryPolicy() {
        this(DEFAULT_TIMEOUT);
    }

    public TimeoutRetryPolicy(long timeout) {
        this.timeout = timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        return ((TimeoutRetryContext)context).isAlive();
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new TimeoutRetryContext(parent, timeout);
    }

    @Override
    public void close(RetryContext context) {
        // nothing to do
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        ((RetryContextSupport)context).registerThrowable(throwable);
    }

    private static class TimeoutRetryContext extends RetryContextSupport {
        /** 超时时间 */
        private final long timeout;
        /** Context实例被创建的时间 */
        private final long start;

        public TimeoutRetryContext(RetryContext parent, long timeout) {
            super(parent);
            this.timeout = timeout;
            this.start = System.currentTimeMillis();
        }

        public boolean isAlive() {
            return (System.currentTimeMillis() - start) <= timeout;
        }
    }
}
