package com.siyukio.tools.collection;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A general-purpose, thread-safe cache with no limit on the number of entries.
 * It is automatically cleared when the JVM is low on memory.
 *
 * @author Buddy
 */
public class ConcurrentCache<K, V> {

    private final int size;

    private final Map<K, V> eden;

    private final Map<K, V> longTerm;

    private final ReentrantLock lock = new ReentrantLock();

    public ConcurrentCache(int size) {
        this.size = size;
        int realSize = size / 10;
        this.eden = new ConcurrentHashMap<>(realSize);
        this.longTerm = new WeakHashMap<>(size);
    }

    public V get(K k) {
        V v = this.eden.get(k);
        if (v == null) {
            this.lock.lock();
            try {
                v = this.longTerm.get(k);
            } finally {
                this.lock.unlock();
            }
            if (v != null) {
                this.eden.put(k, v);
            }
        }
        return v;
    }

    public void put(K k, V v) {
        if (this.eden.size() >= size) {
            this.lock.lock();
            try {
                this.longTerm.putAll(this.eden);
            } finally {
                this.lock.unlock();
            }
            this.eden.clear();
        }
        this.eden.put(k, v);
    }

    public void remove(K k) {
        this.eden.remove(k);
        this.lock.lock();
        try {
            this.longTerm.remove(k);
        } finally {
            this.lock.unlock();
        }
    }

    public void removeAll() {
        this.eden.clear();
        this.lock.lock();
        try {
            this.longTerm.clear();
        } finally {
            this.lock.unlock();
        }
    }
}
