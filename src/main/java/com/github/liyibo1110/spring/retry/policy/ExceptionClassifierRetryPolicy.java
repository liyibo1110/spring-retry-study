package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.classify.Classifier;
import com.github.liyibo1110.spring.classify.ClassifierSupport;
import com.github.liyibo1110.spring.classify.SubclassClassifier;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据最新异常的值，动态适配一组已注入policy的policy实现
 * @author liyibo
 * @date 2026-01-28 10:15
 */
public class ExceptionClassifierRetryPolicy implements RetryPolicy {
    /**
     * Throwable -> RetryPolicy
     */
    private Classifier<Throwable, RetryPolicy> classifier = new ClassifierSupport<>(new NeverRetryPolicy());

    public void setPolicyMap(Map<Class<? extends Throwable>, RetryPolicy> policyMap) {
        this.classifier = new SubclassClassifier<>(policyMap, new NeverRetryPolicy());
    }

    public void setExceptionClassifier(Classifier<Throwable, RetryPolicy> classifier) {
        this.classifier = classifier;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        RetryPolicy policy = (RetryPolicy)context;  // 就是ExceptionClassifierRetryContext
        return policy.canRetry(context);    // 委托
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new ExceptionClassifierRetryContext(parent, classifier).open(parent);    // 委托
    }

    @Override
    public void close(RetryContext context) {
        RetryPolicy policy = (RetryPolicy)context;
        policy.close(context);  // 委托
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        RetryPolicy policy = (RetryPolicy) context;
        policy.registerThrowable(context, throwable);   // 委托
        ((RetryContextSupport)context).registerThrowable(throwable);
    }

    /**
     * 一个比较难理解的RetryContext实现，因为其跨界实现了RetryPolicy。
     * 因为在ExceptionClassifierRetryPolicy这个实现里，当前生效的RetryPolicy是要在运行时动态变化的（即每次失败可能会换个policy），所以得把“策略决策能力”放到Context里面。
     * 换句话说就是：这个Context本身，就是一个策略代理（policy proxy）
     */
    private static class ExceptionClassifierRetryContext extends RetryContextSupport implements RetryPolicy {
        final private Classifier<Throwable, RetryPolicy> classifier;
        /** 这次retry要使用的policy */
        private RetryPolicy policy;
        /** 这次retry要使用的context */
        private RetryContext context;
        final private Map<RetryPolicy, RetryContext> contexts = new HashMap<>();

        public ExceptionClassifierRetryContext(RetryContext parent, Classifier<Throwable, RetryPolicy> classifier) {
            super(parent);
            this.classifier = classifier;
        }

        /**
         * 实际干活的
         */
        @Override
        public boolean canRetry(RetryContext context) {
            return this.context == null || this.policy.canRetry(this.context);
        }

        @Override
        public RetryContext open(RetryContext parent) {
            return this;
        }

        @Override
        public void close(RetryContext context) {
            for(RetryPolicy policy : contexts.keySet())
                policy.close(getContext(policy, context.getParent()));
        }

        /**
         * 实际干活的
         */
        @Override
        public void registerThrowable(RetryContext context, Throwable throwable) {
            policy = classifier.classify(throwable);    // 找出这次对应的policy
            Assert.notNull(policy, "Could not locate policy for exception=[" + throwable + "].");
            this.context = getContext(policy, context.getParent());
            policy.registerThrowable(this.context, throwable);
        }

        private RetryContext getContext(RetryPolicy policy, RetryContext parent) {
            RetryContext context = contexts.get(policy);
            if(context == null) {
                context = policy.open(parent);
                contexts.put(policy, context);
            }
            return context;
        }
    }
}
