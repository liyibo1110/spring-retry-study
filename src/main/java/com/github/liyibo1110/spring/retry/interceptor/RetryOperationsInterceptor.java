package com.github.liyibo1110.spring.retry.interceptor;

import com.github.liyibo1110.spring.retry.RecoveryCallback;
import com.github.liyibo1110.spring.retry.RetryCallback;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryOperations;
import com.github.liyibo1110.spring.retry.support.Args;
import com.github.liyibo1110.spring.retry.support.RetrySynchronizationManager;
import com.github.liyibo1110.spring.retry.support.RetryTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.Assert;

/**
 * 一个MethodInterceptor的实现类，用于在业务方法调用失败时自动retry。
 * 注入的RetryOperations用来控制retry次数，默认情况下会根据RetryTemplate的默认设置进行固定次数的的重试。
 * 一个内部MethodInvocationRetryCallback实现类将一个method属性暴露到所提供的RetryContext内，
 * 该属性的值来自MethodInvocation.getMethod()。
 * 此外，此方法的参数还作为Args实例暴露到methodArgs属性中
 *
 * 通俗地说，这个类作用是：把普通方法调用包装成RetryTemplate.execute()调用。
 * @author liyibo
 * @date 2026-01-28 14:25
 */
public class RetryOperationsInterceptor implements MethodInterceptor {
    /** 对应RetryContext里面的attribute name，即对应MethodInvocation.getMethod()，就是Method反射实例 */
    public static final String METHOD = "method";

    /** 对应RetryContext里面的attribute name，即对应MethodInvocation.getArguments() */
    public static final String METHOD_ARGS = "methodArgs";

    private RetryOperations retryOperations = new RetryTemplate();

    private MethodInvocationRecoverer<?> recoverer;

    private String label;

    public void setLabel(String label) {
        this.label = label;
    }

    public void setRetryOperations(RetryOperations retryTemplate) {
        Assert.notNull(retryTemplate, "retryOperations cannot be null.");
        this.retryOperations = retryTemplate;
    }

    public void setRecoverer(MethodInvocationRecoverer<?> recoverer) {
        this.recoverer = recoverer;
    }

    /**
     * MethodInvocation.proceed() -> RetryCallback.doWithRetry() -> RetryTemplate.execute()
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        /* 1、把MethodInvocation包装成RetryCallback */
        RetryCallback<Object, Throwable> retryCallback = new MethodInvocationRetryCallback<>(invocation, this.label) {
            @Override
            public Object doWithRetry(RetryContext context) throws Exception {
                context.setAttribute(RetryContext.NAME, this.label);
                Args args = new Args(invocation.getArguments());
                context.setAttribute(METHOD, invocation.getMethod());
                context.setAttribute(METHOD_ARGS, args);

                if(this.invocation instanceof ProxyMethodInvocation) {
                    context.setAttribute("___proxy___", ((ProxyMethodInvocation)this.invocation).getProxy());
                    try {
                        // 用invocableClone目的是每次调用，会返回全新的MethodInvocation，因为proceed方法不能重复调用
                        return ((ProxyMethodInvocation)this.invocation).invocableClone().proceed();
                    } catch (Exception | Error e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                }else {
                    throw new IllegalStateException(
                            "MethodInvocation of the wrong type detected - this should not happen with Spring AOP, "
                                    + "so please raise an issue if you see this exception");
                }
            }
        };

        /* 2、构造RecoveryCallback（可选） */
        RecoveryCallback<Object> recoveryCallback = this.recoverer != null
                ? new ItemRecovererCallback(invocation.getArguments(), this.recoverer) : null;

        /* 3、交给RetryTemplate执行 */
        try {
            return this.retryOperations.execute(retryCallback, recoveryCallback);
        } finally {
            RetryContext context = RetrySynchronizationManager.getContext();
            if(context != null)
                context.removeAttribute("__proxy__");
        }
    }

    private record ItemRecovererCallback(Object[] args, MethodInvocationRecoverer<?> recoverer)
            implements RecoveryCallback<Object> {
        @Override
        public Object recover(RetryContext context) {
            return this.recoverer.recover(this.args, context.getLastThrowable());
        }
    }
}
