package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;
import org.springframework.util.Assert;

import java.util.function.Predicate;

/**
 * 通过Predicate来判断Throwable是否要被retry的实现
 * @author liyibo
 * @date 2026-01-28 10:00
 */
public class PredicateRetryPolicy implements RetryPolicy {
    private final Predicate<Throwable> predicate;

    public PredicateRetryPolicy(Predicate<Throwable> predicate) {
        Assert.notNull(predicate, "predicate must not be null");
        this.predicate = predicate;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        return t == null || predicate.test(t);
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
