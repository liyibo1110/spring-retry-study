package com.github.liyibo1110.spring.retry.interceptor;

import com.github.liyibo1110.spring.retry.RetryCallback;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于Spring AOP的callback实现。
 * @author liyibo
 * @date 2026-01-28 13:52
 */
public abstract class MethodInvocationRetryCallback<T, E extends Throwable> implements RetryCallback<T, E> {
    protected final MethodInvocation invocation;
    protected final String label;

    public MethodInvocationRetryCallback(MethodInvocation invocation, String label) {
        this.invocation = invocation;
        if(StringUtils.hasText(label))
            this.label = label;
        else
            this.label = ClassUtils.getQualifiedMethodName(invocation.getMethod());
    }

    public MethodInvocation getInvocation() {
        return this.invocation;
    }

    @Override
    public String getLabel() {
        return this.label;
    }
}
