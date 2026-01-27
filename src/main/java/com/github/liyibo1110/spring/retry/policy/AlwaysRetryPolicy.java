package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;

/**
 * 允许一直retry的实现类，一般用作特定测试用途。
 * 这里直接继承了NeverRetryPolicy，但是只重写了canRetry方法相当于直接无视了NeverRetryContext对应的finished字段。
 * @author liyibo
 * @date 2026-01-26 23:23
 */
public class AlwaysRetryPolicy extends NeverRetryPolicy {

    public boolean canRetry(RetryContext context) {
        return true;
    }
}

