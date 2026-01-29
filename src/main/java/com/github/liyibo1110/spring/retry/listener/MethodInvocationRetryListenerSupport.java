package com.github.liyibo1110.spring.retry.listener;

import com.github.liyibo1110.spring.retry.RetryCallback;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.interceptor.MethodInvocationRetryCallback;

/**
 *
 * @author liyibo
 * @date 2026-01-28 13:47
 */
public class MethodInvocationRetryListenerSupport implements RetryListener {

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context,
                                                 RetryCallback<T, E> callback) {
        if(callback instanceof MethodInvocationRetryCallback) {
            var methodInvocationRetryCallback = (MethodInvocationRetryCallback<T, E>)callback;
            return doOpen(context, methodInvocationRetryCallback);
        }
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context,
                                               RetryCallback<T, E> callback,
                                               Throwable throwable) {
        if(callback instanceof MethodInvocationRetryCallback) {
            var methodInvocationRetryCallback = (MethodInvocationRetryCallback<T, E>)callback;
            doClose(context, methodInvocationRetryCallback, throwable);
        }
    }

    @Override
    public <T, E extends Throwable> void onSuccess(RetryContext context,
                                                   RetryCallback<T, E> callback,
                                                   T result) {
        if(callback instanceof MethodInvocationRetryCallback) {
            var methodInvocationRetryCallback = (MethodInvocationRetryCallback<T, E>)callback;
            doOnSuccess(context, methodInvocationRetryCallback, result);
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
                                                 RetryCallback<T, E> callback,
                                                 Throwable throwable) {
        if(callback instanceof MethodInvocationRetryCallback) {
            var methodInvocationRetryCallback = (MethodInvocationRetryCallback<T, E>)callback;
            doOnError(context, methodInvocationRetryCallback, throwable);
        }
    }

    /**
     * 首次retry之前的调用，返回false可以否决整个retry过程，会抛出TerminatedRetryException
     */
    protected <T, E extends Throwable> boolean doOpen(RetryContext context,
                                                      MethodInvocationRetryCallback<T, E> callback) {
        return true;
    }

    protected <T, E extends Throwable> void doClose(RetryContext context,
                                                    MethodInvocationRetryCallback<T, E> callback,
                                                    Throwable throwable) {

    }

    protected <T, E extends Throwable> void doOnSuccess(RetryContext context,
                                                        MethodInvocationRetryCallback<T, E> callback,
                                                        T result) {

    }

    protected <T, E extends Throwable> void doOnError(RetryContext context,
                                                      MethodInvocationRetryCallback<T, E> callback,
                                                      Throwable throwable) {

    }
}
