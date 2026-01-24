package com.github.liyibo1110.spring.retry.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户主注解，用于收集BackOffPolicy的元数据，特性为：
 * 1、如果没有明确设置，则默认采用1000毫秒的固定延迟。
 * 2、仅设置了delay，backoff具有该值的固定延迟
 * 3、当设置了delay和maxDelay时，backoff会在两个值之间均匀分布
 * 4、使用了delay、maxDelay和multiplier函数，backoff时间会呈指数级增长，直到达到最大值
 * 5、如果设置了random，则每个延迟的乘数将从[1, 乘数-1]中的均匀分布中选取
 * @author liyibo
 * @date 2026-01-22 00:34
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Backoff {
    /**
     * delay的同义词
     */
    @AliasFor("delay")
    long value() default 1000;

    /**
     * backoff的一个规范周期，在指数情况下用作初始值，在均匀情况下用作最小值
     */
    @AliasFor("value")
    long delay() default 1000;

    /**
     * 重试之间的最大等待时间，如果小于delay，则采用默认值30000
     */
    long maxDelay() default 0;

    /**
     * 如果大于1.0，则用作生成下一次backoff的延迟乘数。
     * 任何小于或等于1.0的值，均视为1.0，即表示固定延迟
     */
    double multiplier() default 0;

    /**
     * 计算backoff时段的表达式，在指数情况下用作初始值，在均匀情况下用作最小值。
     */
    String delayExpression() default "";

    String maxDelayExpression() default "";

    String multiplierExpression() default "";

    /**
     * 在指数情况下（multiplier > 1.0），若值为true，backoff会产生随机抖动
     */
    boolean random() default false;

    String randomExpression() default "";
}
