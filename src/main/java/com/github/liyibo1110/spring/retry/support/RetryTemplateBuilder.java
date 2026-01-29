package com.github.liyibo1110.spring.retry.support;

import com.github.liyibo1110.spring.classify.BinaryExceptionClassifier;
import com.github.liyibo1110.spring.classify.BinaryExceptionClassifierBuilder;
import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.backoff.BackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.ExponentialBackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.ExponentialRandomBackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.FixedBackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.NoBackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.UniformRandomBackOffPolicy;
import com.github.liyibo1110.spring.retry.policy.AlwaysRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.BinaryExceptionClassifierRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.CompositeRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.MaxAttemptsRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.PredicateRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.TimeoutRetryPolicy;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * RetryTemplate的构建器
 * @author liyibo
 * @date 2026-01-26 18:27
 */
public class RetryTemplateBuilder {
    private RetryPolicy baseRetryPolicy;
    private BackOffPolicy backOffPolicy;
    private List<RetryListener> listeners;
    private BinaryExceptionClassifierBuilder classifierBuilder;
    private Predicate<Throwable> retryOnPredicate;

    /* ---------------- Configure retry policy -------------- */
    public RetryTemplateBuilder maxAttempts(int maxAttempts) {
        Assert.isTrue(maxAttempts > 0, "Number of attempts should be positive");
        Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
        this.baseRetryPolicy = new MaxAttemptsRetryPolicy(maxAttempts);
        return this;
    }

    public RetryTemplateBuilder withTimeout(long timeoutMillis) {
        Assert.isTrue(timeoutMillis > 0, "timeoutMillis should be greater than 0");
        Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
        this.baseRetryPolicy = new TimeoutRetryPolicy(timeoutMillis);
        return this;
    }

    public RetryTemplateBuilder withTimeout(Duration timeout) {
        Assert.notNull(timeout, "timeout must not be null");
        return withTimeout(timeout.toMillis());
    }

    public RetryTemplateBuilder infiniteRetry() {
        Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
        this.baseRetryPolicy = new AlwaysRetryPolicy();
        return this;
    }

    public RetryTemplateBuilder customPolicy(RetryPolicy policy) {
        Assert.notNull(policy, "Policy should not be null");
        Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
        this.baseRetryPolicy = policy;
        return this;
    }

    /* ---------------- Configure backoff policy -------------- */

    public RetryTemplateBuilder exponentialBackoff(long initialInterval, double multiplier, long maxInterval) {
        return exponentialBackoff(initialInterval, multiplier, maxInterval, false);
    }

    public RetryTemplateBuilder exponentialBackoff(Duration initialInterval, double multiplier, Duration maxInterval) {
        Assert.notNull(initialInterval, "initialInterval must not be null");
        Assert.notNull(maxInterval, "maxInterval must not be null");
        return exponentialBackoff(initialInterval.toMillis(), multiplier, maxInterval.toMillis(), false);
    }

    public RetryTemplateBuilder exponentialBackoff(long initialInterval, double multiplier, long maxInterval,
                                                   boolean withRandom) {
        Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
        Assert.isTrue(initialInterval >= 1, "Initial interval should be >= 1");
        Assert.isTrue(multiplier > 1, "Multiplier should be > 1");
        Assert.isTrue(maxInterval > initialInterval, "Max interval should be > than initial interval");
        ExponentialBackOffPolicy policy = withRandom ? new ExponentialRandomBackOffPolicy() : new ExponentialBackOffPolicy();
        policy.setInitialInterval(initialInterval);
        policy.setMultiplier(multiplier);
        policy.setMaxInterval(maxInterval);
        this.backOffPolicy = policy;
        return this;
    }

    public RetryTemplateBuilder exponentialBackoff(Duration initialInterval, double multiplier, Duration maxInterval,
                                                   boolean withRandom) {
        Assert.notNull(initialInterval, "initialInterval most not be null");
        Assert.notNull(maxInterval, "maxInterval must not be null");
        return this.exponentialBackoff(initialInterval.toMillis(), multiplier, maxInterval.toMillis(), withRandom);
    }

    public RetryTemplateBuilder fixedBackoff(long interval) {
        Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
        Assert.isTrue(interval >= 1, "Interval should be >= 1");
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(interval);
        this.backOffPolicy = policy;
        return this;
    }

    public RetryTemplateBuilder fixedBackoff(Duration interval) {
        Assert.notNull(interval, "interval must not be null");
        long millis = interval.toMillis();
        Assert.isTrue(millis >= 1, "interval is less than 1 millisecond");
        return this.fixedBackoff(millis);
    }

    public RetryTemplateBuilder uniformRandomBackoff(long minInterval, long maxInterval) {
        Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
        Assert.isTrue(minInterval >= 1, "Min interval should be >= 1");
        Assert.isTrue(maxInterval >= 1, "Max interval should be >= 1");
        Assert.isTrue(maxInterval > minInterval, "Max interval should be > than min interval");
        UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
        policy.setMinBackOffPeriod(minInterval);
        policy.setMaxBackOffPeriod(maxInterval);
        this.backOffPolicy = policy;
        return this;
    }

