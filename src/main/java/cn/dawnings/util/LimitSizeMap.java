package cn.dawnings.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LimitSizeMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;
    public void setMaxSize(int maxSize) {
        MAX_SIZE = maxSize;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > MAX_SIZE;
    }

    private volatile int MAX_SIZE = 1024; // 默认最大容量为1024


    @Override
    public V put(K key, V value) {
        return super.put(key, value);
    }
}
