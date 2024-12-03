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
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TaskActuator<T> {
    //监控数据
    private LimitSizeMap<String, AtomicInteger> monitorRates;
    private Semaphore semaphore;
    //最后一次数据获取时间
    @Getter
    private LocalDateTime lastFetchTime;
    //最后一次数据获取的数量
    @Getter
    private int lastFetchCount;
    //用于存储上一次数据填充任务的返回结果，以便后续fetch数据时避让
    @Getter
    private List<T> lastFatchData;
    //执行器线程的名称
    @Getter
    private String threadName;
    //执行器自身所在的线程池，单线程定时从队列中获取任务，经过测试，低于20ms执行效率不会有提升，但会有显著的cpu消耗增加
    private ScheduledExecutorService taskActuatorsExecutor;
    //用于填充监控的线程池，定时获取令牌填充防止漏填充
    private ScheduledExecutorService fillMonitorRateExecutor;
    //监控回调线程池，为保证监控回调不会阻塞任务
    private ThreadPoolExecutor monitorAlarmExecutor;
    //真正的任务执行线程池
    private ThreadPoolExecutor taskExecutor;
    //数据填充任务线程池，单线程，多余任务直接抛弃
    private ThreadPoolExecutor fetchDataExecutor;

    //执行状态，标记为true后整个执行器终止，无法自动恢复
    private boolean stop;
    //总体配置
    private CoreConfig<T> configs;
    //用于发布任务的队列
    private BlockingQueue<T> replenishQueue;
    private BlockingQueue<Runnable> workingQueue;
    //数据获取接口
    private DataFetchInterface<T> dataFetchInterface;
    //任务执行接口
    private TaskRunnerInterface<T> taskRunnerInterface;
    //任务回调接口
    private TaskCallBackInterface<T> taskCallBackInterface;
    //任务监控回调接口
    private MonitorTaskInterface<T> monitorTaskInterface;
    //执行器状态回调接口
    private MonitorStatusInterface<T> monitorStatusInterface;
    //数据获取回调接口
    private MonitorDataFetchInterface<T> monitorDataFetchInterface;
    //监控速度计算接口
    private MonitorCacuRateInterface monitorCacuRateInterface;
    //监控速率回调接口
    private MonitorRateDealInterface monitorRateDealInterface;


    public void start() {
        if (configs == null) {
            throw new RuntimeException("configs is null");
        }
        fetchData(false);
        semaphore = new Semaphore(configs.getTaskLimitMax());
        taskActuatorsExecutor.scheduleWithFixedDelay(this::execute, configs.getInitDelay(), 20, TimeUnit.MILLISECONDS);
    }

    public boolean hadShutDown() {
        return taskExecutor.isShutdown() && taskExecutor.isTerminated();
    }

    public void exitActuator() {
        stop = true;
        fetchDataExecutor.shutdown();
        taskExecutor.shutdown();
        taskActuatorsExecutor.shutdown();
        fillMonitorRateExecutor.shutdown();
        monitorAlarmExecutor.shutdown();
        semaphore.release(semaphore.availablePermits());
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
        taskActuatorsExecutor = Executors.newSingleThreadScheduledExecutor((r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(threadName);
            t.setDaemon(false);
            return t;
        }));
        workingQueue = new LinkedBlockingQueue<>(configs.getTaskLimitMax());
        taskExecutor = new ThreadPoolExecutor(configs.getThreadCount(), configs.getThreadCount(), 30L, TimeUnit.SECONDS,
                workingQueue,
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
        if (monitorCacuRateInterface != null && monitorRateDealInterface != null) {
            monitorRates.setMaxSize(monitorCacuRateInterface.getMonitorSize());
            fillMonitorRateExecutor = Executors.newSingleThreadScheduledExecutor((r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName(threadName + "-fillMonitorRate-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
                t.setDaemon(false);
                return t;
            }));
            fillMonitorRateExecutor.scheduleWithFixedDelay(() -> {
                final String monitorFormat = monitorCacuRateInterface.getMonitorFormat();
                addMonitorRate(monitorFormat);
            }, 10, 500, TimeUnit.MICROSECONDS);
            monitorAlarmExecutor = new ThreadPoolExecutor(1, 1, configs.getPollMaxLimit(), TimeUnit.MINUTES, new LinkedBlockingDeque<>(configs.getBatchLimitMin()),
                    (r -> {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setName(threadName + "-monitorAlarm-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
                        t.setDaemon(false);
                        return t;
                    }));
            monitorAlarmExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        }


    }

    private void fetchData(boolean isFromPoll) {
        if (stopped()) return;
        int fullCa = taskExecutor.getQueue().remainingCapacity() - taskExecutor.getQueue().size();
        int waitCa = replenishQueue.remainingCapacity();
        if (waitCa < configs.getBatchLimitMin() || fullCa < configs.getBatchLimitMin()) {
            if (monitorDataFetchInterface != null) monitorDataFetchInterface.monitor(-1, fullCa, waitCa, 0, 0, null);
            return;
        }
        lastFetchCount = Math.min(fullCa, waitCa);
        lastFetchTime = LocalDateTime.now();

        try {
            if (monitorDataFetchInterface != null)
                monitorDataFetchInterface.monitor(1, fullCa, waitCa, lastFetchCount, 0, null);
            final List<T> ts = dataFetchInterface.didDataFetch(lastFatchData, lastFetchCount);
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

        if (stopped()) {
            return;
        }
        try {

            //因其他原因产生的停机
            if (waiting()) {
                Thread.sleep(1000 * 60 * 5);
                return;
            }
            //利用poll的延迟时间实现超时(定时)更新
            final T poll = replenishQueue.poll(configs.getPollMaxLimit(), TimeUnit.MINUTES);
            if (poll == null) {
                fetchDataExecutor.submit(() -> {
                    fetchData(true);
                });
                return;
            }

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                semaphore.release();
                throw e;
            }

            taskExecutor.execute(() -> {
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
                    semaphore.release();
                    lastFatchData.remove(poll);
                    fetchDataExecutor.submit(() -> {
                        fetchData(false);
                    });
                }
                if (monitorTaskInterface != null) monitorTaskInterface.monitor(montorTaskDto);
            });
            if (monitorRateDealInterface != null && monitorCacuRateInterface != null) {
                String monitorKey = monitorCacuRateInterface.getMonitorFormat();
                monitorRate(monitorKey);
            }


        } catch (Exception e) {

            log.error(e.getMessage());
            if (monitorStatusInterface != null) monitorStatusInterface.monitor(false, e);
        }
    }

    public synchronized void addMonitorRate(String key) {
        if (!monitorRates.containsKey(key)) {
            monitorRates.put(key, new AtomicInteger(0));
            if (monitorRateDealInterface != null && monitorCacuRateInterface != null)
                monitorAlarmExecutor.execute(() -> {
                    final AtomicInteger lastSize = monitorRates.values().size() > 1 ? (AtomicInteger) monitorRates.values().toArray()[monitorRates.values().size() - 2] : (AtomicInteger) monitorRates.values().toArray()[monitorRates.values().size() - 1];
                    monitorRateDealInterface.monitor(monitorRates, key, monitorCacuRateInterface.getRate(monitorRates), lastSize);
                });
        }
    }

    public void monitorRate(String key) {
        if (monitorRates.containsKey(key)) {
            monitorRates.get(key).incrementAndGet();
        } else {
            addMonitorRate(key);
            monitorRates.get(key).incrementAndGet();
        }

    }


}
