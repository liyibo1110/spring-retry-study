package com.github.liyibo1110.spring.retry.support;

import com.github.liyibo1110.spring.classify.Classifier;
import com.github.liyibo1110.spring.retry.RetryState;

/**
 * RetryState的默认实现类
 * @author liyibo
 * @date 2026-01-29 11:04
 */
public class DefaultRetryState implements RetryState {
    private final Object key;
    private final boolean forceRefresh;
    /** 特定的异常，要不要回滚retry状态（和业务回滚是联动的，为了避免业务事务回滚了，retryCount却没有清0的bug） */
    private final Classifier<? super Throwable, Boolean> rollbackClassifier;

    public DefaultRetryState(Object key) {
        this(key, false, null);
    }

    public DefaultRetryState(Object key, Classifier<? super Throwable, Boolean> rollbackClassifier) {
        this(key, false, rollbackClassifier);
    }

    public DefaultRetryState(Object key, boolean forceRefresh) {
        this(key, forceRefresh, null);
    }

    public DefaultRetryState(Object key, boolean forceRefresh,
                             Classifier<? super Throwable, Boolean> rollbackClassifier) {
        this.key = key;
        this.forceRefresh = forceRefresh;
        this.rollbackClassifier = rollbackClassifier;
    }

    @Override
    public Object getKey() {
        return this.key;
    }

    @Override
    public boolean isForceRefresh() {
        return this.forceRefresh;
    }

    @Override
    public boolean rollbackFor(Throwable exception) {
        if(this.rollbackClassifier == null)
            return true;
        return this.rollbackClassifier.classify(exception);
    }

    @Override
    public String toString() {
        return String.format("[%s: key=%s, forceRefresh=%b]", getClass().getSimpleName(), key, forceRefresh);
    }
}
