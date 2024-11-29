package cn.dawnings.monitor;

import cn.dawnings.util.LimitSizeMap;

import java.util.concurrent.atomic.AtomicInteger;

public interface MonitorCacuRateInterface {
    public String getRate(LimitSizeMap<String, AtomicInteger> monitorRates);

    public String getMonitorFormat();

    public int getMonitorSize();
}
