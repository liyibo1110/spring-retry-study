package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.classify.BinaryExceptionClassifier;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;

/**
 * 基于BinaryExceptionClassifier组件的policy实现，一般用这个就足够了。
 * 如果需要更灵活的分类，可以使用ExceptionClassifierRetryPolicy。
 * @author liyibo
 * @date 2026-01-27 23:56
 */
public class BinaryExceptionClassifierRetryPolicy implements RetryPolicy {

    private final BinaryExceptionClassifier classifier;

    public BinaryExceptionClassifierRetryPolicy(BinaryExceptionClassifier classifier) {
        this.classifier = classifier;
    }

    public BinaryExceptionClassifier getExceptionClassifier() {
        return classifier;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        return t == null || classifier.classify(t);
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
        RetryContextSupport simpleContext = ((RetryContextSupport)context);
        simpleContext.registerThrowable(throwable);
    }
}
