package com.github.liyibo1110.spring.retry;

/**
 * @author liyibo
 * @date 2026-01-24 14:19
 */
public class TerminatedRetryExceptionTests extends AbstractExceptionTests {
    @Override
    public Exception getException(String message) {
        return new TerminatedRetryException(message);
    }

    @Override
    public Exception getException(String message, Throwable t) {
        return new TerminatedRetryException(message, t);
    }
}
