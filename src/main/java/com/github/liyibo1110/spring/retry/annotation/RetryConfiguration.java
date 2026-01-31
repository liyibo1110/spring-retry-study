package com.github.liyibo1110.spring.retry.annotation;

import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.backoff.Sleeper;
import com.github.liyibo1110.spring.retry.interceptor.MethodArgumentsKeyGenerator;
import com.github.liyibo1110.spring.retry.interceptor.NewMethodArgumentsIdentifier;
import com.github.liyibo1110.spring.retry.policy.RetryContextCache;
import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 为@Retryable提供AOP Advisor的工厂
 * 1、AbstractPointcutAdvisor抽象类：代表实现类是一个AOP Advisor。
 * 2、IntroductionAdvisor接口：给Bean引入新的接口。
 * 3、BeanFactoryAware接口：能从bean容器找bean。
 * 4、InitializingBean接口：自身作为被bean生成可以进行一些初始化任务。
 * 5、SmartInitializingSingleton接口：所有单例实例都创建好了，可以做收尾任务。
 * 3、ImportAware接口：用来读取EnableRetry注解里面的字段（因为它里面Import了RetryConfiguration）。
 * @author liyibo
 * @date 2026-01-31 17:32
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Component
public class RetryConfiguration extends AbstractPointcutAdvisor
        implements IntroductionAdvisor, BeanFactoryAware, InitializingBean, SmartInitializingSingleton, ImportAware {
    protected AnnotationAttributes enableRetry;
    private AnnotationAwareRetryOperationsInterceptor advice;
    private Pointcut pointcut;
    private RetryContextCache retryContextCache;
    private List<RetryListener> retryListeners;
    private MethodArgumentsKeyGenerator methodArgumentsKeyGenerator;
    private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;
    private Sleeper sleeper;
    private BeanFactory beanFactory;

    /**
     * 获取到@EnableRetry注解里面的字段（proxyTargetClass和order）,
     * 重点要关注设置Advisor的顺序（尤其是和@Transactional注解）
     */
    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableRetry = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableRetry.class.getName()));
    }

    /**
     * 填充各个组件，重点是实现Pointcut
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.retryContextCache = this.findBean(RetryContextCache.class);
        this.methodArgumentsKeyGenerator = this.findBean(MethodArgumentsKeyGenerator.class);
        this.newMethodArgumentsIdentifier = this.findBean(NewMethodArgumentsIdentifier.class);
        this.sleeper = this.findBean(Sleeper.class);
        Set<Class<? extends Annotation>> retryableAnnotationTypes = new LinkedHashSet<>(1);
        retryableAnnotationTypes.add(Retryable.class);
        this.pointcut = this.buildPointcut(retryableAnnotationTypes);
        this.advice = this.buildAdvice();
        this.advice.setBeanFactory(this.beanFactory);
        if(this.enableRetry != null)
            setOrder(this.enableRetry.getNumber("order"));
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.retryListeners = this.findBeans(RetryListener.class);
        if(this.retryListeners != null)
            this.advice.setListeners(this.retryListeners);
    }

    /**
     * 根据class实例，找一组对应的bean
     */
    private <T> List<T> findBeans(Class<? extends T> type) {
        if(this.beanFactory instanceof ListableBeanFactory) {
            ListableBeanFactory listable = (ListableBeanFactory)this.beanFactory;
            if(listable.getBeanNamesForType(type).length > 0) {
                ArrayList<T> list = new ArrayList<>(listable.getBeansOfType(type, false, false).values());
                OrderComparator.sort(list);
                return list;
            }
        }
        return null;
    }

    /**
     * 根据class实例，找一个对应的bean
     */
    private <T> T findBean(Class<? extends T> type) {
        if(this.beanFactory instanceof ListableBeanFactory) {
            ListableBeanFactory listable = (ListableBeanFactory)this.beanFactory;
            if(listable.getBeanNamesForType(type, false, false).length == 1)
                return listable.getBean(type);
        }
        return null;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public ClassFilter getClassFilter() {
        return this.pointcut.getClassFilter();
    }

    /**
     * 被代理的bean，要额外实现的接口（就是Retryable那个空的标记接口），用途是：
     * 1、让Spring内部能识别出这是一个被Retry管理的代理。
     * 2、AOP排序。
     * 3、特殊识别。
     * 4、调试和扩展
     */
    @Override
    public Class<?>[] getInterfaces() {
        return new Class[] { com.github.liyibo1110.spring.retry.interceptor.Retryable.class };
    }

    @Override
    public void validateInterfaces() throws IllegalArgumentException {
        // nothing to do
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    protected AnnotationAwareRetryOperationsInterceptor buildAdvice() {
        AnnotationAwareRetryOperationsInterceptor interceptor = new AnnotationAwareRetryOperationsInterceptor();
        if(this.retryContextCache != null)
            interceptor.setRetryContextCache(this.retryContextCache);
        if(this.methodArgumentsKeyGenerator != null)
            interceptor.setKeyGenerator(this.methodArgumentsKeyGenerator);
        if(this.newMethodArgumentsIdentifier != null)
            interceptor.setNewItemIdentifier(this.newMethodArgumentsIdentifier);
        if(this.sleeper != null)
            interceptor.setSleeper(this.sleeper);
        return interceptor;
    }

    /**
     * Pointcut在AOP的语义就是要拦截哪些方法的抽象
     */
    protected Pointcut buildPointcut(Set<Class<? extends Annotation>> retryAnnotationTypes) {
        ComposablePointcut result = null;
        for(Class<? extends Annotation> retryAnnotationType : retryAnnotationTypes) {
            Pointcut filter = new AnnotationClassOrMethodPointcut(retryAnnotationType);
            if(result == null)
                result = new ComposablePointcut(filter);
            else
                result.union(filter);
        }
        return result;
    }

    /**
     * 从类上以及方法上，寻找特定的Annotation的Pointcut实现
     */
    private final class AnnotationClassOrMethodPointcut extends StaticMethodMatcherPointcut {
        private final MethodMatcher methodResolver;

        AnnotationClassOrMethodPointcut(Class<? extends Annotation> annotationType) {
            this.methodResolver = new AnnotationMethodMatcher(annotationType);
            this.setClassFilter(new AnnotationClassOrMethodFilter(annotationType));
        }

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return this.getClassFilter().matches(targetClass) || this.methodResolver.matches(method, targetClass);
        }

        @Override
        public boolean equals(Object other) {
            if(this == other)
                return true;
            if(!(other instanceof AnnotationClassOrMethodPointcut))
                return false;
            AnnotationClassOrMethodPointcut otherAdvisor = (AnnotationClassOrMethodPointcut)other;
            return ObjectUtils.nullSafeEquals(this.methodResolver, otherAdvisor.methodResolver)
        }
    }

    /**
     * 从类上以及方法上，寻找是否具有特定的Annotation（找类上注解直接复用父类的功能，找方法级别是自己扩展实现的）
     */
    private final class AnnotationClassOrMethodFilter extends AnnotationClassFilter {
        private final AnnotationMethodsResolver methodResolver;

        AnnotationClassOrMethodFilter(Class<? extends Annotation> annotationType) {
            super(annotationType, true);
            this.methodResolver = new AnnotationMethodsResolver(annotationType);
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return super.matches(clazz) || this.methodResolver.hasAnnotatedMethods(clazz);
        }
    }

    /**
     * 寻找有特定注解的方法
     */
    private static class AnnotationMethodsResolver {
        private final Class<? extends Annotation> annotationType;

        public AnnotationMethodsResolver(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        /**
         * 在给定的类上，寻找是否存在特定的注解
         */
        public boolean hasAnnotatedMethods(Class<?> clazz) {
            final AtomicBoolean found = new AtomicBoolean(false);
            ReflectionUtils.doWithMethods(clazz, method -> {
                if(found.get())
                    return;
                Annotation anno = AnnotationUtils.findAnnotation(method, AnnotationMethodsResolver.this.annotationType);
                if(anno != null)
                    found.set(true);
            });
            return found.get();
        }
    }
}
