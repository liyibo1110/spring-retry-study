package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 由多个retry policy组成的列表，并按照顺序将调用委托给这些policy的特殊实现类
 * @author liyibo
 * @date 2026-01-28 11:27
 */
public class CompositeRetryPolicy implements RetryPolicy {
    RetryPolicy[] policies = new RetryPolicy[0];
    /**
     * 乐观标记，为true则表示所有的policy，有一个canRetry通过，就算通过，为false表示所有policy，有一个canRetry不通过，则整个不通过
     */
    private boolean optimistic = false;

    public void setOptimistic(boolean optimistic) {
        this.optimistic = optimistic;
    }

    public void setPolicies(RetryPolicy[] policies) {
        this.policies = Arrays.asList(policies).toArray(new RetryPolicy[policies.length]);
    }

    @Override
    public boolean canRetry(RetryContext context) {
        RetryContext[] contexts = ((CompositeRetryContext)context).contexts;
        RetryPolicy[] policies = ((CompositeRetryContext)context).policies;
        boolean retryable = true;

        if(this.optimistic) {
            retryable = false;
            for(int i = 0; i < contexts.length; i++) {
                if(policies[i].canRetry(contexts[i]))
                    return true;
            }
        }else {
            for(int i = 0; i < contexts.length; i++) {
                if(!policies[i].canRetry(contexts[i]))
                    retryable = false;
            }
        }

        return retryable;
    }

    @Override
    public RetryContext open(RetryContext parent) {
        List<RetryContext> list = new ArrayList<>();
        for(RetryPolicy policy : this.policies)
            list.add(policy.open(parent));
        return new CompositeRetryContext(parent, list, this.policies);
    }

    @Override
    public void close(RetryContext context) {   // context就是CompositeRetryContext类型
        RetryContext[] contexts = ((CompositeRetryContext)context).contexts;
        RetryPolicy[] policies = ((CompositeRetryContext)context).policies;
        RuntimeException exception = null;
        for(int i = 0; i < contexts.length; i++) {
            try {
                policies[i].close(contexts[i]);
            } catch (RuntimeException e) {
                if(exception == null)
                    exception = e;
            }
        }
        if(exception != null)
            throw exception;
    }

    /**
     * 返回policies里面所有的maxAttempts次数，找出最少的那次，都没有设置则返回默认（-1）
     */
    @Override
    public int getMaxAttempts() {
        return Arrays.stream(policies)
                .map(RetryPolicy::getMaxAttempts)
                .filter(maxAttempts -> maxAttempts != NO_MAXIMUM_ATTEMPTS_SET)
                .sorted()
                .findFirst()
                .orElse(NO_MAXIMUM_ATTEMPTS_SET);
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        RetryContext[] contexts = ((CompositeRetryContext)context).contexts;
        RetryPolicy[] policies = ((CompositeRetryContext)context).policies;
        for(int i = 0; i < contexts.length; i++)
            policies[i].registerThrowable(contexts[i], throwable);
        // 不要漏了这个
        ((RetryContextSupport)context).registerThrowable(throwable);
    }

    private static class CompositeRetryContext extends RetryContextSupport {
        RetryContext[] contexts;
        RetryPolicy[] policies;

        public CompositeRetryContext(RetryContext parent, List<RetryContext> contexts, RetryPolicy[] policies) {
            super(parent);
            this.contexts = contexts.toArray(new RetryContext[contexts.size()]);
            this.policies = policies;
        }
    }
}
