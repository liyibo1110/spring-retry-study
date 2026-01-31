package com.github.liyibo1110.spring.retry.stats;

import com.github.liyibo1110.spring.retry.RetryStatistics;

/**
 * RetryStatistics实例的存储库
 * @author liyibo
 * @date 2026-01-31 20:21
 */
public interface StatisticsRepository {
    RetryStatistics findOne(String name);
    Iterable<RetryStatistics> findAll();
    void addStarted(String name);
    void addError(String name);
    void addRecovery(String name);
    void addComplete(String name);
    void addAbort(String name);
}
