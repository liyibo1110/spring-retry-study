package com.github.liyibo1110.spring.retry;

/**
 * 有状态重试的特点是：必须识别正在处理的项，因此此接口用于在失败尝试之间提供缓存key。
 * 它还为RetryOperations提供提示，以便进行优化，
 * 避免不必要的缓存命中，并在不需要回滚时切到到无状态重试。
 * @author liyibo
 * @date 2026-01-23 22:32
 */
public interface RetryState {
    /**
     * 表示状态的key
     */
    Object getKey();

    /**
     * 指示是否可以避免缓存查找
     * 如果在重试前已知key是新的（即之前从未见过），如果此方法为true，则避免缓存查找
     */
    boolean isForceRefresh();

    /**
     * 检查此异常是否需要回滚，默认值始终为true。
     * 这是一种保守做法，因此如果存在不需要回滚的异常，此方法会优化切换到无状态重试。
     */
    boolean rollbackFor(Throwable exception);
}
