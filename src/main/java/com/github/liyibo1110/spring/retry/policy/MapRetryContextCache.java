package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于Map的同步实现类
 * @author liyibo
 * @date 2026-01-24 22:35
 */
public class MapRetryContextCache implements RetryContextCache {
    public static final int DEFAULT_CAPACITY = 4096;

    private final Map<Object, RetryContext> map = Collections.synchronizedMap(new HashMap<>());

    private int capacity;

    public MapRetryContextCache() {
        this(DEFAULT_CAPACITY);
    }

    public MapRetryContextCache(int defaultCapacity) {
        super();
        this.capacity = defaultCapacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public RetryContext get(Object key) {
        return map.get(key);
    }

    @Override
    public void put(Object key, RetryContext context) throws RetryCacheCapacityExceededException {
        if(map.size() >= capacity) {
            throw new RetryCacheCapacityExceededException("Retry cache capacity limit breached. "
                    + "Do you need to re-consider the implementation of the key generator, "
                    + "or the equals and hashCode of the items that failed?");
        }
        map.put(key, context);
    }

    @Override
    public void remove(Object key) {
        map.remove(key);
    }
}