    public RetryTemplateBuilder uniformRandomBackoff(Duration minInterval, Duration maxInterval) {
        Assert.notNull(minInterval, "minInterval must not be null");
        Assert.notNull(maxInterval, "maxInterval must not be null");
        return this.uniformRandomBackoff(minInterval.toMillis(), maxInterval.toMillis());
    }

    public RetryTemplateBuilder noBackoff() {
        Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
        this.backOffPolicy = new NoBackOffPolicy();
        return this;
    }

    public RetryTemplateBuilder customBackoff(BackOffPolicy backOffPolicy) {
        Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
        Assert.notNull(backOffPolicy, "You should provide non null custom policy");
        this.backOffPolicy = backOffPolicy;
        return this;
    }

    /* ---------------- Configure exception classifier -------------- */

    /**
     * 增加要retry的异常
     */
    public RetryTemplateBuilder retryOn(Class<? extends Throwable> t) {
        classifierBuilder().retryOn(t);
        return this;
    }

    /**
     * 增加要retry的排除异常
     */
    public RetryTemplateBuilder notRetryOn(Class<? extends Throwable> t) {
        classifierBuilder().notRetryOn(t);
        return this;
    }

    /**
     * 增加要retry的异常
     */
    public RetryTemplateBuilder retryOn(List<Class<? extends Throwable>> exceptions) {
        for(final Class<? extends Throwable> e : exceptions)
            classifierBuilder().retryOn(e);
        return this;
    }

    /**
     * 增加要retry的排除异常
     */
    public RetryTemplateBuilder notRetryOn(List<Class<? extends Throwable>> exceptions) {
        for(final Class<? extends Throwable> e : exceptions)
            classifierBuilder().notRetryOn(e);
        return this;
    }

    /**
     * 根据给定的Predicate判断对应的Throwable是否要retry。
     * 这个不能和retryOn或者notRetryOn混合使用。
     */
    public RetryTemplateBuilder retryOn(Predicate<Throwable> predicate) {
        Assert.isTrue(this.classifierBuilder == null && this.retryOnPredicate == null,
                "retryOn(Predicate<Throwable>) cannot be mixed with other retryOn() or noRetryOn()");
        Assert.notNull(predicate, "Predicate can not be null");
        this.retryOnPredicate = predicate;
        return this;
    }

    /**
     * 开启Classifier的traversing标记
     */
    public RetryTemplateBuilder traversingCauses() {
        classifierBuilder().traversingCauses();
        return this;
    }

    /* ---------------- Add listeners -------------- */

    /**
     * 增加listener
     */
    public RetryTemplateBuilder withListener(RetryListener listener) {
        Assert.notNull(listener, "Listener should not be null");
        listenersList().add(listener);
        return this;
    }

    public RetryTemplateBuilder withListeners(List<RetryListener> listeners) {
        for(RetryListener listener : listeners)
            Assert.notNull(listener, "Listener should not be null");
        listenersList().addAll(listeners);
        return this;
    }

    /* ---------------- Building -------------- */

    public RetryTemplate build() {
        RetryTemplate template = new RetryTemplate();
        // retry policy
        if(this.baseRetryPolicy == null)
            this.baseRetryPolicy = new MaxAttemptsRetryPolicy();    // 默认policy

        RetryPolicy exceptionRetryPolicy;
        if(this.retryOnPredicate == null) {
            BinaryExceptionClassifier exceptionClassifier = this.classifierBuilder == null
                    ? BinaryExceptionClassifier.defaultClassifier() : this.classifierBuilder.build();
            exceptionRetryPolicy = new BinaryExceptionClassifierRetryPolicy(exceptionClassifier);
        }else {
            exceptionRetryPolicy = new PredicateRetryPolicy(this.retryOnPredicate);
        }

        CompositeRetryPolicy finalPolicy = new CompositeRetryPolicy();
        finalPolicy.setPolicies(new RetryPolicy[] { this.baseRetryPolicy, exceptionRetryPolicy });
        template.setRetryPolicy(finalPolicy);

        // backoff policy
        if(this.backOffPolicy == null)
            this.backOffPolicy = new NoBackOffPolicy();
        template.setBackOffPolicy(this.backOffPolicy);

        // listeners
        if(this.listeners != null)
            template.setListeners(this.listeners.toArray(new RetryListener[0]));

        return template;
    }

    /* ---------------- Private utils -------------- */

    /**
     * 返回classifierBuilder字段，为null就先初始化
     */
    private BinaryExceptionClassifierBuilder classifierBuilder() {
        if(this.classifierBuilder == null)
            this.classifierBuilder = new BinaryExceptionClassifierBuilder();
        return this.classifierBuilder;
    }

    /**
     * 返回listeners字段，为null就先初始化
     */
    private List<RetryListener> listenersList() {
        if(this.listeners == null)
            this.listeners = new ArrayList<>();
        return this.listeners;
    }
}
