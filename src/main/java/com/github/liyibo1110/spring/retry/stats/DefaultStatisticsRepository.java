package com.github.liyibo1110.spring.retry.stats;

import com.github.liyibo1110.spring.retry.RetryStatistics;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RetryStatistics实例的存储库实现（基于ConcurrentHashMap存储）
 * @author liyibo
 * @date 2026-01-31 20:22
 */
public class DefaultStatisticsRepository implements StatisticsRepository {
    private final ConcurrentMap<String, MutableRetryStatistics> map = new ConcurrentHashMap<>();
    private RetryStatisticsFactory factory = new DefaultRetryStatisticsFactory();

    public void setRetryStatisticsFactory(RetryStatisticsFactory factory) {
        this.factory = factory;
    }

    @Override
    public RetryStatistics findOne(String name) {
        return map.get(name);
    }

    @Override
    public Iterable<RetryStatistics> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public void addStarted(String name) {
        this.getStatistics(name).incrementStartedCount();
    }

    @Override
    public void addError(String name) {
        this.getStatistics(name).incrementErrorCount();
    }

    @Override
    public void addRecovery(String name) {
        this.getStatistics(name).incrementRecoveryCount();
    }

    @Override
    public void addComplete(String name) {
        this.getStatistics(name).incrementCompleteCount();
    }

    @Override
    public void addAbort(String name) {
        this.getStatistics(name).incrementAbortCount();
    }

    /**
     * 根据name获取对应的MutableRetryStatistics，如果cache中没有，则创建新的实例并顺便放入cache
     */
    private MutableRetryStatistics getStatistics(String name) {
        MutableRetryStatistics stats;
        if(!map.containsKey(name))
            map.putIfAbsent(name, this.factory.create(name));
        stats = map.get(name);
        return stats;
    }
}
