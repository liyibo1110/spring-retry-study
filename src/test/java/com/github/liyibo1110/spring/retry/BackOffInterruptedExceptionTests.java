package com.github.liyibo1110.spring.retry;

import com.github.liyibo1110.spring.retry.backoff.BackOffInterruptedException;

/**
 * @author liyibo
 * @date 2026-01-24 14:26
 */
public class BackOffInterruptedExceptionTests extends AbstractExceptionTests {
    @Override
    public Exception getException(String message) {
        return new BackOffInterruptedException(message);
    }

    @Override
    public Exception getException(String message, Throwable t) {
        return new BackOffInterruptedException(message, t);
    }
}
