package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;

/**
 * 用于在存储和检索RetryContext实例时，使用有状态retry policy的简单类映射抽象。
 * 不应该传入null key，如果传入，实现类可以自由选择丢弃而不是保存
 * @author liyibo
 * @date 2026-01-24 22:33
 */
public interface RetryContextCache {
    RetryContext get(Object key);

    void put(Object key, RetryContext context) throws RetryCacheCapacityExceededException;

    void remove(Object key);

    boolean containsKey(Object key);
}
