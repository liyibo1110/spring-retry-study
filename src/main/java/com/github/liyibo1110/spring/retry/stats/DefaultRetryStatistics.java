package com.github.liyibo1110.spring.retry.stats;

import com.github.liyibo1110.spring.retry.RetryStatistics;
import org.springframework.core.AttributeAccessorSupport;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认统计实现类
 * @author liyibo
 * @date 2026-01-31 20:15
 */
public class DefaultRetryStatistics extends AttributeAccessorSupport implements RetryStatistics, MutableRetryStatistics {
    private String name;
    private final AtomicInteger startedCount = new AtomicInteger();
    private final AtomicInteger completeCount = new AtomicInteger();
    private final AtomicInteger recoveryCount = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final AtomicInteger abortCount = new AtomicInteger();

    DefaultRetryStatistics() {}

    public DefaultRetryStatistics(String name) {
        this.name = name;
    }

    @Override
    public int getCompleteCount() {
        return this.completeCount.get();
    }

    @Override
    public int getStartedCount() {
        return this.startedCount.get();
    }

    @Override
    public int getErrorCount() {
        return this.errorCount.get();
    }

    @Override
    public int getAbortCount() {
        return this.abortCount.get();
    }

    @Override
    public int getRecoveryCount() {
        return this.recoveryCount.get();
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void incrementStartedCount() {
        this.startedCount.incrementAndGet();
    }

    @Override
    public void incrementCompleteCount() {
        this.completeCount.incrementAndGet();
    }

    @Override
    public void incrementRecoveryCount() {
        this.recoveryCount.incrementAndGet();
    }

    @Override
    public void incrementErrorCount() {
        this.errorCount.incrementAndGet();
    }

    @Override
    public void incrementAbortCount() {
        this.abortCount.incrementAndGet();
    }

    @Override
    public String toString() {
        return "DefaultRetryStatistics [name=" + name + ", startedCount=" + startedCount + ", completeCount="
                + completeCount + ", recoveryCount=" + recoveryCount + ", errorCount=" + errorCount + ", abortCount="
                + abortCount + "]";
    }
}
