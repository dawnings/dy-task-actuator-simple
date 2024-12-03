package cn.dawnings.init;

import cn.dawnings.actuators.TaskActuator;
import cn.dawnings.config.CoreConfig;
import cn.dawnings.coustoms.DataFetchInterface;
import cn.dawnings.coustoms.MonitorRateDealInterface;
import cn.dawnings.coustoms.TaskCallBackInterface;
import cn.dawnings.coustoms.TaskRunnerInterface;
import cn.dawnings.monitor.MonitorCacuRateInterface;
import cn.dawnings.monitor.MonitorDataFetchInterface;
import cn.dawnings.monitor.MonitorStatusInterface;
import cn.dawnings.monitor.MonitorTaskInterface;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

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
     * @param taskLimitMax 最大任务队列数量
     * @return this
     */
    public TaskActuatorBuilder<T> taskLimitMax(int taskLimitMax) {
        coreConfig.setTaskLimitMax(taskLimitMax);
        return this;
    }
    /**
     * 设定任务启动延迟时间
     * 单位：ms
     * @param initDelay 任务启动延迟时间（ms）
     * @return this
     */
    public TaskActuatorBuilder<T> initDelay(int initDelay) {
        coreConfig.setInitDelay(initDelay);
        return this;
    }

    /**
     * 设定任务补充批次的最小尝试查询量
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
     * @param pollMinLimit 最小间隔时间（sec）
     * @return this
     */
    public TaskActuatorBuilder<T> pollMinLimit(int pollMinLimit) {
        coreConfig.setPollMinLimit(pollMinLimit);
        return this;
    }
    /**
     * 设定线程池大小
     * @param threadCount 线程池大小
     * @return this
     */
    public TaskActuatorBuilder<T> threadCount(int threadCount) {
        coreConfig.setThreadCount(threadCount);
        return this;
    }

    /**
     * 设定数据抓取接口
     * @param dataFetchInterface 数据抓取接口
     * @return this
     */
    public TaskActuatorBuilder<T> dataFetchInterface(DataFetchInterface<T> dataFetchInterface) {
        coreConfig.setDataFetchInterface(dataFetchInterface);
        return this;
    }

    /**
     * 设定任务执行接口
     * @param taskRunnerInterface 任务执行接口
     * @return this
     */
    public TaskActuatorBuilder<T> taskRunnerInterface(TaskRunnerInterface<T> taskRunnerInterface) {
        coreConfig.setTaskRunnerInterface(taskRunnerInterface);
        return this;
    }

    /**
     * 设定任务执行完回调接口
     * @param taskCallBackInterface 回调接口
     * @return this
     */
    public TaskActuatorBuilder<T> taskCallBackInterface(TaskCallBackInterface<T> taskCallBackInterface) {
        coreConfig.setTaskCallBackInterface(taskCallBackInterface);
        return this;
    }

    public TaskActuatorBuilder<T> monitorTaskInterface(MonitorTaskInterface<T> monitorTaskInterface) {
        coreConfig.setMonitorTaskInterface(monitorTaskInterface);
        return this;
    }

    public TaskActuatorBuilder<T> monitorStatusInterface(MonitorStatusInterface<T> monitorStatusInterface) {
        coreConfig.setMonitorStatusInterface(monitorStatusInterface);
        return this;
    }

    public TaskActuatorBuilder<T> monitorDataFetchInterface(MonitorDataFetchInterface<T> monitorDataFetchInterface) {
        coreConfig.setMonitorDataFetchInterface(monitorDataFetchInterface);
        return this;
    }


    public TaskActuatorBuilder<T> monitorCacuRateInterface(MonitorCacuRateInterface monitorCacuRateInterface) {
        coreConfig.setMonitorCacuRateInterface(monitorCacuRateInterface);
        return this;
    }

    public TaskActuatorBuilder<T> monitorRateDealInterface(MonitorRateDealInterface monitorRateDealInterface) {
        coreConfig.setMonitorRateDealInterface(monitorRateDealInterface);
        return this;
    }

    public TaskActuator<T> build() {
        if (coreConfig == null) {
            throw new IllegalArgumentException("Core configs are not set for the TaskActuatorBuilder.");
        }
        TaskActuator<T> taskActuators = new TaskActuator<>();
        taskActuators.init(coreConfig);
        taskActuatorMap.put(taskActuators.getThreadName(), taskActuators);
        return taskActuators;
    }
}
