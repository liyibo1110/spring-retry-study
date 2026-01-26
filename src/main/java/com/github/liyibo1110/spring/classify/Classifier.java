package com.github.liyibo1110.spring.classify;

import java.io.Serializable;

/**
 * 分类器（classifier）的基础接口。
 * 是将一种类型的对象，映射到另一种类型的对象。
 * 请注意只有当参数类型本身是可序列化的，实现才能使可序列化的
 * @author liyibo
 * @date 2026-01-26 00:13
 */
public interface Classifier<C, T> extends Serializable {
    /**
     * 对给定对象进行分类（classify），并返回一个不同类型的对象，也可能是枚举类型
     */
    T classify(C classifiable);
}
