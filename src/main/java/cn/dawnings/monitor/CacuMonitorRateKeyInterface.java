package cn.dawnings.monitor;

import cn.dawnings.util.LimitSizeMap;

import java.util.concurrent.atomic.AtomicInteger;

public interface CacuMonitorRateKeyInterface {
    String getRate(LimitSizeMap<String, AtomicInteger> monitorRates);

    String getMonitorFormat();

    int getMonitorSize();
}
