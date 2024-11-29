package cn.dawnings.config;


import cn.dawnings.coustoms.DataFetchInterface;
import cn.dawnings.coustoms.MonitorRateDealInterface;
import cn.dawnings.coustoms.TaskCallBackInterface;
import cn.dawnings.coustoms.TaskRunnerInterface;
import cn.dawnings.defaults.MonitorCacuRateForMinute;
import cn.dawnings.monitor.MonitorDataFetchInterface;
import cn.dawnings.monitor.MonitorCacuRateInterface;
import cn.dawnings.monitor.MonitorStatusInterface;
import cn.dawnings.monitor.MonitorTaskInterface;
import lombok.Getter;
import lombok.Setter;

public class CoreConfig<T> {
    public CoreConfig() {
        pollMaxLimit = 5;
        taskLimitMax = 201;
        batchLimitMin = 100;
        threadCount = Runtime.getRuntime().availableProcessors() * 2 - 1;
        monitorCacuRateInterface = new MonitorCacuRateForMinute();
    }

    @Getter
    public boolean manualStop = false;

    public void stop() {
        this.manualStop = true;
    }

    /*

     */
    @Getter
    int taskLimitMax,
            batchLimitMin, pollMaxLimit;
    @Getter
    int threadCount;
    @Getter
    public boolean manualWait = false;

    public void setPollMaxLimit(int pollMaxLimit) {
        this.pollMaxLimit = pollMaxLimit;
    }

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

    public void setManualWait(boolean manualWait) {
        this.manualWait = manualWait;
    }
}
