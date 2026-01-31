package com.github.liyibo1110.spring.retry.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户主注解：标记需要支持熔断功能的方法（自身也是个stateful模式的Retryable）
 * @author liyibo
 * @date 2026-01-31 12:12
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Retryable(stateful = true)
public @interface CircuitBreaker {
    @AliasFor(annotation = Retryable.class)
    @Deprecated
    Class<? extends Throwable>[] value() default {};

    @AliasFor(annotation = Retryable.class)
    @Deprecated
    Class<? extends Throwable>[] include() default {};

    @AliasFor(annotation = Retryable.class)
    Class<? extends Throwable>[] retryFor() default {};

    @Deprecated
    @AliasFor(annotation = Retryable.class)
    Class<? extends Throwable>[] exclude() default {};

    @AliasFor(annotation = Retryable.class)
    Class<? extends Throwable>[] noRetryFor() default {};

    @AliasFor(annotation = Retryable.class)
    Class<? extends Throwable>[] notRecoverable() default {};

    @AliasFor(annotation = Retryable.class)
    int maxAttempts() default 3;

    @AliasFor(annotation = Retryable.class)
    String maxAttemptsExpression() default "";

    @AliasFor(annotation = Retryable.class)
    String label() default "";

    /**
     * 开始熔断20秒之后，允许一次尝试（切换到half-open状态）
     */
    long resetTimeout() default 20000;

    String resetTimeoutExpression() default "";

    /**
     * 5秒内遇到失败，就要开启熔断
     */
    long openTimeout() default 5000;

    String openTimeoutExpression() default "";

    @AliasFor(annotation = Retryable.class)
    String exceptionExpression() default "";

    /**
     * 当触发exhausted状态时，是否封装成ExhaustedException（为true则不封装）
     */
    boolean throwLastExceptionOnExhausted() default false;

    @AliasFor(annotation = Retryable.class)
    String recover() default "";
}
