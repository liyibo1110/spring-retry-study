package com.github.liyibo1110.spring.retry.backoff;

import java.util.function.Supplier;

/**
 * @author liyibo
 * @date 2026-01-27 00:30
 */
public class BackOffPolicyBuilder {
    private static final long DEFAULT_INITIAL_DELAY = 1000L;
    private Long delay = DEFAULT_INITIAL_DELAY;
    private Long maxDelay;
    private Double multiplier;
    private Boolean random;
    private Sleeper sleeper;
    private Supplier<Long> delaySupplier;

    private Supplier<Long> maxDelaySupplier;

    private Supplier<Double> multiplierSupplier;

    private Supplier<Boolean> randomSupplier;

    private BackOffPolicyBuilder() {}

    public static BackOffPolicyBuilder newBuilder() {
        return new BackOffPolicyBuilder();
    }

    public static BackOffPolicy newDefaultPolicy() {
        return new BackOffPolicyBuilder().build();
    }

    public BackOffPolicyBuilder delay(long delay) {
        this.delay = delay;
        return this;
    }

    public BackOffPolicyBuilder maxDelay(long maxDelay) {
        this.maxDelay = maxDelay;
        return this;
    }

    public BackOffPolicyBuilder multiplier(double multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    public BackOffPolicyBuilder random(boolean random) {
        this.random = random;
        return this;
    }

    public BackOffPolicyBuilder sleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
        return this;
    }

    public BackOffPolicyBuilder delaySupplier(Supplier<Long> delaySupplier) {
        this.delaySupplier = delaySupplier;
        return this;
    }

    public BackOffPolicyBuilder maxDelaySupplier(Supplier<Long> maxDelaySupplier) {
        this.maxDelaySupplier = maxDelaySupplier;
        return this;
    }

    public BackOffPolicyBuilder multiplierSupplier(Supplier<Double> multiplierSupplier) {
        this.multiplierSupplier = multiplierSupplier;
        return this;
    }

    public BackOffPolicyBuilder randomSupplier(Supplier<Boolean> randomSupplier) {
        this.randomSupplier = randomSupplier;
        return this;
    }

    public BackOffPolicy build() {
        // Exponential系列实现
        if(multiplier != null && (multiplier > 0 || multiplierSupplier != null)) {
            ExponentialBackOffPolicy policy;
            if(isRandom())
                policy = new ExponentialRandomBackOffPolicy();
            else
                policy = new ExponentialBackOffPolicy();
            if(delay != null)
                policy.setInitialInterval(delay);
            if(delaySupplier != null)
                policy.initialIntervalSupplier(delaySupplier);
            if(multiplier != null)
                policy.setMultiplier(multiplier);
            if(multiplierSupplier != null)
                policy.multiplierSupplier(multiplierSupplier);
            if(maxDelay != null && delay != null)
                policy.setMaxInterval(
                        maxDelay > delay ? maxDelay : ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL);
            if(maxDelaySupplier != null)
                policy.maxIntervalSupplier(maxDelaySupplier);
            if(sleeper != null)
                policy.setSleeper(sleeper);
            return policy;
        }
        // UniformRandomBackOffPolicy实现
        if(maxDelay != null && delay != null && maxDelay > delay) {
            UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
            if(delay != null)
                policy.setMinBackOffPeriod(delay);
            if(delaySupplier != null)
                policy.minBackOffPeriodSupplier(delaySupplier);
            if(maxDelay != null)
                policy.setMaxBackOffPeriod(maxDelay);
            if(maxDelaySupplier != null)
                policy.maxBackOffPeriodSupplier(maxDelaySupplier);
            if(sleeper != null)
                policy.setSleeper(sleeper);
            return policy;
        }
        // 最后是FixedBackOffPolicy实现
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        if(delaySupplier != null)
            policy.backOffPeriodSupplier(delaySupplier);
        else if(delay != null)
            policy.setBackOffPeriod(delay);
        if(sleeper != null)
            policy.setSleeper(sleeper);
        return policy;
    }

    private boolean isRandom() {
        return (randomSupplier != null && Boolean.TRUE.equals(randomSupplier.get()))
                || Boolean.TRUE.equals(random);
    }
}
