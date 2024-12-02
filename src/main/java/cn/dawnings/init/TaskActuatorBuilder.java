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


    public TaskActuatorBuilder<T> taskLimitMax(int taskLimitMax) {
        coreConfig.setTaskLimitMax(taskLimitMax);
        return this;
    }
    public TaskActuatorBuilder<T> initDelay(int initDelay) {
        coreConfig.setInitDelay(initDelay);
        return this;
    }
    public TaskActuatorBuilder<T> batchLimitMin(int batchLimitMin) {
        coreConfig.setBatchLimitMin(batchLimitMin);
        return this;
    }

    public TaskActuatorBuilder<T> pollMaxLimit(int pollMaxLimit) {
        coreConfig.setPollMaxLimit(pollMaxLimit);
        return this;
    }

    public TaskActuatorBuilder<T> threadCount(int threadCount) {
        coreConfig.setThreadCount(threadCount);
        return this;
    }


    public TaskActuatorBuilder<T> dataFetchInterface(DataFetchInterface<T> dataFetchInterface) {
        coreConfig.setDataFetchInterface(dataFetchInterface);
        return this;
    }

    public TaskActuatorBuilder<T> taskRunnerInterface(TaskRunnerInterface<T> taskRunnerInterface) {
        coreConfig.setTaskRunnerInterface(taskRunnerInterface);
        return this;
    }

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
