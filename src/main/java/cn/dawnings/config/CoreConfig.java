package cn.dawnings.config;


import cn.dawnings.coustoms.DataFetchInterface;
import cn.dawnings.coustoms.MonitorRateDealInterface;
import cn.dawnings.coustoms.TaskCallBackInterface;
import cn.dawnings.coustoms.TaskRunnerInterface;
import cn.dawnings.defaults.MonitorCacuRateFor1m;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.dawnings.monitor.MonitorCacuRateInterface;
import cn.dawnings.monitor.MonitorDataFetchInterface;
import cn.dawnings.monitor.MonitorStatusInterface;
import cn.dawnings.monitor.MonitorTaskInterface;
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
        monitorCacuRateInterface = new MonitorCacuRateFor1m();
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
    private TaskRunnerInterface<T> taskRunnerInterface;
    @Setter
    @Getter
    private TaskCallBackInterface<T> taskCallBackInterface;
    @Setter
    @Getter
    private DataFetchInterface<T> dataFetchInterface;
    @Setter
    @Getter
    private MonitorTaskInterface<T> monitorTaskInterface;
    @Setter
    @Getter
    private MonitorStatusInterface<T> monitorStatusInterface;
    @Setter
    @Getter
    private MonitorDataFetchInterface<T> monitorDataFetchInterface;
    @Getter
    @Setter
    private MonitorCacuRateInterface monitorCacuRateInterface;
    @Getter
    @Setter
    private MonitorRateDealInterface monitorRateDealInterface;

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
