package com.github.liyibo1110.spring.retry.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户主注解：标记需要重试调用的方法
 * @author liyibo
 * @date 2026-01-22 00:46
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Retryable {
    /**
     * 此类中用于recover的方法名称，该方法必须标有Recover注解。
     */
    String recover() default "";

    /**
     * retry拦截器的bean名称，用于可重试方法，与其它属性互斥
     */
    String interceptor() default "";

    /**
     * 可重试的异常类型，默认为空（如果exclude也为空，则所有异常都会被重试）
     * 推荐使用retryFor()
     */
    @Deprecated
    Class<? extends Throwable>[] value() default {};

    /**
     * 可重试的异常类型，默认为空（如果exclude也为空，则所有异常都会被重试）
     * 推荐使用retryFor()
     */
    @AliasFor("retryFor")
    @Deprecated
    Class<? extends Throwable>[] include() default {};

    /**
     * 可重试的异常类型，默认为空（如果noRetryFor也为空，则所有异常都会被重试）
     */
    Class<? extends Throwable>[] retryFor() default {};

    /**
     * 不可重试的异常类型，默认为空（如果include也为空，则所有异常都会被重试）
     * 推荐使用noRetryFor()
     */
    @Deprecated
    @AliasFor("noRetryFor")
    Class<? extends Throwable>[] exclude() default {};

    /**
     * 不可重试的异常类型，默认为空（如果noRetryFor也为空，则所有异常都会被重试）
     */
    @AliasFor("exclude")
    Class<? extends Throwable>[] noRetryFor() default {};

    /**
     * 不可恢复的异常类型，这些异常会被抛出给调用者，而不会调用任何recover
     */
    Class<? extends Throwable>[] notRecoverable() default {};

    /**
     * 统计报告的唯一标签
     */
    String label() default "";

    /**
     * 重试是否为有状态的，即异常被重新抛出，但后续使用相同参数的调用会引用相同的重试策略。
     * 如果为false，则不会抛出可重试的异常
     */
    boolean stateful() default false;

    /**
     * 最大重试次数（包括了第1次的失败）
     */
    int maxAttempts() default 3;

    String maxAttemptsExpression() default "";

    /**
     * 默认返回无属性设置的Backoff，即使用默认值的版本
     */
    Backoff backoff() default @Backoff();

    /**
     * 指定表达式，在SimpleRetryPolicy.canRetry()返回true后评估
     * 可用于有条件地抑制重试，仅在抛出异常后调用
     * 评估的根对象是最后一个Throwable，可以引用context中的其它bean
     */
    String exceptionExpression() default "";

    /**
     * 要使用的retry监听器的bean名称，如设置为空字符串，则排除所有监听器（包括默认bean）的使用
     */
    String[] listeners() default {};
}
