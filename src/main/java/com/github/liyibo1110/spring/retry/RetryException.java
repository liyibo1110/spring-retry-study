package com.github.liyibo1110.spring.retry;

import org.springframework.core.NestedRuntimeException;

/**
 * @author liyibo
 * @date 2026-01-23 22:47
 */
public class RetryException extends NestedRuntimeException {
    public RetryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RetryException(String msg) {
        super(msg);
    }
}
