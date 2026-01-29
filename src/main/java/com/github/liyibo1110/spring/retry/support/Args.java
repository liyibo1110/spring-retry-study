package com.github.liyibo1110.spring.retry.support;

/**
 * 一个包含在表达式计算中要使用的method参数的根对象。
 * 在首次调用retryable方法之前，这些参数不可用（空值），
 * 通常只对maxAttempts有影响，即这些参数不能用于表示maxAttempts=0的情况
 * @author liyibo
 * @date 2026-01-28 14:33
 */
public class Args {
    public static final Args NO_ARGS = new Args(new Object[100]);

    private final Object[] args;

    public Args(Object[] args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
