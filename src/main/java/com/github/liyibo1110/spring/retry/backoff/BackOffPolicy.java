package com.github.liyibo1110.spring.retry.backoff;

import com.github.liyibo1110.spring.retry.RetryContext;

/**
 * 用于控制单次retry操作中尝试之间的backoff，实现类应具备线程安全，并应该设计为支持并发访问。
 * 每个实现类的配置也应该具有线程安全性，但无需适应高负载并发访问。
 * 对于每个retry操作块，会调用start方法，实现类可以返回一个特定的BackOffContext，可用于在后续backoff调用中跟踪状态。
 * 每个backoff过程都通过调用backOff来处理，RetryTemplate将传入由调用start方法创建的相应BackOffContext对象
 * @author liyibo
 * @date 2026-01-24 22:41
 */
public interface BackOffPolicy {

    /**
     * 启动一组新的backoff操作
     */
    BackOffContext start(RetryContext context);

    /**
     * 以特定于实现的方式back/pause，
     * 传入的BackOffContext实例与通过调用start返回的BackOffContext实例相对应。
     */
    void backOff(BackOffContext backOffContext) throws BackOffInterruptedException;

}
