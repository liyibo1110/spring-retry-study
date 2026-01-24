package com.github.liyibo1110.spring.retry.backoff;

import com.github.liyibo1110.spring.retry.RetryException;

/**
 * 表示使用BackOffPolicy进行回退尝试时被中断。
 * 可能是在调用Thread.sleep(long)方法时发生了InterruptedException
 * @author liyibo
 * @date 2026-01-24 14:21
 */
public class BackOffInterruptedException extends RetryException {

    public BackOffInterruptedException(String msg) {
        super(msg);
    }

    public BackOffInterruptedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
