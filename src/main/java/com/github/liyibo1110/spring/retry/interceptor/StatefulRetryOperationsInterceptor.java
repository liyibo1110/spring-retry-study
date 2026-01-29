package com.github.liyibo1110.spring.retry.interceptor;

import com.github.liyibo1110.spring.classify.Classifier;
import com.github.liyibo1110.spring.retry.RecoveryCallback;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryOperations;
import com.github.liyibo1110.spring.retry.policy.NeverRetryPolicy;
import com.github.liyibo1110.spring.retry.support.DefaultRetryState;
import com.github.liyibo1110.spring.retry.support.RetryTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * @author liyibo
 * @date 2026-01-29 00:33
 */
public class StatefulRetryOperationsInterceptor implements MethodInterceptor {
    private transient final Log logger = LogFactory.getLog(getClass());
    /** 根据方法参数值，生成key的组件 */
    private MethodArgumentsKeyGenerator keyGenerator;
    private MethodInvocationRecoverer<?> recoverer;
    private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;
    private RetryOperations retryOperations;
    private String label;
    private Classifier<? super Throwable, Boolean> rollbackClassifier;
    /** 是否直接使用key（即不额外加label） */
    private boolean useRawKey;

    public StatefulRetryOperationsInterceptor() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new NeverRetryPolicy());
        this.retryOperations = retryTemplate;
    }

    public void setRetryOperations(RetryOperations retryTemplate) {
        Assert.notNull(retryTemplate, "'retryOperations' cannot be null.");
        this.retryOperations = retryTemplate;
    }

    public void setRecoverer(MethodInvocationRecoverer<?> recoverer) {
        this.recoverer = recoverer;
    }

    public void setRollbackClassifier(Classifier<? super Throwable, Boolean> rollbackClassifier) {
        this.rollbackClassifier = rollbackClassifier;
    }

    public void setKeyGenerator(MethodArgumentsKeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setNewItemIdentifier(NewMethodArgumentsIdentifier newMethodArgumentsIdentifier) {
        this.newMethodArgumentsIdentifier = newMethodArgumentsIdentifier;
    }

    public void setUseRawKey(boolean useRawKey) {
        this.useRawKey = useRawKey;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if(this.logger.isDebugEnabled())
            this.logger.debug("Executing proxied method in stateful retry: " + invocation.getStaticPart() + "("
                    + ObjectUtils.getIdentityHexString(invocation) + ")");
        // 默认把业务方法的参数值列表当作key（但目前还不足以表达唯一性，因为其它方法参数值也可以完全一样）
        Object[] args = invocation.getArguments();
        Object defaultKey = Arrays.asList(args);
        if(args.length == 1)    // 特殊情况，如果方法参数就1个，直接使用值作为key（表达唯一性依然不足）
            defaultKey = args[0];
        // 生成最终版本的key
        Object key = createKey(invocation, defaultKey);

        // 生成RetryState实例
        DefaultRetryState retryState = new DefaultRetryState(key,
                this.newMethodArgumentsIdentifier != null && this.newMethodArgumentsIdentifier.isNew(args),
                this.rollbackClassifier);

        // 开始干活
        Object result = this.retryOperations.execute(new StatefulMethodInvocationRetryCallback(invocation, this.label),
                this.recoverer != null ? new ItemRecovererCallback(args, this.recoverer) : null, retryState);

        if(this.logger.isDebugEnabled())
            this.logger.debug("Exiting proxied method in stateful retry with result: (" + result + ")");

        return result;
    }

    /**
     * 生成完整的stateful key（就是加上label属性）
     */
    private Object createKey(final MethodInvocation invocation, Object defaultKey) {
        Object generatedKey = defaultKey;
        // 如果有keyGenerator组件，直接使用它来生成key
        if(this.keyGenerator != null)
            generatedKey = this.keyGenerator.getKey(invocation.getArguments());
        if(generatedKey == null)
            return null;
        if(this.useRawKey)
            return generatedKey;
        String name = StringUtils.hasText(label) ? label : invocation.getMethod().toGenericString();
        return Arrays.asList(name, generatedKey);   // 追加name元素（注意这里返回的是个数组）
    }

    /**
     * 比无状态版本的callback实现要简单很多，是因为把复杂性分散到了interceptor/retry state/retry template里面了
     * 无状态：callback本身复杂（要负责AOP所有细节，因为是在execute内部要重试N次，要重走AOP链）
     * 有状态：retry执行模型复杂（但是execute内部只会被运行1次，execute本身可能被调用多次）
     */
    private static final class StatefulMethodInvocationRetryCallback extends MethodInvocationRetryCallback<Object, Throwable> {
        private StatefulMethodInvocationRetryCallback(MethodInvocation invocation, String label) {
            super(invocation, label);
        }
        @Override
        public Object doWithRetry(RetryContext context) throws Exception {
            context.setAttribute(RetryContext.NAME, label);
            try {
                return this.invocation.proceed();
            } catch (Exception | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final class ItemRecovererCallback implements RecoveryCallback<Object> {
        private final Object[] args;
        private final MethodInvocationRecoverer<?> recoverer;

        private ItemRecovererCallback(Object[] args, MethodInvocationRecoverer<?> recoverer) {
            this.args = Arrays.asList(args).toArray();  // copy???
            this.recoverer = recoverer;
        }

        @Override
        public Object recover(RetryContext context) throws Exception {
            return this.recoverer.recover(this.args, context.getLastThrowable());
        }
    }
}
