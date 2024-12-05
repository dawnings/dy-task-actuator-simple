package cn.dawnings.init;

import cn.dawnings.actuators.TaskActuator;
import cn.dawnings.config.CoreConfig;
import cn.dawnings.coustoms.DataFetchSyncInterface;
import cn.dawnings.coustoms.RateLimitAcceptInterface;
import cn.dawnings.coustoms.TaskCallBackAsyncInterface;
import cn.dawnings.coustoms.TaskRunnerSyncInterface;
import cn.dawnings.monitor.CacuMonitorRateKeyInterface;
import cn.dawnings.monitor.MonitorDataFetchAsyncInterface;
import cn.dawnings.monitor.MonitorRateMsgAsyncInterface;
import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TaskActuatorBuilder<T> {
    /**
     * 任务执行器实例map
     * key是线程名称
     * value是任务执行器实例
     * 在各个任务执行线程中都可通过这个map获取到任务执行器实例
     * 非任务执行器托管的线程必须手动设定线程名称
     * 任务执行器总线程：taskActuator-name1
     * 任务线程：taskActuator-name1-task-name2
     * 数据采集线程：taskActuator-name1-fetch-name3
     * 速率监控线程：taskActuator-name1-fillMonitorRate-name4
     * 通知线程：taskActuator-name1-monitorAlarm-name5
     */
    public final static HashMap<String, TaskActuator<?>> taskActuatorMap = new HashMap<>();

    public static void removeTaskActuator(String taskName) {
        taskActuatorMap.remove(taskName);
    }

    private TaskActuatorBuilder() {

    }

    public static <T> TaskActuatorBuilder<T> builder(CoreConfig<T> coreConfig) {
        final TaskActuatorBuilder<T> objectTaskActuatorBuilder = new TaskActuatorBuilder<>();
        objectTaskActuatorBuilder.coreConfig = coreConfig;
        return objectTaskActuatorBuilder;
    }

    public static <T> TaskActuatorBuilder<T> builder() {
        final TaskActuatorBuilder<T> objectTaskActuatorBuilder = new TaskActuatorBuilder<>();
        objectTaskActuatorBuilder.coreConfig = new CoreConfig<>();
        return objectTaskActuatorBuilder;
    }

    private CoreConfig<T> coreConfig;

    /**
     * 设定最大任务队列数量
     *
     * @param taskLimitMax 最大任务队列数量
     * @return this
     */
    public TaskActuatorBuilder<T> taskLimitMax(int taskLimitMax) {
        coreConfig.setTaskLimitMax(taskLimitMax);
        return this;
    }

    public TaskActuatorBuilder<T> customTag(Object customTag) {
        coreConfig.setCustomTag(customTag);
        return this;
    }

    /**
     * 设定任务启动延迟时间
     * 单位：ms
     *
     * @param initDelay 任务启动延迟时间（ms）
     * @return this
     */
    public TaskActuatorBuilder<T> initDelay(int initDelay) {
        coreConfig.setInitDelay(initDelay);
        return this;
    }

    /**
     * 设定任务补充批次的最小尝试查询量
     *
     * @param batchLimitMin 最小尝试查询量
     * @return this
     */
    public TaskActuatorBuilder<T> batchLimitMin(int batchLimitMin) {
        coreConfig.setBatchLimitMin(batchLimitMin);
        return this;
    }

    /**
     * 设定任务补充队列的最大超时等待时间
     * 单位：min
     *
     * @param pollMaxLimit 超时等待时间（min）
     * @return this
     */
    public TaskActuatorBuilder<T> pollMaxLimit(int pollMaxLimit) {
        coreConfig.setPollMaxLimit(pollMaxLimit);
        return this;
    }

    /**
     * 设定任务补充队列的最小间隔时间
     * 单位：sec
     *
     * @param pollMinLimit 最小间隔时间（sec）
     * @return this
     */
    public TaskActuatorBuilder<T> pollMinLimit(int pollMinLimit) {
        coreConfig.setPollMinLimit(pollMinLimit);
        return this;
    }

    /**
     * 设定线程池大小
     *
     * @param threadCount 线程池大小
     * @return this
     */
    public TaskActuatorBuilder<T> threadCount(int threadCount) {
        coreConfig.setThreadCount(threadCount);
        return this;
    }

    /**
     * 设定数据抓取接口
     *
     * @param dataFetchSyncInterface 数据抓取接口
     * @return this
     */
    public TaskActuatorBuilder<T> dataFetchSyncInterface(DataFetchSyncInterface<T> dataFetchSyncInterface) {
        coreConfig.setDataFetchSyncInterface(dataFetchSyncInterface);
        return this;
    }

    /**
     * 设定任务执行接口
     *
     * @param taskRunnerSyncInterface 任务执行接口
     * @return this
     */
    public TaskActuatorBuilder<T> taskRunnerSyncInterface(TaskRunnerSyncInterface<T> taskRunnerSyncInterface) {
        coreConfig.setTaskRunnerSyncInterface(taskRunnerSyncInterface);
        return this;
    }

    /**
     * 设定任务执行完回调接口
     *
     * @param taskCallBackAsyncInterface 回调接口
     * @return this
     */
    public TaskActuatorBuilder<T> taskCallBacksyncInterface(TaskCallBackAsyncInterface<T> taskCallBackAsyncInterface) {
        coreConfig.setTaskCallBacksyncInterface(taskCallBackAsyncInterface);
        return this;
    }


    public TaskActuatorBuilder<T> monitorDataFetchAsyncInterface(MonitorDataFetchAsyncInterface<T> monitorDataFetchAsyncInterface) {
        coreConfig.setMonitorDataFetchAsyncInterface(monitorDataFetchAsyncInterface);
        return this;
    }


    public TaskActuatorBuilder<T> cacuMonitorRateKeyInterface(CacuMonitorRateKeyInterface cacuMonitorRateKeyInterface) {
        coreConfig.setCacuMonitorRateKeyInterface(cacuMonitorRateKeyInterface);
        return this;
    }

    public TaskActuatorBuilder<T> monitorRateMsgAsyncInterface(MonitorRateMsgAsyncInterface monitorRateMsgAsyncInterface) {
        coreConfig.setMonitorRateMsgAsyncInterface(monitorRateMsgAsyncInterface);
        return this;
    }

    /**
     * 限流器判断接口
     * @param rateLimitAcceptInterface 限流器判断接口
     * @return this
     */
    public TaskActuatorBuilder<T> rateLimitAcceptInterface(RateLimitAcceptInterface rateLimitAcceptInterface) {
        coreConfig.setRateLimitAcceptInterface(rateLimitAcceptInterface);
        return this;
    }
    @Beta
    public TaskActuatorBuilder<T> rateLimiters(Map<String, RateLimiter> rateLimiters) {
        coreConfig.addRateLimiters(rateLimiters);
        return this;
    }

    /**
     * 添加一个限流器
     * <p>RateLimiter.create(double permitsPerSecond, SleepingStopwatch stopwatch)</p>
     * <p>利用key 来区分不同的限流器，同一个 key 的限流器可以被覆盖。可以通过key实现业务分流、也可以通过默认的实现多级分流</p>
     * @param key 限流器key
     * @param rateLimiter 限流器
     * @return this
     */
    @Beta
    public TaskActuatorBuilder<T> addRateLimiter(String key, RateLimiter rateLimiter) {
        coreConfig.addRateLimiter(key, rateLimiter);
        return this;
    }

    public TaskActuator<T> build() {
        if (coreConfig == null) {
            throw new IllegalArgumentException("Core configs are not set for the TaskActuatorBuilder.");
        }
        TaskActuator<T> taskActuators = new TaskActuator<>();
        taskActuators.setCustomTag(coreConfig.getCustomTag());
        taskActuators.init(coreConfig);
        taskActuatorMap.put(taskActuators.getTaskName(), taskActuators);
        return taskActuators;
    }
}
