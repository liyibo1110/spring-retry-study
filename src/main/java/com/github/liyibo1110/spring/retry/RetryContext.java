package com.github.liyibo1110.spring.retry;

import org.springframework.core.AttributeAccessor;
import org.springframework.lang.Nullable;

/**
 * 对正在进行重试操作的低级访问
 * 客户端通常不需要这种访问，但可用于改变重试过程，例如强制提前中止
 * @author liyibo
 * @date 2026-01-23 00:35
 */
public interface RetryContext extends AttributeAccessor {

    /**
     * 重试上下文属性名，用作report key，可用于report
     * 例如在重试监听器中，积累有关重试性能的数据
     */
    String NAME = "context.name";

    /**
     * 状态key的重试上下文属性名称，用于从上下文中识别有状态的重试
     */
    String STATE_KEY = "context.state";

    /**
     * 如果上下文已关闭，则重试非空（且为真）的上下文属性
     */
    String CLOSED = "context.closed";

    /**
     * 如果开启了recover，则重试非空（且为真）的上下文属性
     */
    String RECOVERED = "context.recovered";

    /**
     * 如果重试次数用尽，则重试上下文属性为非空（且为真）
     */
    String EXHAUSTED = "context.exhausted";

    /**
     * 如果异常不可恢复，则重试非空（且为真）的上下文属性
     */
    String NO_RECOVERY = "context.no-recovery";

    /**
     * 表示对于在失败前提供最大尝试次数的策略，其最大尝试次数
     * 对于其它策略，返回的值是RetryPolicy. NO_MAXIMUM_ATTEMPTS_SET
     */
    String MAX_ATTEMPTS = "context.max-attempts";

    /**
     * 设置并表明不应再尝试当前的RetryCallback了
     */
    void setExhaustedOnly();

    boolean isExhaustedOnly();

    /**
     * 如果有重试块嵌套，则为父级上下文提供访问器
     */
    @Nullable
    RetryContext getParent();

    /**
     * 统计重试的尝试次数，首次尝试前，计数器为0
     */
    int getRetryCount();

    /**
     * 返回导致当前重试的异常对象
     */
    @Nullable
    Throwable getLastThrowable();
}
