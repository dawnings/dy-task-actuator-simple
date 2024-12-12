package cn.dawnings.config;


import cn.dawnings.coustoms.DataFetchSyncInterface;
import cn.dawnings.coustoms.RateLimitAcceptInterface;
import cn.dawnings.coustoms.TaskCallBackAsyncInterface;
import cn.dawnings.coustoms.TaskRunnerSyncInterface;
import cn.dawnings.defaults.CacuMonitorRateKeyFor10Min;
import cn.dawnings.defaults.CacuMonitorRateKeyFor1M;
import cn.dawnings.defaults.DefaultRateLimitAccept;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.dawnings.monitor.CacuMonitorRateKeyInterface;
import cn.dawnings.monitor.MonitorDataFetchAsyncInterface;
import cn.dawnings.monitor.MonitorRateMsgAsyncInterface;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;
import lombok.Setter;
import sun.reflect.Reflection;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public volatile boolean wait = false;
    @Getter
    @Setter
    private volatile Object customTag;
    @Getter
    @Setter
    public int initDelay;
    @Getter
    @Setter
    private volatile int pollMinLimit, pollMaxLimit;

    public String getTaskName() {
        if (StrUtil.isBlank(taskName))
            taskName = "taskActuator-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4);
        return taskName;
    }

    @Setter
    private volatile String taskName;
    @Setter
    @Getter
    private TaskRunnerSyncInterface<T> taskRunnerSyncInterface;
    @Setter
    @Getter
    private TaskCallBackAsyncInterface<T> taskCallBackAsyncInterface;
    @Setter
    @Getter
    private DataFetchSyncInterface<T> dataFetchSyncInterface;
    @Setter
    @Getter
    private MonitorDataFetchAsyncInterface<T> monitorDataFetchAsyncInterface;

    public RateLimitAcceptInterface getRateLimitAcceptInterface() {
        if (rateLimitAcceptInterface == null) rateLimitAcceptInterface = new DefaultRateLimitAccept();
        return rateLimitAcceptInterface;
    }

    @Setter
    private RateLimitAcceptInterface rateLimitAcceptInterface;

    public CacuMonitorRateKeyInterface getCacuMonitorRateKeyInterface() {
        if (cacuMonitorRateKeyInterface == null) cacuMonitorRateKeyInterface = new CacuMonitorRateKeyFor10Min();
        return cacuMonitorRateKeyInterface;
    }

    @Beta
    @Getter
    private volatile Map<String, RateLimiter> rateLimiters = new LinkedHashMap<>();

    @Beta
    public void addRateLimiters(Map<String, RateLimiter> rateLimiters) {
        this.rateLimiters.putAll(rateLimiters);
    }

    @Beta
    public void addRateLimiter(String key, RateLimiter lim) {
        rateLimiters.put(key, lim);
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
