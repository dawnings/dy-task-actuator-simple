package cn.dawnings.config;


import cn.dawnings.coustoms.DataFetchSyncInterface;
import cn.dawnings.monitor.MonitorRateMsgAsyncInterface;
import cn.dawnings.coustoms.TaskCallBackAsyncInterface;
import cn.dawnings.coustoms.TaskRunnerSyncInterface;
import cn.dawnings.defaults.CacuMonitorRateKeyFor10Min;
import cn.dawnings.defaults.CacuMonitorRateKeyFor1M;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.dawnings.monitor.CacuMonitorRateKeyInterface;
import cn.dawnings.monitor.MonitorDataFetchAsyncInterface;
import lombok.Getter;
import lombok.Setter;
import sun.reflect.Reflection;

public final class CoreConfig<T> {


    public CoreConfig() {
        pollMaxLimit = 5;
        pollMinLimit = 30;
        taskLimitMax = 201;
        initDelay = 120;
        batchLimitMin = 100;
        threadCount = Runtime.getRuntime().availableProcessors() * 2 - 1;
        cacuMonitorRateKeyInterface = new CacuMonitorRateKeyFor1M();
    }

    @Getter
    volatile int taskLimitMax,
            batchLimitMin;

    @Getter
    volatile int threadCount;
    @Getter
    public volatile boolean manualWait = false;
    @Getter
    @Setter
    private volatile Object customTag;
    @Getter
    @Setter
    public int initDelay;
    @Getter
    @Setter
    private volatile int pollMinLimit, pollMaxLimit;


    @Setter
    @Getter
    private TaskRunnerSyncInterface<T> taskRunnerSyncInterface;
    @Setter
    @Getter
    private TaskCallBackAsyncInterface<T> taskCallBacksyncInterface;
    @Setter
    @Getter
    private DataFetchSyncInterface<T> dataFetchSyncInterface;
    @Setter
    @Getter
    private MonitorDataFetchAsyncInterface<T> monitorDataFetchAsyncInterface;

    public CacuMonitorRateKeyInterface getCacuMonitorRateKeyInterface() {
        if (cacuMonitorRateKeyInterface == null) cacuMonitorRateKeyInterface = new CacuMonitorRateKeyFor10Min();
        return cacuMonitorRateKeyInterface;
    }

    //    @Getter
    @Setter
    private volatile CacuMonitorRateKeyInterface cacuMonitorRateKeyInterface;
    @Getter
    @Setter
    private MonitorRateMsgAsyncInterface monitorRateMsgAsyncInterface;

    public void setTaskLimitMax(int taskLimitMax) {
        Class<?> caller = Reflection.getCallerClass(2);
        if (caller != TaskActuatorBuilder.class)
            throw new SecurityException("Unsafe");
        if (taskLimitMax < 10) throw new IllegalArgumentException("taskLimitMax  must be greater than 10");
        this.taskLimitMax = taskLimitMax;
    }

    public void setBatchLimitMin(int batchLimitMin) {
        if (batchLimitMin < 10) {
            throw new IllegalArgumentException("batchLimitMin  must be greater than 10");
        }
        if (taskLimitMax < batchLimitMin) {
            throw new IllegalArgumentException("taskLimitMax  must be greater than batchLimitMin");
        }
        this.batchLimitMin = batchLimitMin;
    }

    public void setThreadCount(int threadCount) {
        if (threadCount <= 0) throw new IllegalArgumentException("threadCount must be greater than zero");
        this.threadCount = threadCount;
    }

}
