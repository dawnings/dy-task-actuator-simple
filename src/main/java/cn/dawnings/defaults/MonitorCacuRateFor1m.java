package cn.dawnings.defaults;

import cn.dawnings.monitor.MonitorCacuRateInterface;
import cn.dawnings.util.LimitSizeMap;
import cn.hutool.core.date.DateUtil;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitorCacuRateFor1m implements MonitorCacuRateInterface {
    @Override
    public String getRate(LimitSizeMap<String, AtomicInteger> monitorRates) {
        final OptionalDouble average = monitorRates.values().stream().mapToInt(AtomicInteger::get).average();
        if (average.isPresent()) {
            return String.format("%.2f", average.getAsDouble());
        }
        return "0.00";
    }

    @Override
    public String getMonitorFormat() {
        return  DateUtil.format(LocalDateTime.now(),"yyyy-MM-dd-HH-mm");
    }

    @Override
    public int getMonitorSize() {
        return 600;
    }
}
