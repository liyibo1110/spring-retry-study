package com.github.liyibo1110.spring.retry;

import com.sun.net.httpserver.Authenticator;

/**
 * @author liyibo
 * @date 2026-01-24 14:19
 */
public class RetryExceptionTests extends AbstractExceptionTests {
    @Override
    public Exception getException(String message) {
        return new RetryException(message);
    }

    @Override
    public Exception getException(String message, Throwable t) {
        return new RetryException(message, t);
    }
}
