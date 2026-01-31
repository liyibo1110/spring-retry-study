package com.github.liyibo1110.spring.retry.stats;

import com.github.liyibo1110.spring.retry.RetryStatistics;
import org.springframework.core.AttributeAccessor;

/**
 * 对RetryStatistics的功能增强接口
 * @author liyibo
 * @date 2026-01-31 20:14
 */
public interface MutableRetryStatistics extends RetryStatistics, AttributeAccessor {

    void incrementStartedCount();

    void incrementCompleteCount();

    void incrementRecoveryCount();

    void incrementErrorCount();

    void incrementAbortCount();
}
