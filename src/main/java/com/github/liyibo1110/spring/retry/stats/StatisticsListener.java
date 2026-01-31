package com.github.liyibo1110.spring.retry.stats;

import com.github.liyibo1110.spring.retry.RetryCallback;
import com.github.liyibo1110.spring.retry.RetryContext;
import com.github.liyibo1110.spring.retry.RetryListener;
import com.github.liyibo1110.spring.retry.RetryStatistics;
import com.github.liyibo1110.spring.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.core.AttributeAccessor;

/**
 * 统计功能的listener总实现
 * @author liyibo
 * @date 2026-01-31 20:29
 */
public class StatisticsListener implements RetryListener {
    private final StatisticsRepository repository;

    public StatisticsListener(StatisticsRepository repository) {
        this.repository = repository;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context,
                                               RetryCallback<T, E> callback,
                                               Throwable throwable) {
        String name = this.getName(context);
        if(name != null) {
            if(!isExhausted(context) || isGlobal(context))  // 没有exhausted就是成功了
                repository.addStarted(name);
            if(isRecovered(context))    // 标记了recovery就算进入recovery了
                repository.addRecovery(name);
            else if(isExhausted(context))
                repository.addAbort(name);
            else if(isClosed(context))
                repository.addComplete(name);
            RetryStatistics stats = repository.findOne(name);
            if(stats instanceof AttributeAccessor) {
                AttributeAccessor accessor = (AttributeAccessor)stats;
                for(String key : new String[] { CircuitBreakerRetryPolicy.CIRCUIT_OPEN,
                        CircuitBreakerRetryPolicy.CIRCUIT_SHORT_COUNT }) {
                    if(context.hasAttribute(key))
                        accessor.setAttribute(key, context.getAttribute(key));
                }
            }
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
                                                 RetryCallback<T, E> callback,
                                                 Throwable throwable) {
        String name = this.getName(context);
        if(name != null) {
            if(hasState(context))
                repository.addStarted(name);
            repository.addError(name);
        }
    }

    private boolean isGlobal(RetryContext context) {
        return context.hasAttribute("state.global");
    }

    private boolean isExhausted(RetryContext context) {
        return context.hasAttribute(RetryContext.EXHAUSTED);
    }

    private boolean isClosed(RetryContext context) {
        return context.hasAttribute(RetryContext.CLOSED);
    }

    private boolean isRecovered(RetryContext context) {
        return context.hasAttribute(RetryContext.RECOVERED);
    }

    private boolean hasState(RetryContext context) {
        return context.hasAttribute(RetryContext.STATE_KEY);
    }

    private String getName(RetryContext context) {
        return (String) context.getAttribute(RetryContext.NAME);
    }
}
