package com.github.liyibo1110.spring.retry.interceptor;

/**
 * 判断特定的方法参数值，是不是新的（是则可以重置retryCount以及清空旧状态）
 * @author liyibo
 * @date 2026-01-29 11:29
 */
public interface NewMethodArgumentsIdentifier {
    boolean isNew(Object[] args);
}
