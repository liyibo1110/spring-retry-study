package com.github.liyibo1110.spring.retry.backoff;

/**
 * 基于Object.wait()方法的实现
 * @author liyibo
 * @date 2026-01-26 00:05
 */
@Deprecated
public class ObjectWaitSleeper implements Sleeper {
    @Override
    public void sleep(long backOffPeriod) throws InterruptedException {
        Object mutex = new Object();
        synchronized(mutex) {
            mutex.wait(backOffPeriod);
        }
    }
}
