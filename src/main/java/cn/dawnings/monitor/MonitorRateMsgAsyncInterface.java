package cn.dawnings.monitor;

import cn.dawnings.dto.MontorRateMsgDto;
import cn.dawnings.util.LimitSizeMap;

import java.util.concurrent.atomic.AtomicInteger;

public interface MonitorRateMsgAsyncInterface {
    void monitor(MontorRateMsgDto montorRateMsgDto);
}
