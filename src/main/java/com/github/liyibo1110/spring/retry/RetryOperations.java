package com.github.liyibo1110.spring.retry;

/**
 * 定义了retry的基本操作，用于执行具有可配置重试行为的操作
 * @author liyibo
 * @date 2026-01-23 00:31
 */
public interface RetryOperations {

    /**
     * 不带状态的重试
     */
    <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E;

    /**
     * 不带状态的重试，用尽了尝试次数执行recovery
     */
    <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback,
                                       RecoveryCallback<T> recoveryCallback) throws E;

    /**
     * 带状态的重试
     */
    <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback,
                                       RetryState retryState) throws E, ExhaustedRetryException;

    /**
     * 带状态的重试，用尽了尝试次数执行recovery
     */
    <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback,
                                       RecoveryCallback<T> recoveryCallback,
                                       RetryState retryState) throws E;
}
