package com.github.liyibo1110.spring.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于映射的参数化对象类型分类实现，根据与提供的类型映射的继承关系对对象进行分类。
 * 如果待分类的对象key存在（或者是其中一个key的子类），则返回对应的value值，否则返回默认null值。
 * @author liyibo
 * @date 2026-01-26 00:26
 */
public class SubclassClassifier<T, C> implements Classifier<T, C> {
    private ConcurrentMap<Class<? extends T>, C> classified;
    private C defaultValue;

    public SubclassClassifier() {
        this(null);
    }

    public SubclassClassifier(C defaultValue) {
        this(new HashMap<>(), defaultValue);
    }

    public SubclassClassifier(Map<Class<? extends T>, C> typeMap, C defaultValue) {
        super();
        this.classified = new ConcurrentHashMap<>(typeMap);
        this.defaultValue = defaultValue;
    }

    public void setDefaultValue(C defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setTypeMap(Map<Class<? extends T>, C> map) {
        this.classified = new ConcurrentHashMap<>(map);
    }

    public void add(Class<? extends T> type, C target) {
        this.classified.put(type, target);
    }

    @Override
    public C classify(T classifiable) {
        if(classifiable == null)
            return this.defaultValue;

        // 先找自己的类
        Class<? extends T> exceptionClass = (Class<? extends T>)classifiable.getClass();
        if(this.classified.containsKey(exceptionClass))
            return this.classified.get(exceptionClass);
        // 找不到再尝试给定类的父类，直到Object截止
        C value = null;
        for(Class<?> cls = exceptionClass.getSuperclass();
            !cls.equals(Object.class) && value == null; cls = cls.getSuperclass()) {
            value = this.classified.get(cls);
        }
        // 找不到再尝试给定类实现的接口，直到Object截止
        if(value == null) {
            for(Class<?> cls = exceptionClass; !cls.equals(Object.class) && value == null;
            cls = cls.getSuperclass()) {
                for(Class<?> ifc : cls.getInterfaces()) {
                    value = this.classified.get(ifc);
                    if(value != null)
                        break;
                }
            }
        }

        if(value != null)
            this.classified.put(exceptionClass, value);

        if(value == null)
            value = this.defaultValue;
        
        return null;
    }

    final public C getDefault() {
        return this.defaultValue;
    }

    protected Map<Class<? extends T>, C> getClassified() {
        return this.classified;
    }
}
