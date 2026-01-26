package com.github.liyibo1110.spring.retry.backoff;

/**
 * 基于当前线程sleep的实现
 * @author liyibo
 * @date 2026-01-25 0:41
 */
public class ThreadWaitSleeper implements Sleeper {
    @Override
    public void sleep(long backOffPeriod) throws InterruptedException {
        Thread.sleep(backOffPeriod);
    }
}
