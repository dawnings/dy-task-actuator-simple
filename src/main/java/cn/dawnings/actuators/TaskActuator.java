package cn.dawnings.actuators;

import cn.dawnings.config.CoreConfig;
import cn.dawnings.coustoms.DataFetchInterface;
import cn.dawnings.coustoms.MonitorRateDealInterface;
import cn.dawnings.coustoms.TaskCallBackInterface;
import cn.dawnings.coustoms.TaskRunnerInterface;
import cn.dawnings.dto.MontorTaskDto;
import cn.dawnings.dto.ThreadPoolStatus;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.dawnings.monitor.MonitorCacuRateInterface;
import cn.dawnings.monitor.MonitorDataFetchInterface;
import cn.dawnings.monitor.MonitorStatusInterface;
import cn.dawnings.monitor.MonitorTaskInterface;
import cn.dawnings.util.LimitSizeMap;
import cn.hutool.core.util.RandomUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sun.reflect.Reflection;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public final class TaskActuator<T> {
    private Lock updateConfigLock;

    private Lock rateAlarmLock;
    //监控数据
    private LimitSizeMap<String, AtomicInteger> monitorRates;
    //任务队列信号量
    private Semaphore semaphoreForTask;
    //运行状态信号量
    private Semaphore semaphoreForWait;
    //最后一次数据获取时间
    @Getter
    private volatile LocalDateTime lastFetchTime;
    //最后一次数据获取的数量
    @Getter
    private volatile int lastFetchCount;
    //用于存储上一次数据填充任务的返回结果，以便后续fetch数据时避让
    @Getter
    private List<T> lastFatchData;
    //执行器线程的名称
    @Getter
    private String threadName;


    @Setter
    @Getter
    private volatile Object customTag;
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
    private volatile boolean stop;
    //总体配置
    private volatile CoreConfig<T> configs;
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
            throw new IllegalArgumentException("configs is null");
        }
        fetchData();

        semaphoreForTask = new Semaphore(configs.getTaskLimitMax());
        semaphoreForWait = new Semaphore(1);

        updateConfigLock = new ReentrantLock();
        if (monitorRateDealInterface != null && monitorCacuRateInterface != null) {
            rateAlarmLock = new ReentrantLock();
        }

        taskActuatorsExecutor.scheduleWithFixedDelay(this::execute, configs.getInitDelay(), 20, TimeUnit.MILLISECONDS);
    }

    public boolean hadShutDown() {
        return taskExecutor.isShutdown() && taskExecutor.isTerminated();
    }

    @SneakyThrows
    public void fourceExitActuator() {
        stop = true;
        updateConfigLock.unlock();
        if (rateAlarmLock != null)
            rateAlarmLock.unlock();
        semaphoreForTask.release(configs.getTaskLimitMax() + 1);
        semaphoreForWait.release(2);
        fetchDataExecutor.shutdownNow();
        taskExecutor.shutdownNow();
        taskActuatorsExecutor.shutdownNow();

        if (fillMonitorRateExecutor != null)
            fillMonitorRateExecutor.shutdownNow();
        if (monitorAlarmExecutor != null)
            monitorAlarmExecutor.shutdownNow();
        monitorRates.clear();
        monitorRates=null;
    }

    @SneakyThrows
    public void exitActuator() {
        stop = true;
        updateConfigLock.unlock();
        if (rateAlarmLock != null)
            rateAlarmLock.unlock();
        semaphoreForTask.release(configs.getTaskLimitMax() + 1);
        semaphoreForWait.release(2);
        fetchDataExecutor.shutdown();
        taskExecutor.shutdown();
        taskActuatorsExecutor.shutdown();

        if (fillMonitorRateExecutor != null)
            fillMonitorRateExecutor.shutdown();
        if (monitorAlarmExecutor != null)
            monitorAlarmExecutor.shutdown();
    }


    /**
     * 初始化方法
     * 禁止调用
     *
     * @param configs 初始化配置
     */
    public void init(CoreConfig<T> configs) {
        Class<?> caller = Reflection.getCallerClass(2);
        if (caller != TaskActuatorBuilder.class)
            throw new SecurityException("Unsafe");
        this.configs = configs;
        dataFetchInterface = configs.getDataFetchInterface();
        if (dataFetchInterface == null) throw new IllegalArgumentException("dataFetchInterface is null");
        taskRunnerInterface = configs.getTaskRunnerInterface();
        if (taskRunnerInterface == null) throw new IllegalArgumentException("taskRunnerInterface is null");
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
        taskExecutor = new ThreadPoolExecutor(configs.getThreadCount(), configs.getThreadCount(), 30L, TimeUnit.SECONDS, workingQueue, (r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(threadName + "-task-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
            t.setDaemon(false);
            return t;
        }));
        fetchDataExecutor = new ThreadPoolExecutor(1, 1, configs.getPollMaxLimit(), TimeUnit.MINUTES, new LinkedBlockingDeque<>(1), (r -> {
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
            monitorAlarmExecutor = new ThreadPoolExecutor(1, 2, configs.getPollMaxLimit(), TimeUnit.MINUTES, new LinkedBlockingDeque<>(10), (r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName(threadName + "-monitorAlarm-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
                t.setDaemon(false);
                return t;
            }));
            monitorAlarmExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        }


    }

    @SneakyThrows
    private void fetchData() {
        if (this.stop || configs.manualWait) {
            Thread.sleep(1000);
            return;
        }
        if (lastFetchTime != null) {
            long diff = ChronoUnit.SECONDS.between(lastFetchTime, LocalDateTime.now());
            if (diff < configs.getPollMinLimit()) return;
        }

        int fullCa = taskExecutor.getQueue().remainingCapacity() - taskExecutor.getQueue().size();
        int waitCa = replenishQueue.remainingCapacity();
        if (waitCa < configs.getBatchLimitMin() || fullCa < configs.getBatchLimitMin()) {
            if (monitorDataFetchInterface != null) monitorDataFetchInterface.monitor(-1, fullCa, waitCa, 0, 0, null);
            return;
        }
        lastFetchCount = Math.min(fullCa, waitCa);
        try {
            if (monitorDataFetchInterface != null)
                monitorDataFetchInterface.monitor(1, fullCa, waitCa, lastFetchCount, 0, null);
            List<T> ts = dataFetchInterface.didDataFetch(lastFatchData, lastFetchCount);
            if (ts == null || ts.isEmpty()) {
                if (monitorDataFetchInterface != null)
                    monitorDataFetchInterface.monitor(-1, fullCa, waitCa, lastFetchCount, 0, null);
                return;
            }
            lastFatchData.addAll(ts);
            replenishQueue.addAll(ts);
            //释放一个list，某些jdk这个list对象有可能释放不掉
            ts = null;
            if (monitorDataFetchInterface != null)
                monitorDataFetchInterface.monitor(2, fullCa, waitCa, lastFetchCount, lastFatchData.size(), null);
        } catch (Exception e) {
            log.error(e.getMessage());
            if (monitorDataFetchInterface != null)
                monitorDataFetchInterface.monitor(-1, fullCa, waitCa, lastFetchCount, 0, e);
        }
        lastFetchTime = LocalDateTime.now();
    }


    private void execute() {
        if (stop) return;
        try {
            //因其他原因产生的停机
            if (configs.manualWait) {
                semaphoreForWait.acquire();
                return;
            }
            //利用poll的延迟时间实现超时(定时)更新
            final T poll = replenishQueue.poll(configs.getPollMaxLimit(), TimeUnit.MINUTES);
            if (poll == null) {
                fetchDataExecutor.submit(this::fetchData);
                return;
            }
            try {
                semaphoreForTask.acquire();
            } catch (InterruptedException e) {
                semaphoreForTask.release();
                throw e;
            }
            if (!taskExecutor.isTerminated()) {
                taskExecutor.execute(() -> {
                    MontorTaskDto<T> montorTaskDto = new MontorTaskDto<>();
                    montorTaskDto.setTaskData(poll);
                    try {
                        taskRunnerInterface.didTaskRunner(poll);
                        montorTaskDto.setTaskStatus(true);
                        if (taskCallBackInterface != null) taskCallBackInterface.callbackSuccess(poll);
                    } catch (Exception e) {
                        try {
                            if (taskCallBackInterface != null) taskCallBackInterface.callbackError(poll, e);
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                        }
                        montorTaskDto.setException(e);
                    } finally {
                        semaphoreForTask.release();
                        lastFatchData.remove(poll);
                    }
                    if (!stop)
                        fetchDataExecutor.submit(this::fetchData);
                    try {
                        if (monitorTaskInterface != null) monitorTaskInterface.monitor(montorTaskDto);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                    if (monitorRateDealInterface != null && monitorCacuRateInterface != null) {
                        monitorRate(monitorCacuRateInterface.getMonitorFormat());
                    }
                });
            }

        } catch (Throwable e) {
            if (monitorStatusInterface != null) monitorStatusInterface.monitor(false, e);
            else {
                log.error(e.getMessage());
            }
        }
    }

    private void addMonitorRate(String key) {
        rateAlarmLock.lock();
        try {
            if (!monitorRates.containsKey(key)) {
                monitorRates.put(key, new AtomicInteger(0));
                if (monitorRateDealInterface != null && monitorCacuRateInterface != null)
                    monitorAlarmExecutor.execute(() -> {
                        final AtomicInteger lastSize = monitorRates.values().size() > 1 ? (AtomicInteger) monitorRates.values().toArray()[monitorRates.values().size() - 2] : (AtomicInteger) monitorRates.values().toArray()[monitorRates.values().size() - 1];
                        monitorRateDealInterface.monitor(monitorRates, key, monitorCacuRateInterface.getRate(monitorRates), lastSize);
                    });
            }
        } finally {
            rateAlarmLock.unlock();
        }

    }

    private void monitorRate(String key) {
        if (monitorRates.containsKey(key)) {
            monitorRates.get(key).incrementAndGet();
        } else {
            addMonitorRate(key);
            monitorRates.get(key).incrementAndGet();
        }

    }

    /**
     * 暂停
     */
    public void waiting() {
        configs.manualWait = true;
    }

    /**
     * 恢复
     */
    public void resume() {
        updateConfigLock.lock();
        try {
            if (configs.manualWait) {
                configs.manualWait = false;
                semaphoreForWait.release();
            }
        } finally {
            updateConfigLock.unlock();
        }
    }

    /**
     * 更新核心线程数量
     */
    public void updateThreadCount(int threadCount) {
        this.configs.setThreadCount(threadCount);
    }

    /**
     * 获取监控表
     */
    public LinkedHashMap<String, Integer> getMonitorRates() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        monitorRates.forEach((k, v) -> {
            map.put(k, v.get());
        });
        return map;
    }

    /**
     * 获取运行状态与参数
     */
    public ThreadPoolStatus getTaskPoolStatus() {
        ThreadPoolStatus threadPoolStatus = new ThreadPoolStatus();
        threadPoolStatus.setThreadName(threadName);
        threadPoolStatus.setCorePoolSize(taskExecutor.getCorePoolSize());
        threadPoolStatus.setActiveCount(taskExecutor.getActiveCount());
        threadPoolStatus.setQueueMaxRemainingCapacity(taskExecutor.getQueue().remainingCapacity());
        threadPoolStatus.setQueueSize(taskExecutor.getQueue().size());
        threadPoolStatus.setQueueMax(configs.getTaskLimitMax());
        threadPoolStatus.setBatchMinLimit(configs.getBatchLimitMin());
        threadPoolStatus.setPollWaitMin(configs.getPollMinLimit());
        threadPoolStatus.setPollWaitMax(configs.getPollMaxLimit());
        threadPoolStatus.setLastFetchCount(this.getLastFetchCount());
        threadPoolStatus.setLastFetchTime(this.getLastFetchTime());
        return threadPoolStatus;
    }


}
