package com.github.liyibo1110.spring.retry.annotation;

import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.backoff.Sleeper;
import com.github.liyibo1110.spring.retry.interceptor.MethodArgumentsKeyGenerator;
import com.github.liyibo1110.spring.retry.interceptor.MethodInvocationRecoverer;
import com.github.liyibo1110.spring.retry.interceptor.NewMethodArgumentsIdentifier;
import com.github.liyibo1110.spring.retry.policy.MapRetryContextCache;
import com.github.liyibo1110.spring.retry.policy.RetryContextCache;
import com.github.liyibo1110.spring.retry.support.RetryTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.naming.OperationNotSupportedException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final TemplateParserContext PARSER_CONTEXT = new TemplateParserContext();
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final MethodInterceptor NULL_INTERCEPTOR = methodInvocation -> {
        throw new OperationNotSupportedException("Not supported");
    };
    private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

    /** Object -> Method -> MethodInterceptor的双层映射缓存 */
    private final ConcurrentReferenceHashMap<Object, ConcurrentMap<Method, MethodInterceptor>> delegates = new ConcurrentReferenceHashMap<>();

    private RetryContextCache retryContextCache = new MapRetryContextCache();

    private MethodArgumentsKeyGenerator methodArgumentsKeyGenerator;

    private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;

    private Sleeper sleeper;

    private BeanFactory beanFactory;

    private RetryListener[] globalListeners;

    public void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    public void setRetryContextCache(RetryContextCache retryContextCache) {
        this.retryContextCache = retryContextCache;
    }

    public void setKeyGenerator(MethodArgumentsKeyGenerator methodArgumentsKeyGenerator) {
        this.methodArgumentsKeyGenerator = methodArgumentsKeyGenerator;
    }

    public void setNewItemIdentifier(NewMethodArgumentsIdentifier newMethodArgumentsIdentifier) {
        this.newMethodArgumentsIdentifier = newMethodArgumentsIdentifier;
    }

    public void setListeners(Collection<RetryListener> globalListeners) {
        ArrayList<RetryListener> retryListeners = new ArrayList<>(globalListeners);
        AnnotationAwareOrderComparator.sort(retryListeners);
        this.globalListeners = retryListeners.toArray(new RetryListener[0]);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
    }

    /**
     * 检测传入的类，是否实现了Retryable接口
     */
    @Override
    public boolean implementsInterface(Class<?> intf) {
        return com.github.liyibo1110.spring.retry.interceptor.Retryable.class.isAssignableFrom(intf);
    }

    /**
     * 根据注解，寻找合适的拦截器来干活
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        MethodInterceptor delegate = this.getDelegate(invocation.getThis(), invocation.getMethod());
        if(delegate == null)    // 没找到合适的拦截器，直接调用原始方法即可
            return invocation.proceed();
        else
            return delegate.invoke(invocation);
    }

    private MethodInterceptor getDelegate(Object target, Method method) {
        ConcurrentMap<Method, MethodInterceptor> cachedMethods = this.delegates.get(target);
        if(cachedMethods == null)
            cachedMethods = new ConcurrentHashMap<>();
        MethodInterceptor delegate = cachedMethods.get(method);
        if(delegate == null) {  // cache里找不到，只能尝试匹配
            MethodInterceptor interceptor = NULL_INTERCEPTOR; // 默认结果是没找到
            // 在method上面直接找Retryable注解
            Retryable retryable = AnnotatedElementUtils.findMergedAnnotation(method, Retryable.class);
            if(retryable == null)   // 在method所属的类上面继续找Retryable注解（不能同时有Recover注解，因为recover不能也retry）
                retryable = this.classLevelAnnotation(method, Retryable.class);
            if(retryable == null)
                retryable = this.findAnnotationOnTarget(target, method, Retryable.class);
            if(retryable != null) { // 找到注解了
                if(StringUtils.hasText(retryable.interceptor()))    // 如果标明了用哪个拦截器就直接用
                    interceptor = this.beanFactory.getBean(retryable.interceptor(), MethodInterceptor.class);
                else if(retryable.stateful())   // 如果标明了要用stateful模式的拦截器
                    interceptor = getStatefulInterceptor(target, method, retryable);
                else    // 否则只能用stateless模式的拦截器了
                    interceptor = getStatelessInterceptor(target, method, retryable);
            }
            cachedMethods.putIfAbsent(method, interceptor); // 尝试放入cache，下次不用再匹配了
            delegate = cachedMethods.get(method);
        }
        // cache里有，或者匹配到了
        this.delegates.putIfAbsent(target, cachedMethods);
        return delegate == NULL_INTERCEPTOR ? null : delegate;
    }

    /**
     * 在target自身的同名Method里面寻找特定注解（也会在target类级别上寻找）
     */
    private <A extends Annotation> A findAnnotationOnTarget(Object target, Method method, Class<A> annotation) {
        try {
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            A retryable = AnnotatedElementUtils.findMergedAnnotation(targetMethod, annotation);
            if(retryable == null)
                retryable = classLevelAnnotation(targetMethod, annotation);
            return retryable;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 在method对应的类上寻找特定注解，但是不能同时有Recover注解
     */
    private <A extends Annotation> A classLevelAnnotation(Method method, Class<A> annotation) {
        A anno = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), annotation);
        if(anno != null && AnnotatedElementUtils.findMergedAnnotation(method, Recover.class) != null)
            anno = null;
        return anno;
    }

    /**
     * 尝试匹配stateless的拦截器
     */
    private MethodInterceptor getStatelessInterceptor(Object target, Method method, Retryable retryable) {
        RetryTemplate template = createTemplate(retryable.listeners());
        return null;
    }

    /**
     * 尝试匹配stateful的拦截器
     */
    private MethodInterceptor getStatefulInterceptor(Object target, Method method, Retryable retryable) {
        RetryTemplate template = createTemplate(retryable.listeners());
        return null;
    }

    private RetryTemplate createTemplate(String[] listenersBeanNames) {
        RetryTemplate template = new RetryTemplate();
        if(listenersBeanNames.length > 0)
            template.setListeners(this.getListenersBeans(listenersBeanNames));
        else if(this.globalListeners != null)
            template.setListeners(this.globalListeners);
        return template;
    }

    private RetryListener[] getListenersBeans(String[] listenersBeanNames) {
        if(listenersBeanNames.length == 1 && "".equals(listenersBeanNames[0].trim()))
            return new RetryListener[0];
        RetryListener[] listeners = new RetryListener[listenersBeanNames.length];
        for(int i = 0; i < listeners.length; i++)
            listeners[i] = this.beanFactory.getBean(listenersBeanNames[i], RetryListener.class);
        return listeners;
    }

    private MethodInvocationRecoverer<?> getRecoverer(Object target, Method method) {
        if(target instanceof MethodInvocationRecoverer)
            return (MethodInvocationRecoverer<?>)target;
        final AtomicBoolean foundRecoverable = new AtomicBoolean(false);
        ReflectionUtils.doWithMethods(target.getClass(), candidate -> {
            if(AnnotatedElementUtils.findMergedAnnotation(candidate, Recover.class) != null)
                foundRecoverable.set(true);
        });
        if(!foundRecoverable.get())
            return null;
        // 找到了Recover实现，要构造并返回MethodInvocationRecoverer的实现类
        return new RecoverAnnotationRecoveryHandler<>(target, method);
    }

    private RetryPolicy getRetryPolicy(Annotation retryable, boolean stateless) {
        return null;
    }
}
