package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryException;

/**
 * 表示缓存限制已超出。
 * 可能是在调用Thread.sleep(long)方法时发生了InterruptedException
 * @author liyibo
 * @date 2026-01-24 14:21
 */
public class RetryCacheCapacityExceededException extends RetryException {

    public RetryCacheCapacityExceededException(String msg) {
        super(msg);
    }

    public RetryCacheCapacityExceededException(String msg, Throwable nested) {
        super(msg, nested);
    }
}
