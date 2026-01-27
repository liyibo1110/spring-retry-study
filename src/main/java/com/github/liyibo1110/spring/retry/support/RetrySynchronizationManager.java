package com.github.liyibo1110.spring.retry.support;

import com.github.liyibo1110.spring.retry.RetryContext;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 为retry提供全局变量支持
 * @author liyibo
 * @date 2026-01-26 15:20
 */
public final class RetrySynchronizationManager {
    private RetrySynchronizationManager() {}

    private static final ThreadLocal<RetryContext> context = new ThreadLocal<>();
    private static final Map<Thread, RetryContext> contexts = new ConcurrentHashMap<>();
    private static boolean useThreadLocal = true;

    public static void setUseThreadLocal(boolean use) {
        useThreadLocal = use;
    }

    public static boolean isUseThreadLocal() {
        return useThreadLocal;
    }

    @Nullable
    public static RetryContext getContext() {
        if(useThreadLocal)
            return context.get();
        else
            return contexts.get(Thread.currentThread());
    }

    @Nullable
    public static RetryContext register(RetryContext context) {
        if(useThreadLocal) {
            RetryContext oldContext = getContext();
            RetrySynchronizationManager.context.set(context);
            return oldContext;  // 返回之前的context
        }else {
            RetryContext oldContext = contexts.get(Thread.currentThread());
            contexts.put(Thread.currentThread(), context);
            return oldContext;  // 返回之前的context
        }
    }

    /**
     * 清除当前保存的context，并尝试恢复为上级的context
     */
    @Nullable
    public static RetryContext clear() {
        RetryContext value = getContext();
        RetryContext parent = value == null ? null : value.getParent();
        if(useThreadLocal)
            RetrySynchronizationManager.context.set(parent);
        else {
            if(parent != null)
                contexts.put(Thread.currentThread(), parent);
            else
                contexts.remove(Thread.currentThread());
        }
        return value;   // 返回已清除的
    }
}
