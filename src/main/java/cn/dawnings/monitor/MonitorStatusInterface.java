package cn.dawnings.monitor;

public interface MonitorStatusInterface<T> {

    void monitor(boolean status, Throwable e);
}
