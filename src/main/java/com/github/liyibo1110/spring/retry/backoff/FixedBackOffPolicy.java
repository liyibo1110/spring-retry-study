package com.github.liyibo1110.spring.retry.backoff;

import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * 暂停固定时间段的BackOffPolicy实现，通过Sleeper组件实现的暂停
 * @author liyibo
 * @date 2026-01-26 23:43
 */
public class FixedBackOffPolicy extends StatelessBackOffPolicy implements SleepingBackOffPolicy<FixedBackOffPolicy> {
    private static final long DEFAULT_BACK_OFF_PERIOD = 1000L;
    private Supplier<Long> backOffPeriod = () -> DEFAULT_BACK_OFF_PERIOD;
    private Sleeper sleeper = new ThreadWaitSleeper();

    @Override
    public FixedBackOffPolicy withSleeper(Sleeper sleeper) {
        FixedBackOffPolicy res = new FixedBackOffPolicy();
        res.backOffPeriodSupplier(backOffPeriod);
        res.setSleeper(sleeper);
        return null;
    }

    public void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    public void setBackOffPeriod(long backOffPeriod) {
        this.backOffPeriod = () -> (backOffPeriod > 0 ? backOffPeriod : 1);
    }

    public void backOffPeriodSupplier(Supplier<Long> backOffPeriodSupplier) {
        Assert.notNull(backOffPeriodSupplier, "'backOffPeriodSupplier' cannot be null");
        this.backOffPeriod = backOffPeriodSupplier;
    }

    public long getBackOffPeriod() {
        return backOffPeriod.get();
    }

    @Override
    protected void doBackOff() throws BackOffInterruptedException {
        try {
            sleeper.sleep(backOffPeriod.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
        }
    }

    @Override
    public String toString() {
        return "FixedBackOffPolicy[backOffPeriod=" + backOffPeriod.get() + "]";
    }
}
