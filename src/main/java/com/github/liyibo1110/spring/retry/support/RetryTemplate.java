package com.github.liyibo1110.spring.retry.support;

import com.github.liyibo1110.spring.retry.ExhaustedRetryException;
import com.github.liyibo1110.spring.retry.RecoveryCallback;
import com.github.liyibo1110.spring.retry.RetryCallback;
import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.RetryOperations;
import com.github.liyibo1110.spring.retry.RetryState;
import com.github.liyibo1110.spring.retry.backoff.BackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.NoBackOffPolicy;
import com.github.liyibo1110.spring.retry.policy.MapRetryContextCache;
import com.github.liyibo1110.spring.retry.policy.RetryContextCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 模板类，用于简化具有retry语义的操作执行。
 * 可retry操作被封装在RetryCallback接口的视线中，并使用提供的execute方法之一来执行。
 * 默认情况下，如果操作抛出任何Exception或其子类，则会进行重试，可以通过setRetryPolicy方法来更改行为。
 * 默认每个操作最多重试3次，且中间没有间隔时间，也可以通过setRetryPolicy和setBackOffPolicy进行配置。
 * BackOffPolicy控制每次单独retry之间的暂停时间。
 * 这个类时线程安全的，适合在执行操作和进行配置更改时，进行并发访问，
 * 因此可以动态更改重试次数所使用的BackOffPolicy，而不会影响正在进行中的retry操作
 * @author liyibo
 * @date 2026-01-24 22:47
 */
public class RetryTemplate implements RetryOperations {
    /** retry上下文名称 */
    private static final String GLOBAL_STATE = "state.global";

    protected final Log logger = LogFactory.getLog(getClass());

    private volatile BackOffPolicy backOffPolicy = new NoBackOffPolicy();

    private volatile RetryListener[] listeners = new RetryListener[0];

    private RetryContextCache retryContextCache = new MapRetryContextCache();

    private boolean throwLastExceptionOnExhausted;

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E {
        return null;
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback) throws E {
        return null;
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RetryState retryState) throws E, ExhaustedRetryException {
        return null;
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback, RetryState retryState) throws E {
        return null;
    }
}
