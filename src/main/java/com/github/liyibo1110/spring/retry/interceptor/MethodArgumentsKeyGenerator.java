package com.github.liyibo1110.spring.retry.interceptor;

/**
 * 根据method的实际参数值，生成stateful的key
 * @author liyibo
 * @date 2026-01-29 10:48
 */
public interface MethodArgumentsKeyGenerator {

    /**
     * 根据给定的参数值，生成key
     * 注意传来的只有参数值，并没有整个Method实例
     */
    Object getKey(Object[] item);
}
