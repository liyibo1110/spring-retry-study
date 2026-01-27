package com.github.liyibo1110.spring.retry.support;

import com.github.liyibo1110.spring.retry.ExhaustedRetryException;
import com.github.liyibo1110.spring.retry.RecoveryCallback;
import com.github.liyibo1110.spring.retry.RetryCallback;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryException;
import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.RetryOperations;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.RetryState;
import com.github.liyibo1110.spring.retry.TerminatedRetryException;
import com.github.liyibo1110.spring.retry.backoff.BackOffContext;
import com.github.liyibo1110.spring.retry.backoff.BackOffInterruptedException;
import com.github.liyibo1110.spring.retry.backoff.BackOffPolicy;
import com.github.liyibo1110.spring.retry.backoff.NoBackOffPolicy;
import com.github.liyibo1110.spring.retry.policy.MapRetryContextCache;
import com.github.liyibo1110.spring.retry.policy.RetryContextCache;
import com.github.liyibo1110.spring.retry.policy.SimpleRetryPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 模板类，用于简化具有retry语义的操作执行。
 * 可retry操作被封装在RetryCallback接口的视线中，并使用提供的execute方法之一来执行。
 * 默认情况下，如果操作抛出任何Exception或其子类，则会进行重试，可以通过setRetryPolicy方法来更改行为。
 * 默认每个操作最多重试3次，且中间没有间隔时间，也可以通过setRetryPolicy和setBackOffPolicy进行配置。
 * BackOffPolicy控制每次单独retry之间的暂停时间。
 * 这个类是线程安全的，适合在执行操作和进行配置更改时，进行并发访问，
 * 因此可以动态更改重试次数所使用的BackOffPolicy，而不会影响正在进行中的retry操作
 * @author liyibo
 * @date 2026-01-24 22:47
 */
public class RetryTemplate implements RetryOperations {
    /** retry上下文名称 */
    private static final String GLOBAL_STATE = "state.global";

    protected final Log logger = LogFactory.getLog(getClass());

    private volatile BackOffPolicy backOffPolicy = new NoBackOffPolicy();

    private volatile RetryPolicy retryPolicy = new SimpleRetryPolicy(3);

    private volatile RetryListener[] listeners = new RetryListener[0];

    /** stateful context专用 */
    private RetryContextCache retryContextCache = new MapRetryContextCache();

    private boolean throwLastExceptionOnExhausted;

    public void setThrowLastExceptionOnExhausted(boolean throwLastExceptionOnExhausted) {
        this.throwLastExceptionOnExhausted = throwLastExceptionOnExhausted;
    }

    public void setRetryContextCache(RetryContextCache retryContextCache) {
        this.retryContextCache = retryContextCache;
    }

    public void setListeners(RetryListener[] listeners) {
        Assert.notNull(listeners, "listeners must not be null");
        this.listeners = Arrays.copyOf(listeners, listeners.length);
    }

    public void registerListener(RetryListener listener) {
        registerListener(listener, this.listeners.length);
    }

    public void registerListener(RetryListener listener, int index) {
        List<RetryListener> list = new ArrayList<>(Arrays.asList(this.listeners));
        if(index >= list.size())
            list.add(listener);
        else
            list.add(index, listener);
        this.listeners = list.toArray(new RetryListener[0]);
    }

    public boolean hasListeners() {
        return this.listeners.length > 0;
    }

