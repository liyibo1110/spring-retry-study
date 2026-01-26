package com.github.liyibo1110.spring.retry.backoff;

import com.github.liyibo1110.spring.retry.RetryContext;

/**
 * BackOffPolicy的简单实现类，在多次调用之间不维护任何状态
 * @author liyibo
 * @date 2026-01-25 00:32
 */
public abstract class StatelessBackOffPolicy implements BackOffPolicy {
    @Override
    public BackOffContext start(RetryContext context) {
        return null;
    }

    @Override
    public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
        this.doBackOff();
    }

    /**
     * 子类自行实现逻辑
     */
    protected abstract void doBackOff() throws BackOffInterruptedException;
}
