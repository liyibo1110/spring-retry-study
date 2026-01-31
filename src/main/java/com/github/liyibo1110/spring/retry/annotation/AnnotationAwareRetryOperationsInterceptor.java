package com.github.liyibo1110.spring.retry.annotation;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.backoff.BackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.BackOffPolicyBuilder;
import com.github.liyibo1110.spring.retry.backoff.NoBackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.Sleeper;
import com.github.liyibo1110.spring.retry.interceptor.FixedKeyGenerator;
import com.github.liyibo1110.spring.retry.interceptor.MethodArgumentsKeyGenerator;
import com.github.liyibo1110.spring.retry.interceptor.MethodInvocationRecoverer;
import com.github.liyibo1110.spring.retry.interceptor.NewMethodArgumentsIdentifier;
import com.github.liyibo1110.spring.retry.interceptor.RetryInterceptorBuilder;
import com.github.liyibo1110.spring.retry.policy.CircuitBreakerRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.ExpressionRetryPolicy;
import com.github.liyibo1110.spring.retry.policy.MapRetryContextCache;
import com.github.liyibo1110.spring.retry.policy.RetryContextCache;
import com.github.liyibo1110.spring.retry.policy.SimpleRetryPolicy;
import com.github.liyibo1110.spring.retry.support.Args;
import com.github.liyibo1110.spring.retry.support.RetrySynchronizationManager;
import com.github.liyibo1110.spring.retry.support.RetryTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
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
import java.util.HashMap;
import java.util.Map;
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
                    interceptor = this.getStatefulInterceptor(target, method, retryable);
                else    // 否则只能用stateless模式的拦截器了
                    interceptor = this.getStatelessInterceptor(target, method, retryable);
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
        template.setRetryPolicy(this.getRetryPolicy(retryable, true));
        template.setBackOffPolicy(getBackoffPolicy(retryable.backoff(), true));
        return RetryInterceptorBuilder.stateless()
                .retryOperations(template)
                .label(retryable.label())
                .recoverer(this.getRecoverer(target, method))
                .build();
    }

    /**
     * 尝试匹配stateful的拦截器
     */
    private MethodInterceptor getStatefulInterceptor(Object target, Method method, Retryable retryable) {
        RetryTemplate template = createTemplate(retryable.listeners());
        template.setRetryContextCache(this.retryContextCache);

        CircuitBreaker circuit = AnnotatedElementUtils.findMergedAnnotation(method, CircuitBreaker.class);
        if(circuit == null)
            circuit = this.findAnnotationOnTarget(target, method, CircuitBreaker.class);
        if(circuit != null) {
            // 生成带熔断的stateful拦截器
            RetryPolicy policy = this.getRetryPolicy(circuit, false);
            CircuitBreakerRetryPolicy breaker = new CircuitBreakerRetryPolicy(policy);
            this.openTimeout(breaker, circuit);
            this.resetTimeout(breaker, circuit);
            template.setRetryPolicy(breaker);
            template.setBackOffPolicy(new NoBackOffPolicy());
            template.setThrowLastExceptionOnExhausted(circuit.throwLastExceptionOnExhausted());
            String label = circuit.label();
            if(!StringUtils.hasText(label))
                label = method.toGenericString();
            return RetryInterceptorBuilder.circuitBreaker()
                    .keyGenerator(new FixedKeyGenerator("circuit"))
                    .retryOperations(template)
                    .recoverer(getRecoverer(target, method))
                    .label(label)
                    .build();
        }
        // 生成不带熔断的stateful拦截器
        RetryPolicy policy = this.getRetryPolicy(retryable, false);
        template.setRetryPolicy(policy);
        template.setBackOffPolicy(this.getBackoffPolicy(retryable.backoff(), false));
        String label = retryable.label();
        return RetryInterceptorBuilder.stateful()
                .keyGenerator(this.methodArgumentsKeyGenerator)
                .newMethodArgumentsIdentifier(this.newMethodArgumentsIdentifier)
                .retryOperations(template)
                .label(label)
                .recoverer(this.getRecoverer(target, method))
                .build();
    }

    /**
     * 为CircuitBreakerRetryPolicy实例设置openTimeout参数
     * 1、从CircuitBreaker相应配置值获取
     * 2、从CircuitBreaker相应表达式计算得出
     */
    private void openTimeout(CircuitBreakerRetryPolicy breaker, CircuitBreaker circuit) {
        String expression = circuit.openTimeoutExpression();
        if(StringUtils.hasText(expression)) {
            Expression parsed = this.parse(expression);
            if(isTemplate(expression)) {    // 是template类型就直接用getValue获取值
                Long value = parsed.getValue(this.evaluationContext, Long.class);
                if(value != null) {
                    breaker.setOpenTimeout(value);
                    return;
                }
            }else { // 不是template要嗲用evaluate计算实际的值
                breaker.openTimeoutSupplier(() -> evaluate(parsed, Long.class, false));
                return;
            }
        }
        // 没有表达式，就从值获取
        breaker.setOpenTimeout(circuit.openTimeout());
    }

    /**
     * 为CircuitBreakerRetryPolicy实例设置resetTimeout参数
     * 1、从CircuitBreaker相应配置值获取
     * 2、从CircuitBreaker相应表达式计算得出
     */
    private void resetTimeout(CircuitBreakerRetryPolicy breaker, CircuitBreaker circuit) {
        String expression = circuit.resetTimeoutExpression();
        if(StringUtils.hasText(expression)) {
            Expression parsed = this.parse(expression);
            if(isTemplate(expression)) {    // 是template类型就直接用getValue获取值
                Long value = parsed.getValue(this.evaluationContext, Long.class);
                if(value != null) {
                    breaker.setResetTimeout(value);
                    return;
                }
            }else { // 不是template要嗲用evaluate计算实际的值
                breaker.resetTimeoutSupplier(() -> evaluate(parsed, Long.class, false));
                return;
            }
        }
        // 没有表达式，就从值获取
        breaker.setResetTimeout(circuit.resetTimeout());
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

    /**
     * 根据注解，生成最终的RetryPolicy实现实例
     */
    private RetryPolicy getRetryPolicy(Annotation retryable, boolean stateless) {
        // 注意retryable可能是Retryable或者CircuitBreaker
        Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(retryable);
        Class<? extends Throwable>[] includes = (Class<? extends Throwable>[])attrs.get("value");
        String exceptionExpression = (String) attrs.get("exceptionExpression");
        boolean hasExceptionExpression = StringUtils.hasText(exceptionExpression);
        if(includes.length == 0) {
            Class<? extends Throwable>[] value = (Class<? extends Throwable>[])attrs.get("retryFor");
            includes = value;
        }
        Class<? extends Throwable>[] excludes = (Class<? extends Throwable>[])attrs.get("noRetryFor");
        Integer maxAttempts = (Integer)attrs.get("maxAttempts");
        String maxAttemptsExpression = (String)attrs.get("maxAttemptsExpression");
        Expression parsedExpression = null;
        if(StringUtils.hasText(maxAttemptsExpression)) {
            parsedExpression = this.parse(maxAttemptsExpression);
            if(this.isTemplate(maxAttemptsExpression)) {
                maxAttempts = parsedExpression.getValue(this.evaluationContext, Integer.class);
                parsedExpression = null;
            }
        }
        final Expression maxAttExpression = parsedExpression;
        SimpleRetryPolicy simple = null;
        if(includes.length == 0 && excludes.length == 0) {
            simple = hasExceptionExpression
                    ? new ExpressionRetryPolicy(this.resolve(exceptionExpression)).withBeanFactory(this.beanFactory)
                    : new SimpleRetryPolicy();
            if(maxAttExpression != null)
                simple.maxAttemptsSupplier(() -> this.evaluate(maxAttExpression, Integer.class, stateless));
            else
                simple.setMaxAttempts(maxAttempts);
        }
        Map<Class<? extends Throwable>, Boolean> policyMap = new HashMap<>();
        for(Class<? extends Throwable> type : includes)
            policyMap.put(type, true);
        for(Class<? extends Throwable> type : excludes)
            policyMap.put(type, false);
        boolean retryNotExcluded = includes.length == 0;
        if(simple == null) {
            if(hasExceptionExpression) {
                simple = new ExpressionRetryPolicy(maxAttempts, policyMap, true, this.resolve(exceptionExpression),
                        retryNotExcluded).withBeanFactory(this.beanFactory);
            }else {
                simple = new SimpleRetryPolicy(maxAttempts, policyMap, true, retryNotExcluded);
            }
            if(maxAttExpression != null)
                simple.maxAttemptsSupplier(() -> this.evaluate(maxAttExpression, Integer.class, stateless));
        }
        Class<? extends Throwable>[] noRecovery = (Class<? extends Throwable>[]) attrs.get("notRecoverable");
        if(noRecovery != null && noRecovery.length > 0)
            simple.setNotRecoverable(noRecovery);
        return simple;
    }

    /**
     * 根据注解，生成最终的BackOff实现实例
     */
    private BackOffPolicy getBackoffPolicy(Backoff backoff, boolean stateless) {
        Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(backoff);
        long min = backoff.delay() == 0 ? backoff.value() : backoff.delay();
        String delayExpression = (String)attrs.get("delayExpression");
        Expression parsedMinExp = null;
        if(StringUtils.hasText(delayExpression)) {
            parsedMinExp = this.parse(delayExpression);
            if(isTemplate(delayExpression)) {
                min = parsedMinExp.getValue(this.evaluationContext, Long.class);
                parsedMinExp = null;
            }
        }
        long max = backoff.maxDelay();
        String maxDelayExpression = (String)attrs.get("maxDelayExpression");
        Expression parsedMaxExp = null;
        if(StringUtils.hasText(maxDelayExpression)) {
            parsedMaxExp = this.parse(maxDelayExpression);
            if(isTemplate(maxDelayExpression)) {
                max = parsedMaxExp.getValue(this.evaluationContext, Long.class);
                parsedMaxExp = null;
            }
        }
        double multiplier = backoff.multiplier();
        String multiplierExpression = (String)attrs.get("multiplierExpression");
        Expression parsedMultExp = null;
        if(StringUtils.hasText(multiplierExpression)) {
            parsedMultExp = this.parse(multiplierExpression);
            if(this.isTemplate(multiplierExpression)) {
                multiplier = parsedMultExp.getValue(this.evaluationContext, Double.class);
                parsedMultExp = null;
            }
        }
        boolean isRandom = false;
        String randomExpression = (String) attrs.get("randomExpression");
        Expression parsedRandomExp = null;
        if(multiplier > 0 || parsedMultExp != null) {
            isRandom = backoff.random();
            if(StringUtils.hasText(randomExpression)) {
                parsedRandomExp = this.parse(randomExpression);
                if(this.isTemplate(randomExpression)) {
                    isRandom = parsedRandomExp.getValue(this.evaluationContext, Boolean.class);
                    parsedRandomExp = null;
                }
            }
        }
        return this.buildBackOff(min, parsedMinExp, max, parsedMaxExp, multiplier, parsedMultExp, isRandom, parsedRandomExp, stateless);
    }

    /**
     * 根据各参数值，生成最终的BackOff实现实例
     */
    private BackOffPolicy buildBackOff(long min, Expression minExp, long max, Expression maxExp, double multiplier,
                                       Expression multExp, boolean isRandom, Expression randomExp, boolean stateless) {
        BackOffPolicyBuilder builder = BackOffPolicyBuilder.newBuilder();
        if(minExp != null)
            builder.delaySupplier(() -> this.evaluate(minExp, Long.class, stateless));
        else
            builder.delay(min);

        if(maxExp != null)
            builder.maxDelaySupplier(() -> this.evaluate(maxExp, Long.class, stateless));
        else
            builder.maxDelay(max);

        if(multExp != null)
            builder.multiplierSupplier(() -> this.evaluate(multExp, Double.class, stateless));
        else
            builder.multiplier(multiplier);

        if(randomExp != null)
            builder.randomSupplier(() -> this.evaluate(randomExp, Boolean.class, stateless));
        else
            builder.random(isRandom);

        builder.sleeper(this.sleeper);
        return builder.build();
    }

    /**
     * 将表达式的字符串值，转换成Expression AST实例
     */
    private Expression parse(String expression) {
        if(this.isTemplate(expression))
            return PARSER.parseExpression(this.resolve(expression), PARSER_CONTEXT);
        else
            return PARSER.parseExpression(this.resolve(expression));
    }

    /**
     * 所谓template格式，就是类似#{...}这样的，在项目启动时立刻就能计算出来值的形式，因此不需要保存对应的Expression实例。
     * 与template格式相对的叫做literal格式，即不带#{...}这样的，即每次真需要这个值的时候，再去实时计算。
     * 因此解析后需要保存对应的Expression实例，而且因为是需要时再计算，所以一般都是用Supplier来保存一个lambda
     * PARSER_CONTEXT.getExpressionPrefix()其实就是"#{"，PARSER_CONTEXT.getExpressionSuffix()其实就是"}",
     */
    private boolean isTemplate(String expression) {
        return expression.contains(PARSER_CONTEXT.getExpressionPrefix())
                && expression.contains(PARSER_CONTEXT.getExpressionSuffix());
    }

    /***
     * 使用了Args的目的就是为了能在表达式里面带入实际方法的参数，来进行动态判断
     * 例如：@Retryable(maxAttemptsExpression = "args[0] == 'vip' ? 10 : 3")
     */
    private <T> T evaluate(Expression expression, Class<T> type, boolean stateless) {
        Args args = null;
        if(stateless) {
            RetryContext context = RetrySynchronizationManager.getContext();
            if(context != null)
                args = (Args)context.getAttribute("ARGS");
            if(args == null)
                args = Args.NO_ARGS;
        }
        return expression.getValue(this.evaluationContext, args, type);
    }

    /**
     * 解析特定的value，就是把表达式里面的${...}给替换掉
     */
    private String resolve(String value) {
        if(this.beanFactory != null && this.beanFactory instanceof ConfigurableBeanFactory)
            return ((ConfigurableBeanFactory)this.beanFactory).resolveEmbeddedValue(value);
        return value;
    }
}
