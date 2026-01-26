package com.github.liyibo1110.spring.classify;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Throwable -> true/false的精确实现
 * @author liyibo
 * @date 2026-01-26 13:36
 */
public class BinaryExceptionClassifier extends SubclassClassifier<Throwable, Boolean> {
    /** 是否遍历Throwable的cause */
    private boolean traverseCauses;

    /**
     * 除非明确说了哪些异常，否则一律按defaultValue处理
     */
    public BinaryExceptionClassifier(boolean defaultValue) {
        super(defaultValue);
    }

    /**
     * 传进来的异常，结果才是true
     */
    public BinaryExceptionClassifier(Collection<Class<? extends Throwable>> exceptionClasses) {
        this(exceptionClasses, true);
    }

    /**
     * 完全自定义映射关系
     */
    public BinaryExceptionClassifier(Map<Class<? extends Throwable>, Boolean> typeMap,
                                     boolean defaultValue) {
        super(typeMap, defaultValue);
    }

    /**
     * 完全自定义映射关系
     */
    public BinaryExceptionClassifier(Map<Class<? extends Throwable>, Boolean> typeMap) {
        this(typeMap, false);
    }

    public BinaryExceptionClassifier(Collection<Class<? extends Throwable>> exceptionClasses,
                                     boolean value) {
        this(!value);
        if(exceptionClasses != null) {
            Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
            for(Class<? extends Throwable> type : exceptionClasses)
                map.put(type, !this.getDefault());
            this.setTypeMap(map);
        }
    }

    public static BinaryExceptionClassifier defaultClassifier() {
        return new BinaryExceptionClassifier(
                Collections.singletonMap(Exception.class, true), false);
    }

    public void setTraverseCauses(boolean traverseCauses) {
        this.traverseCauses = traverseCauses;
    }

    @Override
    public Boolean classify(Throwable classifiable) {
        // 先调用SubclassClassifier的版本
        Boolean classified = super.classify(classifiable);
        if(!this.traverseCauses)    // 未开启cause遍历，行为和SubclassClassifier的版本一致
            return classified;

        // 如果开启了cause遍历，并且结果等于default值，才会继续遍历cause链
        if(classified.equals(this.getDefault())) { // 命中规则是：只要cause链中有一层命中“非默认value”，就立刻采用这个结果
            Throwable cause = classifiable;
            do {
                if(this.getClassified().containsKey(cause.getCause()))
                    return classified;  // 找到了
                cause = cause.getCause();   // 继续找下一层cause
                classified = super.classify(cause);
            }
            while(cause != null && classified.equals(this.getDefault()));
        }
        return classified;
    }
}
