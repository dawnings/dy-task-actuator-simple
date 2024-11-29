package cn.dawnings.monitor;

import java.util.concurrent.Future;

public interface MonitorTaskInterface<T> {

    void monitor(Future<?> submit);
}
