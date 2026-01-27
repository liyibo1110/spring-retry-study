package com.github.liyibo1110.spring.retry;

import java.io.Serializable;

/**
 * RetryPolicy负责分配和管理RetryOperations所需的资源，使retry能够感知上下文。
 * 上下文可以是retry框架内部的，例如用于支持嵌套重试，
 * 也可以是外部的，并且RetryPolicy为外部上下提供一系列不同平台的统一API
 * @author liyibo
 * @date 2026-01-24 21:56
 */
public interface RetryPolicy extends Serializable {
    /** 当策略未规定失败前的最大尝试次数时，getMaxAttempts()要返回的值 */
    int NO_MAXIMUM_ATTEMPTS_SET = -1;

    /**
     * 本次是否可以进行retry
     */
    boolean canRetry(RetryContext context);

    /**
     * 获取retry需要的资源，传入callback，以便可以使用标记接口，并
     * 且管理器可以与callback协作，在状态令牌中设置某些状态
     */
    RetryContext open(RetryContext parent);

    void close(RetryContext context);

    /**
     * 在callback失败时，每次retry尝试时会调用1次
     */
    void registerThrowable(RetryContext context, Throwable throwable);

    default int getMaxAttempts() {
        return NO_MAXIMUM_ATTEMPTS_SET;
    }
}
