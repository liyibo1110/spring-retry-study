package com.github.liyibo1110.spring.retry.interceptor;

/**
 * 方法key的特殊实现，不考虑具体参数，直接把label当作key（熔断器interceptor专用）
 * @author liyibo
 * @date 2026-01-30 14:48
 */
public class FixedKeyGenerator implements MethodArgumentsKeyGenerator {
    private final String label;

    public FixedKeyGenerator(String label) {
        this.label = label;
    }

    @Override
    public Object getKey(Object[] item) {
        return this.label;
    }
}
