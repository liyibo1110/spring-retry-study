package com.github.liyibo1110.spring.retry;

/**
 * @author liyibo
 * @date 2026-01-24 14:19
 */
public class ExhaustedRetryExceptionTests extends AbstractExceptionTests {
    @Override
    public Exception getException(String message) {
        return new ExhaustedRetryException(message);
    }

    @Override
    public Exception getException(String message, Throwable t) {
        return new ExhaustedRetryException(message, t);
    }
}
