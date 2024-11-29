package cn.dawnings.actuators;

import cn.dawnings.config.CoreConfig;
import cn.dawnings.coustoms.DataFetchInterface;
import cn.dawnings.coustoms.MonitorRateDealInterface;
import cn.dawnings.coustoms.TaskCallBackInterface;
import cn.dawnings.coustoms.TaskRunnerInterface;
import cn.dawnings.dto.MontorTaskDto;
import cn.dawnings.monitor.MonitorCacuRateInterface;
import cn.dawnings.monitor.MonitorDataFetchInterface;
import cn.dawnings.monitor.MonitorStatusInterface;
import cn.dawnings.monitor.MonitorTaskInterface;
import cn.dawnings.util.LimitSizeMap;
import cn.hutool.core.util.RandomUtil;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TaskActuator<T> {
    private ExecutorService taskActuatorsExecutor;
    private boolean stop;

    private CoreConfig<T> configs;
    private ThreadPoolExecutor taskExecutor;
    private ThreadPoolExecutor fetchDataExecutor;
    @Getter
    private Date lastFetchTime;
    @Getter
    private int lastFetchCount;
    private BlockingQueue<T> replenishQueue;
    private List<T> lastFatchData;
    private DataFetchInterface<T> dataFetchInterface;
    private TaskRunnerInterface<T> taskRunnerInterface;
    private TaskCallBackInterface<T> taskCallBackInterface;
    private MonitorTaskInterface<T> monitorTaskInterface;
    private MonitorStatusInterface<T> monitorStatusInterface;
    private MonitorDataFetchInterface<T> monitorDataFetchInterface;
    private MonitorCacuRateInterface monitorCacuRateInterface;
    private MonitorRateDealInterface monitorRateDealInterface;
    @Getter
    private String threadName;
    private LimitSizeMap<String, AtomicInteger> monitorRates;

    public void start() {
        if (configs == null) {
            throw new RuntimeException("configs is null");
        }
        taskActuatorsExecutor.execute(this::execute);
    }

    public void exitActuator() {
        stop = true;
        fetchDataExecutor.shutdown();
        taskExecutor.shutdown();
        taskActuatorsExecutor.shutdown();

    }

    //初始化
    public void init(CoreConfig<T> configs) {
        this.configs = configs;
        dataFetchInterface = configs.getDataFetchInterface();
        if (dataFetchInterface == null) throw new RuntimeException("dataFetchInterface is null");
        taskRunnerInterface = configs.getTaskRunnerInterface();
        if (taskRunnerInterface == null) throw new RuntimeException("taskRunnerInterface is null");
        taskCallBackInterface = configs.getTaskCallBackInterface();

        monitorTaskInterface = configs.getMonitorTaskInterface();
        monitorStatusInterface = configs.getMonitorStatusInterface();
        monitorDataFetchInterface = configs.getMonitorDataFetchInterface();
        monitorCacuRateInterface = configs.getMonitorCacuRateInterface();
        monitorRateDealInterface = configs.getMonitorRateDealInterface();
        replenishQueue = new LinkedBlockingQueue<>(configs.getTaskLimitMax());

        threadName = "taskActuator-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4);
        taskActuatorsExecutor = Executors.newSingleThreadExecutor((r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(threadName);
            t.setDaemon(false);
            return t;
        }));
        taskExecutor = new ThreadPoolExecutor(configs.getThreadCount(), configs.getThreadCount(), 30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(configs.getTaskLimitMax()),
                (r -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setName(threadName + "-task-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
                    t.setDaemon(false);
                    return t;
                }));
        fetchDataExecutor = new ThreadPoolExecutor(1, 1, configs.getPollMaxLimit(), TimeUnit.MINUTES, new LinkedBlockingDeque<>(1),
                (r -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setName(threadName + "-fetch-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
                    t.setDaemon(false);
                    return t;
                }));
        fetchDataExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        lastFatchData = new ArrayList<>(configs.getTaskLimitMax());
        monitorRates = new LimitSizeMap<>();
        if (monitorCacuRateInterface != null) monitorRates.setMaxSize(monitorCacuRateInterface.getMonitorSize());

    }

    private void fetchData(boolean isFromPoll) {
        if (stopped()) return;
        int fullCa = taskExecutor.getQueue().remainingCapacity();
        int waitCa = replenishQueue.remainingCapacity();
        if (waitCa < configs.getBatchLimitMin() || fullCa < configs.getBatchLimitMin()) {
            if (monitorDataFetchInterface != null) monitorDataFetchInterface.monitor(-1, fullCa, waitCa, 0, 0, null);
            return;
        }
        lastFetchCount = Math.min(fullCa, waitCa);
        lastFetchTime = new Date();

        try {
            if (monitorDataFetchInterface != null)
                monitorDataFetchInterface.monitor(1, fullCa, waitCa, lastFetchCount, 0, null);
            final List<T> ts = dataFetchInterface.didDataFetch(lastFatchData, lastFetchCount);
            lastFatchData.clear();
            if (ts == null) {
                if (monitorDataFetchInterface != null)
                    monitorDataFetchInterface.monitor(-1, fullCa, waitCa, lastFetchCount, 0, null);
                if (!isFromPoll) Thread.sleep(configs.getPollMaxLimit() * 60 * 1000L);
                return;
            }
            lastFatchData.addAll(ts);
            if (monitorDataFetchInterface != null)
                monitorDataFetchInterface.monitor(2, fullCa, waitCa, lastFetchCount, lastFatchData.size(), null);
            replenishQueue.addAll(lastFatchData);
        } catch (Exception e) {
            lastFatchData.clear();
            log.error(e.getMessage());
            if (monitorDataFetchInterface != null)
                monitorDataFetchInterface.monitor(-1, fullCa, waitCa, lastFetchCount, 0, e);
        }
    }

    private boolean waiting() {
        return configs.manualWait;
//                || configs.stopFlag || isShutdown();
    }

    private boolean stopped() {
        return configs.manualStop || stop;
    }


    private void execute() {
        fetchData(false);
        while (true) {
            if (stopped()) {
                return;
            }
            try {
                Thread.sleep(0);
                //因其他原因产生的停机
                if (waiting()) {
                    Thread.sleep(1000 * 60 * 5);
                    continue;
                }
                //利用poll的延迟时间实现超时(定时)更新
                final T poll = replenishQueue.poll(configs.getPollMaxLimit(), TimeUnit.MINUTES);
                if (poll == null) {
                    fetchDataExecutor.submit(() -> {
                        fetchData(true);
                    });
                    continue;
                }
                final Future<MontorTaskDto<T>> submit = taskExecutor.submit(() -> {
                    MontorTaskDto<T> montorTaskDto = new MontorTaskDto<>();
                    montorTaskDto.setTaskData(poll);
                    try {
                        taskRunnerInterface.didTaskRunner(poll);
                        montorTaskDto.setTaskStatus(true);
                        if (taskCallBackInterface != null) taskCallBackInterface.callbackSuccess(poll);
                    } catch (Exception e) {
                        if (taskCallBackInterface != null) taskCallBackInterface.callbackError(poll, e);
                        montorTaskDto.setException(e);
                    } finally {
                        fetchDataExecutor.submit(() -> {
                            fetchData(false);
                        });
                    }
                    return montorTaskDto;
                });
                if (monitorTaskInterface != null) monitorTaskInterface.monitor(submit);
                if (monitorRateDealInterface != null && monitorCacuRateInterface != null) {
                    String monitorKey = monitorCacuRateInterface.getMonitorFormat();
                    monitorRate(monitorKey);
                }


            } catch (Exception e) {
                log.error(e.getMessage());
                if (monitorStatusInterface != null) monitorStatusInterface.monitor(false, e);
            }
        }
    }

    @Synchronized
    public void monitorRate(String key) {
        if (monitorRates.containsKey(key)) {
            monitorRates.get(key).incrementAndGet();
        } else {
            monitorRates.put(key, new AtomicInteger(1));
            if (monitorRateDealInterface != null && monitorCacuRateInterface != null)
                monitorRateDealInterface.monitor(monitorRates, key, monitorCacuRateInterface.getRate(monitorRates));
        }
    }

}
