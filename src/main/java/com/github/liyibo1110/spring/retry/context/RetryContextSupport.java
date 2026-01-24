package com.github.liyibo1110.spring.retry.context;

import com.github.liyibo1110.spring.retry.RetryContext;
import org.springframework.core.AttributeAccessorSupport;

/**
 * @author liyibo
 * @date 2026-01-24 22:23
 */
public class RetryContextSupport extends AttributeAccessorSupport implements RetryContext {
    private final RetryContext parent;

    private volatile boolean terminate = false;

    private volatile int count;

    private volatile Throwable lastException;

    public RetryContextSupport(RetryContext parent) {
        super();
        this.parent = parent;
    }

    @Override
    public RetryContext getParent() {
        return this.parent;
    }

    @Override
    public boolean isExhaustedOnly() {
        return this.terminate;
    }

    @Override
    public void setExhaustedOnly() {
        this.terminate = true;
    }

    @Override
    public int getRetryCount() {
        return this.count;
    }

    @Override
    public Throwable getLastThrowable() {
        return this.lastException;
    }

    /**
     * 为Context设置异常，并在Throwable非空时增加重试次数。
     * 所有RetryPolicy实现都应该在注册Throwable时使用此方法。
     * 由于会增加计数器，因此每次retry时只应调用1次该方法
     */
    public void registerThrowable(Throwable throwable) {
        this.lastException = throwable;
        if(throwable != null)
            this.count++;
    }

    @Override
    public String toString() {
        return String.format("[RetryContext: count=%d, lastException=%s, exhausted=%b]",
                count, lastException, terminate);
    }
}
