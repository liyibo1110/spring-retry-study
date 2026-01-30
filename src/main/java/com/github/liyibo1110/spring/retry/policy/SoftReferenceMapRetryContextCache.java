package com.github.liyibo1110.spring.retry.policy;

import com.github.liyibo1110.spring.retry.RetryContext;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于Map的同步实现类，但值为软引用
 * @author liyibo
 * @date 2026-01-30 14:43
 */
public class SoftReferenceMapRetryContextCache implements RetryContextCache {
    public static final int DEFAULT_CAPACITY = 4096;
    private final Map<Object, SoftReference<RetryContext>> map = Collections.synchronizedMap(new HashMap<>());
    private int capacity;

    public SoftReferenceMapRetryContextCache() {
        this(DEFAULT_CAPACITY);
    }

    public SoftReferenceMapRetryContextCache(int defaultCapacity) {
        super();
        this.capacity = defaultCapacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean containsKey(Object key) {
        if(!map.containsKey(key))
            return false;
        if(map.get(key).get() == null)  // value已被GC
            map.remove(key);
        return map.containsKey(key);
    }

    @Override
    public RetryContext get(Object key) {
        return map.get(key).get();
    }

    @Override
    public void put(Object key, RetryContext context) {
        if(map.size() >= capacity)
            throw new RetryCacheCapacityExceededException("Retry cache capacity limit breached. "
                    + "Do you need to re-consider the implementation of the key generator, "
                    + "or the equals and hashCode of the items that failed?");
        map.put(key, new SoftReference<>(context));
    }

    @Override
    public void remove(Object key) {
        map.remove(key);
    }
}
