package com.github.liyibo1110.spring.retry;

/**
 * @author liyibo
 * @date 2026-01-23 22:48
 */
public class ExhaustedRetryException extends RetryException {
    public ExhaustedRetryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ExhaustedRetryException(String msg) {
        super(msg);
    }
}
