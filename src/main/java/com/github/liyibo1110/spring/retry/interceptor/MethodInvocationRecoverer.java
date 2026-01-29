package com.github.liyibo1110.spring.retry.interceptor;

/**
 * retry最终失败的recover策略接口
 * @author liyibo
 * @date 2026-01-28 14:30
 */
public interface MethodInvocationRecoverer<T> {
    /**
     * 尝试恢复错误或继续抛出异常
     */
    T recover(Object[] args, Throwable cause);
}
