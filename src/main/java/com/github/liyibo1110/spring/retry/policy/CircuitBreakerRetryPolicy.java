package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryPolicy;
import com.github.liyibo1110.spring.retry.context.RetryContextSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 熔断器拦截器专用的retry policy，改变了接口方法语义，用来实现方法级别的熔断效果（相当于是个装饰器组件）
 * @author liyibo
 * @date 2026-01-30 14:50
 */
public class CircuitBreakerRetryPolicy implements RetryPolicy {
    private static final Log logger = LogFactory.getLog(CircuitBreakerRetryPolicy.class);
    /** 熔断器状态1 -- 开启熔断 */
    public static final String CIRCUIT_OPEN = "circuit.open";
    public static final String CIRCUIT_SHORT_COUNT = "circuit.shortCount";
    /** 实际的retry policy */
    private final RetryPolicy delegate;
    /** 开始熔断20秒之后，允许一次尝试（切换到half-open状态） */
    private long resetTimeout = 20000;
    /** 5秒内遇到失败，就要开启熔断 */
    private long openTimeout = 5000;
    private Supplier<Long> resetTimeoutSupplier;

    private Supplier<Long> openTimeoutSupplier;

    public CircuitBreakerRetryPolicy() {
        this(new SimpleRetryPolicy());
    }

    public CircuitBreakerRetryPolicy(RetryPolicy delegate) {
        this.delegate = delegate;
    }

    public void setResetTimeout(long timeout) {
        this.resetTimeout = timeout;
    }

    public void resetTimeoutSupplier(Supplier<Long> timeoutSupplier) {
        this.resetTimeoutSupplier = timeoutSupplier;
    }

    public void setOpenTimeout(long timeout) {
        this.openTimeout = timeout;
    }

    public void openTimeoutSupplier(Supplier<Long> timeoutSupplier) {
        this.openTimeoutSupplier = timeoutSupplier;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        CircuitBreakerRetryContext circuit = (CircuitBreakerRetryContext)context;
        if(circuit.isOpen()) {  // 熔断器如果开着，直接false
            circuit.incrementShortCircuitCount();
            return false;
        }else {
            circuit.reset();
        }
        // 熔断器说明没开，正常走实际的canRetry
        return this.delegate.canRetry(circuit.context);
    }

    @Override
    public RetryContext open(RetryContext parent) {
        long resetTimeout = this.resetTimeout;
        if(this.resetTimeoutSupplier != null)
            resetTimeout = this.resetTimeoutSupplier.get();
        long openTimeout = this.openTimeout;
        if(this.openTimeoutSupplier != null)
            openTimeout = this.openTimeoutSupplier.get();
        return new CircuitBreakerRetryContext(parent, this.delegate, resetTimeout, openTimeout);
    }

    @Override
    public void close(RetryContext context) {
        CircuitBreakerRetryContext circuit = (CircuitBreakerRetryContext)context;
        this.delegate.close(circuit.context);
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        CircuitBreakerRetryContext circuit = (CircuitBreakerRetryContext)context;
        circuit.registerThrowable(throwable);
        this.delegate.registerThrowable(circuit.context, throwable);
    }

    /**
     * 封装了一层内部的RetryContext和RetryPolicy
     */
    static class CircuitBreakerRetryContext extends RetryContextSupport {
        private volatile RetryContext context;
        private final RetryPolicy policy;
        private volatile long start = System.currentTimeMillis();
        private final long timeout;
        private final long openWindow;
        /** 记录熔断期间被直接拒绝的次数，仅用于监控/诊断，不参与状态判断 */
        private final AtomicInteger shortCircuitCount = new AtomicInteger();

        public CircuitBreakerRetryContext(RetryContext parent, RetryPolicy policy, long timeout, long openWindow) {
            super(parent);
            this.context = createDelegateContext(policy, parent);
            this.policy = policy;
            this.timeout = timeout;
            this.openWindow = openWindow;
            setAttribute("state.global", true);
        }

        /**
         * 重置熔断状态（即关闭熔断）
         */
        public void reset() {
            shortCircuitCount.set(0);
            setAttribute(CIRCUIT_SHORT_COUNT, shortCircuitCount.get());
        }

        public void incrementShortCircuitCount() {
            shortCircuitCount.incrementAndGet();
            setAttribute(CIRCUIT_SHORT_COUNT, shortCircuitCount.get());
        }

        private RetryContext createDelegateContext(RetryPolicy policy, RetryContext parent) {
            RetryContext context = policy.open(parent);
            reset();
            return context;
        }

        /**
         * 核心方法，判断是否要开启熔断状态并阻止继续retry
         * @return
         */
        public boolean isOpen() {
            long time = System.currentTimeMillis() - this.start;    // context的生存时间
            Boolean retryable = this.policy.canRetry(this.context);
            if(retryable) { // 本身的policy就可以retry，肯定要放行，不会开启熔断
                if(time > this.openWindow) {    // 如果context存活时间已超过熔断阈值时间，要重置熔断计数器
                    logger.trace("Resetting context");
                    this.start = System.currentTimeMillis();
                    this.context = createDelegateContext(policy, getParent());
                }
            }else { // 本身policy已经不能再retry，要判断2次失败的时间差，是否小于熔断阈值时间
                if(time > this.timeout) {   // 2次失败的时间差，大于熔断阈值时间，即熔断期结束了，允许重新尝试
                    logger.trace("Closing");
                    this.context = createDelegateContext(policy, getParent());  // 半开状态，没有设计相关的字段，通过重建delegate的context来间接实现
                    this.start = System.currentTimeMillis();
                    retryable = this.policy.canRetry(this.context);
                }else if(time < this.openWindow) {  // 2次失败的时间差，小于熔断阈值时间，要开启熔断即不允许canRetry
                    if(!hasAttribute(CIRCUIT_OPEN) || (Boolean)getAttribute(CIRCUIT_OPEN) == false) {
                        logger.trace("Opening circuit");
                        setAttribute(CIRCUIT_OPEN, true);
                        this.start = System.currentTimeMillis();
                    }
                    return true;
                }
            }
            if(logger.isTraceEnabled())
                logger.trace("Open: " + !retryable);
            setAttribute(CIRCUIT_OPEN, !retryable);
            return !retryable;
        }

        @Override
        public int getRetryCount() {
            return this.context.getRetryCount();
        }

        @Override
        public String toString() {
            return this.context.toString();
        }
    }
}
