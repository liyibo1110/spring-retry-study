package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;

/**
 * 只允许retry第一次的实现类，主要用于其它实现类的父类
 * @author liyibo
 * @date 2026-01-26 23:12
 */
public class NeverRetryPolicy implements RetryPolicy {
    @Override
    public boolean canRetry(RetryContext context) {
        return !((NeverRetryContext)context).isFinished();
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new NeverRetryContext(parent);
    }

    @Override
    public void close(RetryContext context) {
        // nothing to do
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        // 失败就立刻标记为结束，符合never的语义
        ((NeverRetryContext)context).setFinished();
        // 调用RetryContextSupport类的版本
        ((RetryContextSupport)context).registerThrowable(throwable);
    }

    /**
     * NeverRetryPolicy对应的RetryContext实现。
     * 实现了一个与RetryContext.isExhaustedOnly()功能相似的标记，但保持独立
     * 以便NeverRetryPolicy的子类在需要时可以修改canRetry的行为，而不会影响isExhaustedOnly()
     */
    private static class NeverRetryContext extends RetryContextSupport {
        private boolean finished = false;

        public NeverRetryContext(RetryContext parent) {
            super(parent);
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished() {
            this.finished = true;
        }
    }
}
