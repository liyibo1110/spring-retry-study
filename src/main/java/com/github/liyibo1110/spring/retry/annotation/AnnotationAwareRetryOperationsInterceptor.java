package com.github.liyibo1110.spring.retry.annotation;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * 用于解析正在调用method上面的retry元数据的拦截器，并委托给适当的RetryOperationsInterceptor组件进行处理
 * 即annotation -> RetryOperationsInterceptor的生产工厂
 * 1、解析@Retryable / @Recover / @CircuitBreaker
 * 2、根据注解，动态构建一个合适的MethodInterceptor实例
 * 3、把真正的调用，委托给interceptor来执行
 * 顺便说IntroductionInterceptor接口是个啥：是个入口级AOP拦截器，决定要不要，以及用什么方式拦截当前方法
 * @author liyibo
 * @date 2026-01-28 15:49
 */
public class AnnotationAwareRetryOperationsInterceptor implements IntroductionInterceptor, BeanFactoryAware {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return null;
    }

    @Override
    public boolean implementsInterface(Class<?> intf) {
        return false;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

    }
}
