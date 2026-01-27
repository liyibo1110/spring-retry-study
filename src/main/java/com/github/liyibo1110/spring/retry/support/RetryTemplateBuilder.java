package com.github.liyibo1110.spring.retry.support;

import com.github.liyibo1110.spring.classify.BinaryExceptionClassifierBuilder;
import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.backoff.BackOffPolicy;
import com.github.liyibo1110.spring.retry.policy.AlwaysRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.MaxAttemptsRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.TimeoutRetryPolicy;
import org.springframework.util.Assert;

import java.time.Duration;
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

    /* ---------------- Configure exception classifier -------------- */

    /* ---------------- Add listeners -------------- */

    /* ---------------- Building -------------- */

    /* ---------------- Private utils -------------- */
}
