package com.github.liyibo1110.spring.retry.backoff;

/**
 * 不执行任何操作的BackOffPolicy实现。
 * 给定集合中的所有retry都会一个接一个地执行，中间没有停顿
 * @author liyibo
 * @date 2026-01-25 00:42
 */
public class NoBackOffPolicy extends StatelessBackOffPolicy {
    @Override
    protected void doBackOff() throws BackOffInterruptedException {

    }

    @Override
    public String toString() {
        return "NoBackOffPolicy []";
    }
}
