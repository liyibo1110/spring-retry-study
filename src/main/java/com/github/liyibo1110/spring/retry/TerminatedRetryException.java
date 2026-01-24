package com.github.liyibo1110.spring.retry;

/**
 * @author liyibo
 * @date 2026-01-23 22:49
 */
public class TerminatedRetryException extends RetryException {
    public TerminatedRetryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public TerminatedRetryException(String msg) {
        super(msg);
    }
}
