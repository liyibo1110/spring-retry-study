package com.github.liyibo1110.spring.retry.stats;

/**
 * RetryStatistics实现类的对象工厂
 * @author liyibo
 * @date 2026-01-31 20:18
 */
public interface RetryStatisticsFactory {
    /**
     * 构建新的MutableRetryStatistics实例
     */
    MutableRetryStatistics create(String name);
}
