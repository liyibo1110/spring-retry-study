package com.github.liyibo1110.spring.retry.backoff;

import java.io.Serializable;

/**
 * backoff policy执行停止操作的具体策略
 * @author liyibo
 * @date 2026-01-25 00:35
 */
public interface Sleeper extends Serializable {

    /**
     * 暂停指定时长
     */
    void sleep(long backOffPeriod) throws InterruptedException;
}
