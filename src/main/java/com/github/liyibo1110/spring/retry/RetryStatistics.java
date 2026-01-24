package com.github.liyibo1110.spring.retry;

/**
 * 统计retry的具体行为
 * @author liyibo
 * @date 2026-01-24 21:52
 */
public interface RetryStatistics {
    /**
     * 返回retry成功次数
     */
    int getCompleteCount();

    /**
     * 返回进入重试块的次数，不考虑操作被重试了多少次
     */
    int getStartedCount();

    /**
     * 返回检测到的错误数量，以及这些错误是否导致了retry
     */
    int getErrorCount();

    /**
     * 返回retry后仍未成功完成的次数
     */
    int getAbortCount();

    /**
     * 返回recovery callback被应用的次数
     */
    int getRecoveryCount();

    /**
     * 返回retry块的标识符，用于进行report
     */
    String getName();
}
