package com.github.liyibo1110.spring.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户主注解，标记作为recover处理器方法。
 * recover处理器方法第一个参数需要是Throwable或其子类型，
 * 以及一个与要恢复的@Retryable方法类型相同的返回值。
 * @author liyibo
 * @date 2026-01-22 00:30
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Recover {

}
