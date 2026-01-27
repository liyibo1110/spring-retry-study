package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.classify.BinaryExceptionClassifier;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    private BinaryExceptionClassifier retryableClassifier;

    private BinaryExceptionClassifier recoverableClassifier = new BinaryExceptionClassifier(Collections.emptyMap(),
            true, true);

    public SimpleRetryPolicy() {
        this(DEFAULT_MAX_ATTEMPTS, BinaryExceptionClassifier.defaultClassifier());
    }

    public SimpleRetryPolicy(int maxAttempts) {
        this(maxAttempts, BinaryExceptionClassifier.defaultClassifier());
    }

    public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions) {
        this(maxAttempts, retryableExceptions, false);
    }

    public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
                             boolean traverseCauses) {
        this(maxAttempts, retryableExceptions, traverseCauses, false);
    }

    public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
                             boolean traverseCauses, boolean defaultValue) {
        super();
        this.maxAttempts = maxAttempts;
        this.retryableClassifier = new BinaryExceptionClassifier(retryableExceptions, defaultValue);
        this.retryableClassifier.setTraverseCauses(traverseCauses);
    }

    public SimpleRetryPolicy(int maxAttempts, BinaryExceptionClassifier classifier) {
        super();
        this.maxAttempts = maxAttempts;
        this.retryableClassifier = classifier;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * 配置不进行recovery操作的异常（即重试最终失败了，但是也恢复不了的严重异常）
     */
    public void setNotRecoverable(Class<? extends Throwable>... noRecovery) {
        Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
        for(var clazz : noRecovery)
            map.put(clazz, false);
        this.recoverableClassifier = new BinaryExceptionClassifier(map, true, true);
    }

    public void maxAttemptsSupplier(Supplier<Integer> maxAttemptsSupplier) {
        Assert.notNull(maxAttemptsSupplier, "'maxAttemptsSupplier' cannot be null");
        this.maxAttemptsSupplier = maxAttemptsSupplier;
    }

    @Override
    public int getMaxAttempts() {
        if(this.maxAttemptsSupplier != null)
            return this.maxAttemptsSupplier.get();
        return this.maxAttempts;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        // t == null 针对的是第一次重试之前的判断
        boolean retry = (t == null || retryForException(t) && context.getRetryCount() < getMaxAttempts());
        if(!retry && t != null && !this.recoverableClassifier.classify(t))
            context.setAttribute(RetryContext.NO_RECOVERY, true);   // 设定不执行recovery的标记
        else
            context.removeAttribute(RetryContext.NO_RECOVERY);  // 移除不执行recovery的标记（即要执行recovery）
        return retry;
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new SimpleRetryContext(parent);
    }

    @Override
    public void close(RetryContext context) {
        // nothing to do
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        SimpleRetryContext simpleContext = ((SimpleRetryContext)context);
        simpleContext.registerThrowable(throwable);
    }

    /**
     * 委托给retryableClassifier干活，看指定的异常，是不是要retry
     */
    private boolean retryForException(Throwable ex) {
        return this.retryableClassifier.classify(ex);
    }

    /**
     * 等同于RetryContextSupport
     */
    private static class SimpleRetryContext extends RetryContextSupport {
        public SimpleRetryContext(RetryContext parent) {
            super(parent);
        }
    }

    @Override
    public String toString() {
        return ClassUtils.getShortName(getClass()) + "[maxAttempts=" + getMaxAttempts() + "]";
    }
}