    public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
        this.backOffPolicy = backOffPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback) throws E {
        return doExecute(retryCallback, null, null);
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback) throws E {
        return doExecute(retryCallback, recoveryCallback, null);
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RetryState retryState) throws E, ExhaustedRetryException {
        return doExecute(retryCallback, null, retryState);
    }

    @Override
    public <T, E extends Throwable> T execute(RetryCallback<T, E> retryCallback, RecoveryCallback<T> recoveryCallback, RetryState retryState) throws E {
        return doExecute(retryCallback, recoveryCallback, retryState);
    }

    /**
     * 真正的干活方法，如果policy允许，则执行1次retry callback，否则执行recovery callback
     */
    protected <T, E extends Throwable> T doExecute(RetryCallback<T, E> retryCallback,
                                                   RecoveryCallback<T> recoveryCallback,
                                                   RetryState state) throws E, ExhaustedRetryException {
        RetryPolicy retryPolicy = this.retryPolicy;
        BackOffPolicy backOffPolicy = this.backOffPolicy;

        // 获取这一次的context
        RetryContext context = open(retryPolicy, state);
        if(this.logger.isTraceEnabled())
            this.logger.trace("RetryContext retrieved: " + context);

        // 同一个线程复用
        RetrySynchronizationManager.register(context);

        Throwable lastException = null;
        boolean exhausted = false;

        try {
            // 是否被listener检查为中止执行
            boolean running = doOpenInterceptors(retryCallback, context);
            if (!running)
                throw new TerminatedRetryException("Retry terminated abnormally by interceptor before first attempt");

            // 注意具体规则都来自policy实例
            if(!context.hasAttribute(RetryContext.MAX_ATTEMPTS))
                context.setAttribute(RetryContext.MAX_ATTEMPTS, retryPolicy.getMaxAttempts());

            // 初始化backoff context
            BackOffContext backOffContext = null;
            Object resource = context.getAttribute("backOffContext");
            if(resource instanceof BackOffContext)
                backOffContext = (BackOffContext)resource;

            if(backOffContext == null) {    // 从RetryContext返回BackOffContext
                backOffContext = backOffPolicy.start(context);
                if(backOffContext != null)
                    context.setAttribute("backOffContext", backOffContext);
            }

            // 获取retry callback label
            Object label = retryCallback.getLabel();
            String labelMessage = label != null ? "; for: '" + label + "'" : "";

            // 进入主retry循环核心部分
            while(canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {
                try {
                    if(this.logger.isDebugEnabled())
                        this.logger.debug("Retry: count=" + context.getRetryCount() + labelMessage);
                    // 先清空lastException，假定这次retry是成功的
                    lastException = null;
                    T result = retryCallback.doWithRetry(context);  // 调用真正的业务请求
                    // 这下面说明成功了，如果业务调用出现了异常就会直接跳到catch里了
                    doOnSuccessInterceptors(retryCallback, context, result);
                    return result;
                } catch (Throwable e) {
                    // 进入这里说明业务调用失败了
                    lastException = e;

                    // 记录最新的throwable
                    try {
                        registerThrowable(retryPolicy, state, context, e);
                    } catch (Exception ex) {
                        throw new TerminatedRetryException("Could not register throwable", ex);
                    } finally {
                        doOnErrorInterceptors(retryCallback, context, e);
                    }

                    // 执行backoff逻辑
                    if(canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {
                        try {
                            backOffPolicy.backOff(backOffContext);
                        } catch (BackOffInterruptedException ex) {
                            if(this.logger.isDebugEnabled())
                                this.logger.debug("Abort retry because interrupted: count=" + context.getRetryCount()
                                        + labelMessage);
                            throw ex;
                        }
                    }

                    if(this.logger.isDebugEnabled())
                        this.logger.debug("Checking for rethrow: count=" + context.getRetryCount() + labelMessage);

                    if(shouldRethrow(retryPolicy, context, state)) {
                        if(this.logger.isDebugEnabled())
                            this.logger.debug("Rethrow in retry for policy: count=" + context.getRetryCount() + labelMessage);
                        throw RetryTemplate.<E>wrapIfNecessary(e);
                    }
                }

                // 如果是全局状态模式，则直接跳出retry循环，不再retry
                if(state != null && context.hasAttribute(GLOBAL_STATE))
                    break;
            }

            // 到这里说明stateless模式的重试次数已耗尽，准备执行recovery
            if(state == null && this.logger.isDebugEnabled())
                this.logger.debug("Retry failed last attempt: count=" + context.getRetryCount() + labelMessage);
            exhausted = true;
            return handleRetryExhausted(recoveryCallback, context, state);
        } catch (Throwable e) {
            throw RetryTemplate.<E>wrapIfNecessary(e);
        } finally {
            close(retryPolicy, context, state, lastException == null || exhausted);
            doCloseInterceptors(retryCallback, context, lastException);
            RetrySynchronizationManager.clear();
        }
    }

    protected boolean canRetry(RetryPolicy retryPolicy, RetryContext context) {
        return retryPolicy.canRetry(context);
    }

    protected void close(RetryPolicy retryPolicy, RetryContext context, RetryState state, boolean succeeded) {
        if(state != null) {
            if(succeeded) {
                if(!context.hasAttribute(GLOBAL_STATE)) // 全局的不能从cache中移除
                    this.retryContextCache.remove(state.getKey());
                retryPolicy.close(context);
                context.setAttribute(RetryContext.CLOSED, true);
            }
        }else {
            retryPolicy.close(context);
            context.setAttribute(RetryContext.CLOSED, true);
        }
    }

    protected void registerThrowable(RetryPolicy retryPolicy, RetryState state,
                                     RetryContext context, Throwable e) {
        retryPolicy.registerThrowable(context, e);
        registerContext(context, state);
    }

    /**
     * 将stateful的context加入cache（注意stateless的context和cache没有任何关系）
     */
    private void registerContext(RetryContext context, RetryState state) {
        if(state != null) {
            Object key = state.getKey();
            if(key != null) {
                // 工程防御
                if(context.getRetryCount() > 1 && !this.retryContextCache.containsKey(key)) {
                    throw new RetryException("Inconsistent state for failed item key: cache key has changed. "
                            + "Consider whether equals() or hashCode() for the key might be inconsistent, "
                            + "or if you need to supply a better key");
                }
                this.retryContextCache.put(key, context);
            }
        }
    }

    protected RetryContext open(RetryPolicy retryPolicy, RetryState state) {
        if(state == null)   // 判断是否是无状态retry模式，即每次retry都对应一个全新的RetryContext
            return doOpenInternal(retryPolicy);

        // 以下为有状态（还分成普通和全局）
        Object key = state.getKey();
        if(state.isForceRefresh())  // 不从context cache里面找，直接生成新的RetryContext
            return doOpenInternal(retryPolicy, state);

        if(!this.retryContextCache.containsKey(key))    // context cache里没找到，也要直接生成新的RetryContext
            return doOpenInternal(retryPolicy, state);

        RetryContext context = this.retryContextCache.get(key);
        if(context == null) {   // 找到了context，但是为null，直接报错
            if(this.retryContextCache.containsKey(key)) {
                throw new RetryException("Inconsistent state for failed item: no history found. "
                        + "Consider whether equals() or hashCode() for the item might be inconsistent, "
                        + "or if you need to supply a better ItemKeyGenerator");
            }
            return doOpenInternal(retryPolicy, state);
        }

        // 到这里用的是context cache中的RetryContext
        context.removeAttribute(RetryContext.CLOSED);
        context.removeAttribute(RetryContext.EXHAUSTED);
        context.removeAttribute(RetryContext.RECOVERED);
        return context;
    }

    private RetryContext doOpenInternal(RetryPolicy retryPolicy, RetryState state) {
        // RetrySynchronizationManager.getContext()其实是获取上一层的context，用来构建这一层的context
        RetryContext context = retryPolicy.open(RetrySynchronizationManager.getContext());
        if(state != null)
            context.setAttribute(RetryContext.STATE_KEY, state.getKey());
        if(context.hasAttribute(GLOBAL_STATE))
            registerContext(context, state);
        return context;
    }

    private RetryContext doOpenInternal(RetryPolicy retryPolicy) {
        return doOpenInternal(retryPolicy, null);
    }

    /**
     * 尝试次数耗尽后要执行的操作，如果有状态要清理，则清理cache。
     * 如果有recovery callback，则执行并返回其结果，否则应该抛出异常
     */
    protected <T> T handleRetryExhausted(RecoveryCallback<T> recoveryCallback,
                                         RetryContext context,
                                         RetryState state) throws Throwable {
        context.setAttribute(RetryContext.EXHAUSTED, true);
        if(state != null && !context.hasAttribute(GLOBAL_STATE))    // stateful模式，在这里也要清理cache了
            this.retryContextCache.remove(state.getKey());
        boolean doRecover = !Boolean.TRUE.equals(context.getAttribute(RetryContext.NO_RECOVERY));
        if(recoveryCallback != null) {
            if(doRecover) {
                try {
                    T recovered = recoveryCallback.recover(context);
                    context.setAttribute(RetryContext.RECOVERED, true);
                    return recovered;
                } catch (UndeclaredThrowableException undeclaredThrowableException) {
                    /**
                     * UndeclaredThrowableException是JDK动态代理和AOP在接口方法抛了声明之外的异常，从而被最终包装成的异常
                     * recover方法声明了throws Exception，如果是抛出了非Exception的Throwable，就会进入下面的catch了
                     * 注意使用的是getUndeclaredThrowable方法，即要抛出真正cause
                     */
                    throw wrapIfNecessary(undeclaredThrowableException.getUndeclaredThrowable());
                }
            }else {
                logger.debug("Retry exhausted and recovery disabled for this throwable");
            }
        }
        // 没有recovery callback，则抛出异常
        if(state != null) {
            /**
             * stateful模式的特点在于，RetryTemplate只是参与者，不负责retry的整个生命周期（不像stateless是负责全部）。
             * 真正的流程控制，其实在外部，业务代码类似这样：
             * try {
             *     retryTemplate.execute(callback, retryState);
             * } catch (Exception e) {
             *     // 在这里根据Exception的不同，而做不同的业务，例如回滚事务、重新发送、记录失败等，因为RetryTemplate本身无法触发补偿操作
             * }
             */
            this.logger.debug("Retry exhausted after last attempt with no recovery path.");
            // throwLastExceptionOnExhausted或不进行recovery，也要直接抛出原本异常，不要包装成ExhaustedRetryException
            rethrow(context, "Retry exhausted after last attempt with no recovery path",
                    this.throwLastExceptionOnExhausted || !doRecover);
        }
        throw wrapIfNecessary(context.getLastThrowable());
    }

    protected <E extends Throwable> void rethrow(RetryContext context, String message, boolean wrap) throws E {
        if(wrap) {
            E rethrow = (E)context.getLastThrowable();
            throw rethrow;
        }else {
            throw new ExhaustedRetryException(message, context.getLastThrowable());
        }
    }

    /**
     * 在RetryCallback中捕获异常后，子类可决定行为的扩展点。
     * 正常的stateless行为是不重新抛出异常，而stateful行为是重新抛出异常，不再同一个execute内部继续重试
     */
    protected boolean shouldRethrow(RetryPolicy retryPolicy, RetryContext context, RetryState state) {
        return state != null && state.rollbackFor(context.getLastThrowable());
    }

    /**
     * 在首次执行之前，先执行所有的listener的open，如果返回false,则doExecute将根本不执行retry（即提供一个否决retry的机会）
     */
    private <T, E extends Throwable> boolean doOpenInterceptors(RetryCallback<T, E> callback, RetryContext context) {
        boolean result = true;
        for(RetryListener listener : this.listeners)
            result = result && listener.open(context, callback);
        return result;
    }

    private <T, E extends Throwable> void doCloseInterceptors(RetryCallback<T, E> callback, RetryContext context,
                                                              Throwable lastException) {
        for(int i = this.listeners.length; i-- > 0;)
            this.listeners[i].close(context, callback, lastException);
    }

    private <T, E extends Throwable> void doOnSuccessInterceptors(RetryCallback<T, E> callback, RetryContext context,
                                                                  T result) {
        for(int i = this.listeners.length; i-- > 0;)
            this.listeners[i].onSuccess(context, callback, result);
    }

    private <T, E extends Throwable> void doOnErrorInterceptors(RetryCallback<T, E> callback, RetryContext context,
                                                                Throwable throwable) {
        for(int i = this.listeners.length; i-- > 0;)
            this.listeners[i].onError(context, callback, throwable);
    }

    /**
     * 如果传入的异常是Error则直接throw。
     * 如果传入的异常就是Exception，则直接返回并转成调用方想要的形式，否则以cause的形式封装成RetryException统一throw
     */
    private static <E extends Throwable> E wrapIfNecessary(Throwable t) throws RetryException {
        if(t instanceof Error) {
            throw (Error)t;
        }else if(t instanceof Exception) {
            E rethrow = (E)t;
            return rethrow;
        }else {
            throw new RetryException("Exception in retry", t);
        }
    }
}
