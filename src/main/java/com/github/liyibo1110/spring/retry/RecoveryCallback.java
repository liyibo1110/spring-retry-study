package com.github.liyibo1110.spring.retry;

/**
 * 在所有尝试都失败后，用于有状态重试的回调
 * @author liyibo
 * @date 2026-01-23 22:29
 */
public interface RecoveryCallback<T> {

    /**
     * @return 用于替换失败回调结果的对象
     */
    T recover(RetryContext context) throws Exception;
}
