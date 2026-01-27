package com.github.liyibo1110.spring.retry.backoff;

import org.springframework.util.Assert;

import java.util.Random;
import java.util.function.Supplier;

/**
 * 会暂停一段随机时间的实现，通过Sleeper组件实现的暂停
 * @author liyibo
 * @date 2026-01-26 23:49
 */
public class UniformRandomBackOffPolicy extends StatelessBackOffPolicy implements SleepingBackOffPolicy<UniformRandomBackOffPolicy> {

    private static final long DEFAULT_BACK_OFF_MIN_PERIOD = 500L;
    private static final long DEFAULT_BACK_OFF_MAX_PERIOD = 1500L;
    private Supplier<Long> minBackOffPeriod = () -> DEFAULT_BACK_OFF_MIN_PERIOD;
    private Supplier<Long> maxBackOffPeriod = () -> DEFAULT_BACK_OFF_MAX_PERIOD;
    private final Random random = new Random(System.currentTimeMillis());
    private Sleeper sleeper = new ThreadWaitSleeper();

    @Override
    public UniformRandomBackOffPolicy withSleeper(Sleeper sleeper) {
        UniformRandomBackOffPolicy res = new UniformRandomBackOffPolicy();
        res.minBackOffPeriodSupplier(minBackOffPeriod);
        res.maxBackOffPeriodSupplier(maxBackOffPeriod);
        res.setSleeper(sleeper);
        return res;
    }

    public void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    public void setMinBackOffPeriod(long backOffPeriod) {
        this.minBackOffPeriod = () -> (backOffPeriod > 0 ? backOffPeriod : 1);
    }

    public void minBackOffPeriodSupplier(Supplier<Long> backOffPeriodSupplier) {
        Assert.notNull(backOffPeriodSupplier, "'backOffPeriodSupplier' cannot be null");
        this.minBackOffPeriod = backOffPeriodSupplier;
    }

    public long getMinBackOffPeriod() {
        return minBackOffPeriod.get();
    }

    public void setMaxBackOffPeriod(long backOffPeriod) {
        this.maxBackOffPeriod = () -> (backOffPeriod > 0 ? backOffPeriod : 1);
    }

    public void maxBackOffPeriodSupplier(Supplier<Long> backOffPeriodSupplier) {
        Assert.notNull(backOffPeriodSupplier, "'backOffPeriodSupplier' cannot be null");
        this.maxBackOffPeriod = backOffPeriodSupplier;
    }

    public long getMaxBackOffPeriod() {
        return maxBackOffPeriod.get();
    }

    @Override
    protected void doBackOff() throws BackOffInterruptedException {
        try {
            Long min = minBackOffPeriod.get();
            Long max = maxBackOffPeriod.get();
            long delta = max <= min ? 0 : random.nextInt((int)(max - min));
            sleeper.sleep(min + delta);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
        }
    }

    @Override
    public String toString() {
        return "RandomBackOffPolicy[backOffPeriod=" + minBackOffPeriod + ", " + maxBackOffPeriod + "]";
    }
}
