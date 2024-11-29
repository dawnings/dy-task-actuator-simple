package cn.dawnings.coustoms;

import cn.dawnings.util.LimitSizeMap;

import java.util.concurrent.atomic.AtomicInteger;

public interface MonitorRateDealInterface {
    public void monitor(LimitSizeMap<String, AtomicInteger> monitorRates, String key, String rate, AtomicInteger lastSize);
}
