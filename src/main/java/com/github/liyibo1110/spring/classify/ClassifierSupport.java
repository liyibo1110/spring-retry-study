package com.github.liyibo1110.spring.classify;

/**
 * Classifier接口的骨架Support类（就是提供了classify的方法的默认实现）
 * @author liyibo
 * @date 2026-01-28 10:18
 */
public class ClassifierSupport<C, T> implements Classifier<C, T> {
    final private T defaultValue;

    public ClassifierSupport(T defaultValue) {
        super();
        this.defaultValue = defaultValue;
    }

    @Override
    public T classify(C throwable) {
        return this.defaultValue;
    }
}
