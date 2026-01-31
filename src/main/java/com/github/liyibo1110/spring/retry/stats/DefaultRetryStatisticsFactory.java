package com.github.liyibo1110.spring.retry.stats;

/**
 * RetryStatistics实现类的对象工厂实现
 * @author liyibo
 * @date 2026-01-31 20:19
 */
public class DefaultRetryStatisticsFactory implements RetryStatisticsFactory {
    /** 滑动时间窗口（毫秒） */
    private long window = 15000;
    public void setWindow(long window) {
        this.window = window;
    }

    @Override
    public MutableRetryStatistics create(String name) {
        return null;
    }
}
