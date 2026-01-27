package com.github.liyibo1110.spring.retry.backoff;

import com.github.liyibo1110.spring.retry.RetryContext;

import java.util.Random;
import java.util.function.Supplier;

/**
 * ExponentialBackOffPolicy的扩展实现，选择了一个随机倍数，来自于一个简单的确定性指数。
 * 随机倍数在1和确定性倍数之间均匀分布，例如：initialInterval = 50，multiplier = 2.0，maxInterval = 3000，numRetries = 5
 * ExponentialBackOffPolicy产生的结果为：[50, 100, 200, 400, 800]，
 * ExponentialRandomBackOffPolicy可能产生的结果为：[76, 151, 304, 580, 901]或[53, 190, 267, 451, 815],
 * 即取[50-100, 100-200, 200-400, 400-800, 800-1600]范围内随机分布的值
 * @author liyibo
 * @date 2026-01-27 00:24
 */
public class ExponentialRandomBackOffPolicy extends ExponentialBackOffPolicy {

    @Override
    public BackOffContext start(RetryContext context) {
        return new ExponentialRandomBackOffContext(getInitialInterval(), getMultiplier(), getMaxInterval(),
                getInitialIntervalSupplier(), getMultiplierSupplier(), getMaxIntervalSupplier());
    }

    @Override
    protected ExponentialBackOffPolicy newInstance() {
        return new ExponentialRandomBackOffPolicy();
    }

    static class ExponentialRandomBackOffContext extends ExponentialBackOffPolicy.ExponentialBackOffContext {
        private final Random r = new Random();

        public ExponentialRandomBackOffContext(long expSeed, double multiplier, long maxInterval, Supplier<Long> expSeedSupplier, Supplier<Double> multiplierSupplier, Supplier<Long> maxIntervalSupplier) {
            super(expSeed, multiplier, maxInterval, expSeedSupplier, multiplierSupplier, maxIntervalSupplier);
        }
        @Override
        public synchronized long getSleepAndIncrement() {
            long next = super.getSleepAndIncrement();
            next = (long)(next * (1 + r.nextFloat() * (getMultiplier() - 1)));
            if(next > super.getMaxInterval())
                next = super.getMaxInterval();
            return next;
        }

    }
}
