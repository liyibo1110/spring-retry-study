package com.github.liyibo1110.spring.retry.interceptor;

import com.github.liyibo1110.spring.classify.BinaryExceptionClassifier;
import com.github.liyibo1110.spring.classify.Classifier;
import com.github.liyibo1110.spring.retry.RetryOperations;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.backoff.BackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.ExponentialBackOffPolicy;
import com.github.liyibo1110.spring.retry.policy.SimpleRetryPolicy;
import com.github.liyibo1110.spring.retry.support.RetryTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.util.Assert;

/**
 * 简化后的facade，用来构建有状态retry拦截器和无状态retry拦截器
 * @author liyibo
 * @date 2026-01-28 23:23
 */
public abstract class RetryInterceptorBuilder<T extends MethodInterceptor> {
    protected final RetryTemplate retryTemplate = new RetryTemplate();
    protected final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
    protected RetryOperations retryOperations;
    protected MethodInvocationRecoverer<?> recoverer;
    /** 上面的retryTemplate实例是否被显式设置过（比如下面的maxAttempts方法），如果设置过，就不能再setRetryOperations了 */
    private boolean templateAltered;
    private boolean backOffPolicySet;
    private boolean retryPolicySet;
    private boolean backOffOptionsSet;
    protected String label;

    public static StatefulRetryInterceptorBuilder stateful() {
        return new StatefulRetryInterceptorBuilder();
    }

    public static CircuitBreakerInterceptorBuilder circuitBreaker() {
        return new CircuitBreakerInterceptorBuilder();
    }

    public static StatelessRetryInterceptorBuilder stateless() {
        return new StatelessRetryInterceptorBuilder();
    }

    /**
     * 设置retryOperations字段，前提是retryTemplate不能被设置过。
     */
    public RetryInterceptorBuilder<T> retryOperations(RetryOperations retryOperations) {
        Assert.isTrue(!this.templateAltered, "Cannot set retryOperations when the default has been modified");
        this.retryOperations = retryOperations;
        return this;
    }

    /**
     * 给设置retryTemplate最大重试次数，前提是retryOperations和retryPolicy不能被设置过。
     */
    public RetryInterceptorBuilder<T> maxAttempts(int maxAttempts) {
        Assert.isNull(this.retryOperations, "cannot alter the retry policy when a custom retryOperations has been set");
        Assert.isTrue(!this.retryPolicySet, "cannot alter the retry policy when a custom retryPolicy has been set");
        this.simpleRetryPolicy.setMaxAttempts(maxAttempts);
        this.retryTemplate.setRetryPolicy(this.simpleRetryPolicy);
        this.templateAltered = true;
        return this;
    }

    /**
     * 给retryTemplate设置backoff policy，前提是retryOperations和backOffPolicy不能被设置过。
     */
    public RetryInterceptorBuilder<T> backOffOptions(long initialInterval, double multiplier, long maxInterval) {
        Assert.isNull(this.retryOperations, "cannot set the back off policy when a custom retryOperations has been set");
        Assert.isTrue(!this.backOffPolicySet, "cannot set the back off options when a back off policy has been set");
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(initialInterval);
        policy.setMultiplier(multiplier);
        policy.setMaxInterval(maxInterval);
        this.retryTemplate.setBackOffPolicy(policy);
        this.backOffOptionsSet = true;
        this.templateAltered = true;
        return this;
    }

    /**
     * 给retryTemplate设置retry policy，前提是retryOperations和retryTemplate不能被设置过。
     */
    public RetryInterceptorBuilder<T> retryPolicy(RetryPolicy policy) {
        Assert.isNull(this.retryOperations, "cannot set the retry policy when a custom retryOperations has been set");
        Assert.isTrue(!this.templateAltered, "cannot set the retry policy if max attempts or back off policy or options changed");
        this.retryTemplate.setRetryPolicy(policy);
        this.retryPolicySet = true;
        this.templateAltered = true;
        return this;
    }

    /**
     * 给retryTemplate设置backoff policy，前提是retryOperations和backoff policy不能被设置过。
     */
    public RetryInterceptorBuilder<T> backOffPolicy(BackOffPolicy policy) {
        Assert.isNull(this.retryOperations, "cannot set the back off policy when a custom retryOperations has been set");
        Assert.isTrue(!this.backOffOptionsSet, "cannot set the back off policy when the back off policy options have been set");
        this.retryTemplate.setBackOffPolicy(policy);
        this.templateAltered = true;
        this.backOffPolicySet = true;
        return this;
    }

    public RetryInterceptorBuilder<T> recoverer(MethodInvocationRecoverer<?> recoverer) {
        this.recoverer = recoverer;
        return this;
    }

    public RetryInterceptorBuilder<T> label(String label) {
        this.label = label;
        return this;
    }

    /**
     * 生成拦截器实例
     */
    public abstract T build();

    private RetryInterceptorBuilder() {}

    public static class StatefulRetryInterceptorBuilder extends RetryInterceptorBuilder<StatefulRetryOperationsInterceptor> {
        private final StatefulRetryOperationsInterceptor interceptor = new StatefulRetryOperationsInterceptor();
        private MethodArgumentsKeyGenerator keyGenerator;
        private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;
        private Classifier<? super Throwable, Boolean> rollbackClassifier;

        public StatefulRetryInterceptorBuilder keyGenerator(MethodArgumentsKeyGenerator keyGenerator) {
            this.keyGenerator = keyGenerator;
            return this;
        }

