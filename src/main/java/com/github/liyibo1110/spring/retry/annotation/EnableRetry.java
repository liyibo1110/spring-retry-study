package com.github.liyibo1110.spring.retry.annotation;

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 全局retry总开关，如果在上下文任何@Configuration上声明了这个注解，
 * 则retryable注解的方法所在的bean将被生成AOP代理，同时负责注册Retry Advisor（即RetryConfiguration）
 * @author liyibo
 * @date 2026-01-31 17:27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

/**
 * 开启AOP基础设施，即允许创建AOP代理对象，
 * proxyTargetClass为false代表使用JDK动态代理（基于接口实现）而不是CGLIB（子类代理）
 */
@EnableAspectJAutoProxy(proxyTargetClass = false)
/** 将RetryConfiguration注入到bean容器 */
@Import(RetryConfiguration.class)
@Documented
public @interface EnableRetry {
    @AliasFor(annotation = EnableAspectJAutoProxy.class)
    boolean proxyTargetClass() default false;

    int order() default Ordered.LOWEST_PRECEDENCE - 1;
}
