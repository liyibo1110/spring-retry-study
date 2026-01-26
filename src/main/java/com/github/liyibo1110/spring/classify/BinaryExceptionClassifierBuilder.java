package com.github.liyibo1110.spring.classify;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * BinaryExceptionClassifier构建器
 * @author liyibo
 * @date 2026-01-26 14:03
 */
public class BinaryExceptionClassifierBuilder {
    private Boolean isWhiteList = null;
    private boolean traverseCauses = false;
    private final List<Class<? extends Throwable>> exceptionClasses = new ArrayList<>();

    /**
     * 增加要重试的异常（使用retryOn方法，就不能再使用notRetryOn方法了）
     */
    public BinaryExceptionClassifierBuilder retryOn(Class<? extends Throwable> throwable) {
        Assert.isTrue(isWhiteList == null || isWhiteList,
                "Please use only retryOn() or only notRetryOn()");
        Assert.notNull(throwable, "Exception class can not be null");
        isWhiteList = true;
        exceptionClasses.add(throwable);
        return this;
    }

    /**
     * 排除要重试的异常（使用notRetryOn方法，就不能再使用retryOn方法了）
     */
    public BinaryExceptionClassifierBuilder notRetryOn(Class<? extends Throwable> throwable) {
        Assert.isTrue(isWhiteList == null || !isWhiteList,
                "Please use only retryOn() or only notRetryOn()");
        Assert.notNull(throwable, "Exception class can not be null");
        isWhiteList = false;
        exceptionClasses.add(throwable);
        return this;
    }

    public BinaryExceptionClassifierBuilder traversingCauses() {
        traverseCauses = true;
        return this;
    }

    public BinaryExceptionClassifier build() {
        Assert.isTrue(!exceptionClasses.isEmpty(),
                "Attempt to build classifier with empty rules. To build always true, or always false "
                        + "instance, please use explicit rule for Throwable");
        BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(exceptionClasses, isWhiteList);
        classifier.setTraverseCauses(traverseCauses);
        return classifier;
    }
}