        public StatefulRetryInterceptorBuilder newMethodArgumentsIdentifier(NewMethodArgumentsIdentifier newMethodArgumentsIdentifier) {
            this.newMethodArgumentsIdentifier = newMethodArgumentsIdentifier;
            return this;
        }

        public StatefulRetryInterceptorBuilder rollbackFor(Classifier<? super Throwable, Boolean> rollbackClassifier) {
            this.rollbackClassifier = rollbackClassifier;
            return this;
        }

        @Override
        public StatefulRetryInterceptorBuilder retryOperations(RetryOperations retryOperations) {
            super.retryOperations(retryOperations);
            return this;
        }

        @Override
        public StatefulRetryInterceptorBuilder maxAttempts(int maxAttempts) {
            super.maxAttempts(maxAttempts);
            return this;
        }

        @Override
        public StatefulRetryInterceptorBuilder backOffOptions(long initialInterval, double multiplier, long maxInterval) {
            super.backOffOptions(initialInterval, multiplier, maxInterval);
            return this;
        }

        @Override
        public StatefulRetryInterceptorBuilder retryPolicy(RetryPolicy policy) {
            super.retryPolicy(policy);
            return this;
        }

        @Override
        public StatefulRetryInterceptorBuilder backOffPolicy(BackOffPolicy policy) {
            super.backOffPolicy(policy);
            return this;
        }

        @Override
        public StatefulRetryInterceptorBuilder recoverer(MethodInvocationRecoverer<?> recoverer) {
            super.recoverer(recoverer);
            return this;
        }

        @Override
        public StatefulRetryOperationsInterceptor build() {
            if(this.recoverer != null)
                this.interceptor.setRecoverer(this.recoverer);
            if(this.retryOperations != null)
                this.interceptor.setRetryOperations(this.retryOperations);
            else
                this.interceptor.setRetryOperations(this.retryTemplate);
            if(this.keyGenerator != null)
                this.interceptor.setKeyGenerator(this.keyGenerator);
            if(this.rollbackClassifier != null)
                this.interceptor.setRollbackClassifier(this.rollbackClassifier);
            if(this.newMethodArgumentsIdentifier != null)
                this.interceptor.setNewItemIdentifier(this.newMethodArgumentsIdentifier);
            if(this.label != null)
                this.interceptor.setLabel(this.label);
            return this.interceptor;
        }

        private StatefulRetryInterceptorBuilder() {}
    }

    /**
     * 特殊的StatefulRetryOperationsInterceptor（固定key + CircuitBreakerRetryPolicy + RetryContextCache + 特殊的open/close/canRetry语义）
     * 关于熔断的功能实现在了CircuitBreakerRetryPolicy（特点是把RetryContext当作断路器状态机来用）里面。
     * 所以行为实际变化很大。
     */
    public static class CircuitBreakerInterceptorBuilder extends RetryInterceptorBuilder<StatefulRetryOperationsInterceptor> {
        private final StatefulRetryOperationsInterceptor interceptor = new StatefulRetryOperationsInterceptor();
        private MethodArgumentsKeyGenerator keyGenerator;

        @Override
        public CircuitBreakerInterceptorBuilder retryOperations(RetryOperations retryOperations) {
            super.retryOperations(retryOperations);
            return this;
        }

        @Override
        public CircuitBreakerInterceptorBuilder maxAttempts(int maxAttempts) {
            super.maxAttempts(maxAttempts);
            return this;
        }

        @Override
        public CircuitBreakerInterceptorBuilder retryPolicy(RetryPolicy policy) {
            super.retryPolicy(policy);
            return this;
        }

        public CircuitBreakerInterceptorBuilder keyGenerator(MethodArgumentsKeyGenerator keyGenerator) {
            this.keyGenerator = keyGenerator;
            return this;
        }

        @Override
        public CircuitBreakerInterceptorBuilder recoverer(MethodInvocationRecoverer<?> recoverer) {
            super.recoverer(recoverer);
            return this;
        }

        @Override
        public StatefulRetryOperationsInterceptor build() {
            if(this.recoverer != null)
                this.interceptor.setRecoverer(this.recoverer);
            if(this.retryOperations != null)
                this.interceptor.setRetryOperations(this.retryOperations);
            else
                this.interceptor.setRetryOperations(this.retryTemplate);
            if(this.keyGenerator != null)
                this.interceptor.setKeyGenerator(this.keyGenerator);
            if(this.label != null)
                this.interceptor.setLabel(this.label);
            this.interceptor.setRollbackClassifier(new BinaryExceptionClassifier(false));
            return this.interceptor;
        }

        private CircuitBreakerInterceptorBuilder() {}
    }

    public static class StatelessRetryInterceptorBuilder extends RetryInterceptorBuilder<RetryOperationsInterceptor> {
        private final RetryOperationsInterceptor interceptor = new RetryOperationsInterceptor();
        @Override
        public RetryOperationsInterceptor build() {
            if(this.recoverer != null)
                this.interceptor.setRecoverer(this.recoverer);
            if(this.retryOperations != null)
                this.interceptor.setRetryOperations(this.retryOperations);
            else
                this.interceptor.setRetryOperations(this.retryTemplate);
            if(this.label != null)
                this.interceptor.setLabel(this.label);
            return this.interceptor;
        }

        private StatelessRetryInterceptorBuilder() {}
    }
}
