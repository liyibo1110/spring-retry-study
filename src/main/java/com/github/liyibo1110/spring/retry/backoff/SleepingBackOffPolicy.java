package com.github.liyibo1110.spring.retry.backoff;

/**
 * @author liyibo
 * @date 2026-01-25 00:38
 */
public interface SleepingBackOffPolicy<T extends SleepingBackOffPolicy<T>> extends BackOffPolicy {

    /**
     * 复制一个附带Sleeper组件的BackOffPolicy
     * 接口泛型限定了T只能是SleepingBackOffPolicy及其子类
     * @param sleeper
     * @return
     */
    T withSleeper(Sleeper sleeper);
}
