package com.github.liyibo1110.spring.retry.backoff;

import com.github.liyibo1110.spring.retry.RetryContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.function.Supplier;

/**
 * 每次retry都会增加一个时间限制值的BackOffPolicy实现（只能用在stateful，因此每次retry之间的backoff时间会增加），通过Sleeper组件实现的暂停
 * @author liyibo
 * @date 2026-01-27 00:01
 */
public class ExponentialBackOffPolicy implements SleepingBackOffPolicy<ExponentialBackOffPolicy>  {
    protected final Log logger = LogFactory.getLog(this.getClass());
    public static final long DEFAULT_INITIAL_INTERVAL = 100L;
    public static final long DEFAULT_MAX_INTERVAL = 30000L;
    /** 增长指数为2倍 */
    public static final double DEFAULT_MULTIPLIER = 2;

    /* =============== 以下为字段，上面只是常量 =============== */

    private long initialInterval = DEFAULT_INITIAL_INTERVAL;
    private long maxInterval = DEFAULT_MAX_INTERVAL;
    private double multiplier = DEFAULT_MULTIPLIER;
    private Supplier<Long> initialIntervalSupplier;
    private Supplier<Long> maxIntervalSupplier;
    private Supplier<Double> multiplierSupplier;
    private Sleeper sleeper = new ThreadWaitSleeper();

    public void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    @Override
    public ExponentialBackOffPolicy withSleeper(Sleeper sleeper) {
        ExponentialBackOffPolicy res = newInstance();
        cloneValues(res);
        res.setSleeper(sleeper);
        return res;
    }

    protected ExponentialBackOffPolicy newInstance() {
        return new ExponentialBackOffPolicy();
    }

    protected void cloneValues(ExponentialBackOffPolicy target) {
        target.setInitialInterval(getInitialInterval());
        target.setMaxInterval(getMaxInterval());
        target.setMultiplier(getMultiplier());
        target.setSleeper(this.sleeper);
    }

    public void setInitialInterval(long initialInterval) {
        this.initialInterval = initialInterval > 1 ? initialInterval : 1;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier > 1.0 ? multiplier : 1.0;
    }

    public void setMaxInterval(long maxInterval) {
        this.maxInterval = maxInterval > 0 ? maxInterval : 1;
    }

    public void initialIntervalSupplier(Supplier<Long> initialIntervalSupplier) {
        Assert.notNull(initialIntervalSupplier, "'initialIntervalSupplier' cannot be null");
        this.initialIntervalSupplier = initialIntervalSupplier;
    }

    public void multiplierSupplier(Supplier<Double> multiplierSupplier) {
        Assert.notNull(multiplierSupplier, "'multiplierSupplier' cannot be null");
        this.multiplierSupplier = multiplierSupplier;
    }

    public void maxIntervalSupplier(Supplier<Long> maxIntervalSupplier) {
        Assert.notNull(maxIntervalSupplier, "'maxIntervalSupplier' cannot be null");
        this.maxIntervalSupplier = maxIntervalSupplier;
    }

    protected Supplier<Long> getInitialIntervalSupplier() {
        return initialIntervalSupplier;
    }

    protected Supplier<Long> getMaxIntervalSupplier() {
        return maxIntervalSupplier;
    }

    protected Supplier<Double> getMultiplierSupplier() {
        return multiplierSupplier;
    }

    public long getInitialInterval() {
        return this.initialIntervalSupplier != null ? this.initialIntervalSupplier.get() : this.initialInterval;
    }

    public long getMaxInterval() {
        return this.maxIntervalSupplier != null ? this.maxIntervalSupplier.get() : this.maxInterval;
    }

    public double getMultiplier() {
        return this.multiplierSupplier != null ? this.multiplierSupplier.get() : this.multiplier;
    }

    @Override
    public BackOffContext start(RetryContext context) {
        return new ExponentialBackOffContext(this.initialInterval, this.multiplier, this.maxInterval,
                this.initialIntervalSupplier, this.multiplierSupplier, this.maxIntervalSupplier);
    }

    @Override
    public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
        ExponentialBackOffContext context = (ExponentialBackOffContext)backOffContext;
        try {
            long sleepTime = context.getSleepAndIncrement();
            if(this.logger.isDebugEnabled())
                this.logger.debug("Sleeping for " + sleepTime);
            this.sleeper.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
        }
    }

    static class ExponentialBackOffContext implements BackOffContext {
        private final double multiplier;
        private long interval;
        private final long maxInterval;
        private Supplier<Long> initialIntervalSupplier;
        private Supplier<Double> multiplierSupplier;
        private Supplier<Long> maxIntervalSupplier;

        public ExponentialBackOffContext(long interval, double multiplier, long maxInterval,
                                         Supplier<Long> intervalSupplier, Supplier<Double> multiplierSupplier,
                                         Supplier<Long> maxIntervalSupplier) {
            this.interval = interval;
            this.multiplier = multiplier;
            this.maxInterval = maxInterval;
            this.initialIntervalSupplier = intervalSupplier;
            this.multiplierSupplier = multiplierSupplier;
            this.maxIntervalSupplier = maxIntervalSupplier;
        }

        /**
         * 获取此轮要暂停的时间间隔，以后自动出下一轮的时间间隔
         */
        public synchronized long getSleepAndIncrement() {
            long sleep = getInterval();
            long max = getMaxInterval();
            if(sleep > max)
                sleep = max;
            else
                this.interval = getNextInterval();
            return sleep;
        }

        /**
         * 获取下一次的暂停间隔
         */
        protected long getNextInterval() {
            return (long) (this.interval * getMultiplier());
        }

        public double getMultiplier() {
            return this.multiplierSupplier != null ? this.multiplierSupplier.get() : this.multiplier;
        }

        /**
         * 获取本次的暂停间隔
         */
        public long getInterval() {
            if(this.initialIntervalSupplier != null) {
                this.interval = this.initialIntervalSupplier.get();
                this.initialIntervalSupplier = null;
            }
            return this.interval;
        }

        public long getMaxInterval() {
            return this.maxIntervalSupplier != null ? this.maxIntervalSupplier.get() : this.maxInterval;
        }
    }

    @Override
    public String toString() {
        return ClassUtils.getShortName(getClass()) + "[initialInterval=" + getInitialInterval() + ", multiplier="
                + getMultiplier() + ", maxInterval=" + getMaxInterval() + "]";
    }
}
