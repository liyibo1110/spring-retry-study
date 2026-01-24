package com.github.liyibo1110.spring.retry;

/**
 * 可通过RetryOperations进行重试的操作回调接口
 * @author liyibo
 * @date 2026-01-23 00:34
 */
public interface RetryCallback<T, E extends Throwable> {
    /**
     * 执行具有重试语义的操作。
     * 操作通常具有幂等性，但实现时可能会选择在操作重试时实时补偿语义
     */
    T doWithRetry(RetryContext context) throws E;

    /**
     * 回调对应的逻辑标识符，用于区分业务操作的重试
     */
    default String getLabel() {
        return null;
    }
}
