package com.github.liyibo1110.spring.retry;

/**
 * retry监听器，可用于为retry添加行为。
 * RetryOperations的实现可以选择在重试生命周期中向interceptor发出回调
 * @author liyibo
 * @date 2026-01-24 14:32
 */
public interface RetryListener {

    /**
     * 重试第一次尝试前调用
     */
    default <T, E extends Throwable> boolean open(RetryContext context,
                                                  RetryCallback<T, E> callback) {
        return true;
    }

    /**
     * 在最后一次重试后调用（无论成功还是失败了）
     */
    default <T, E extends Throwable> void close(RetryContext context,
                                                RetryCallback<T, E> callback,
                                                Throwable throwable) {
    }

    /**
     * 每次重试成功后调用
     */
    default <T, E extends Throwable> void onSuccess(RetryContext context,
                                                    RetryCallback<T, E> callback,
                                                    T result) {

    }

    /**
     * 每次重试失败后调用
     */
    default <T, E extends Throwable> void onError(RetryContext context,
                                                  RetryCallback<T, E> callback,
                                                  Throwable throwable) {

    }
}
