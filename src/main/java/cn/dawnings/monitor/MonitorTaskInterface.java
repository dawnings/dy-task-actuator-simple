package cn.dawnings.monitor;

import cn.dawnings.dto.MontorTaskDto;

import java.util.concurrent.Future;

public interface MonitorTaskInterface<T> {

    void monitor(MontorTaskDto<T> submit);
}
