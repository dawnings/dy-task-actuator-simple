package cn.dawnings.actuators;

import cn.dawnings.config.CoreConfig;
import cn.dawnings.coustoms.DataFetchSyncInterface;
import cn.dawnings.coustoms.RateLimitAcceptInterface;
import cn.dawnings.coustoms.TaskCallBackAsyncInterface;
import cn.dawnings.coustoms.TaskRunnerSyncInterface;
import cn.dawnings.dto.*;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.dawnings.monitor.CacuMonitorRateKeyInterface;
import cn.dawnings.monitor.MonitorDataFetchAsyncInterface;
import cn.dawnings.monitor.MonitorRateMsgAsyncInterface;
import cn.dawnings.util.LimitSizeMap;
import cn.dawnings.util.MutableSemaphore;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sun.reflect.Reflection;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public final class TaskActuator<T> {
	private Lock updateConfigLock;
	
	private Lock rateKeyLock;
	//监控数据
	private LimitSizeMap<String, AtomicInteger> monitorRates;
	//任务队列信号量
	private MutableSemaphore semaphoreForTask;
	//运行状态信号量
	private MutableSemaphore semaphoreForWait;
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
	private String taskName;
	@Setter
	@Getter
	private volatile Object customTag;
	//执行器自身所在的线程池，单线程定时从队列中获取任务，经过测试，低于20ms执行效率不会有提升，但会有显著的cpu消耗增加
	private ScheduledExecutorService taskActuatorsExecutor;
	//用于填充监控的线程池，定时获取令牌填充防止漏填充
	private ScheduledExecutorService fillMonitorRateExecutor;
	//真正的任务执行线程池
	private ThreadPoolExecutor coreTaskExecutor;
	//执行状态，标记为true后整个执行器终止，无法自动恢复
	private volatile boolean stop;
	//总体配置
	private volatile CoreConfig<T> configs;
	//用于发布任务的队列
	private BlockingQueue<T> replenishQueue;
	//数据获取接口
	private DataFetchSyncInterface<T> dataFetchSyncInterface;
	//任务执行接口
	private TaskRunnerSyncInterface<T> taskRunnerSyncInterface;
	//任务回调接口
	private TaskCallBackAsyncInterface<T> taskCallBacksyncInterface;
	//数据获取回调接口
	private MonitorDataFetchAsyncInterface<T> monitorDataFetchAsyncInterface;
	//监控速度计算接口
	private CacuMonitorRateKeyInterface cacuMonitorRateKeyInterface;
	//监控速率通知回调接口
	private MonitorRateMsgAsyncInterface monitorRateMsgAsyncInterface;
	//限流桶验证接口
	private RateLimitAcceptInterface rateLimitAcceptInterface;
	
	public Map<String, RateLimiter> getRateLimiters() {
		return configs.getRateLimiters();
	}
	
	public void clearRateLimiters() {
		configs.getRateLimiters().clear();
	}
	
	public void start() {
		if (configs == null) {
			throw new IllegalArgumentException("configs is null");
		}
		semaphoreForTask = new MutableSemaphore(configs.getThreadCount());
		semaphoreForWait = new MutableSemaphore(1);
		updateConfigLock = new ReentrantLock();
		Opt.ofNullable(cacuMonitorRateKeyInterface).ifPresent(a -> {
			rateKeyLock = new ReentrantLock();
		});
		if (dataFetchSyncInterface != null)
			fetchData();
		taskActuatorsExecutor.scheduleWithFixedDelay(this::consumeTaskData, configs.getInitDelay(), 20, TimeUnit.MILLISECONDS);
	}
	
	public boolean hadShutDown() {
		return coreTaskExecutor.isShutdown() && coreTaskExecutor.isTerminated();
	}
	
	public void fourceExitActuator() {
		stop = true;
		this.lastFatchData.clear();
		updateConfigLock.tryLock();
		updateConfigLock.unlock();
		if (rateKeyLock != null) {
			rateKeyLock.tryLock();
			rateKeyLock.unlock();
		}
		semaphoreForTask.deprecated();
		semaphoreForWait.deprecated();
		
		coreTaskExecutor.shutdownNow();
		taskActuatorsExecutor.shutdownNow();
		replenishQueue.clear();
		monitorRates.clear();
		if (fillMonitorRateExecutor != null) fillMonitorRateExecutor.shutdownNow();
	}
	
	public void exitActuator() {
		stop = true;
		this.lastFatchData.clear();
		updateConfigLock.tryLock();
		updateConfigLock.unlock();
		if (rateKeyLock != null) {
			rateKeyLock.tryLock();
			rateKeyLock.unlock();
		}
		semaphoreForTask.deprecated();
		semaphoreForWait.deprecated();
		
		coreTaskExecutor.shutdown();
		taskActuatorsExecutor.shutdown();
		replenishQueue.clear();
		monitorRates.clear();
		if (fillMonitorRateExecutor != null) fillMonitorRateExecutor.shutdown();
	}
	
	
	/**
	 * 初始化方法
	 * 禁止调用
	 *
	 * @param configs 初始化配置
	 */
	public void init(CoreConfig<T> configs) {
		Class<?> caller = Reflection.getCallerClass(2);
		if (caller != TaskActuatorBuilder.class) throw new SecurityException("Unsafe");
		this.configs = configs;
		dataFetchSyncInterface = configs.getDataFetchSyncInterface();
		taskRunnerSyncInterface = configs.getTaskRunnerSyncInterface();
		if (taskRunnerSyncInterface == null) throw new IllegalArgumentException("taskRunnerInterface is null");
		taskCallBacksyncInterface = configs.getTaskCallBackAsyncInterface();
		rateLimitAcceptInterface = configs.getRateLimitAcceptInterface();
		monitorDataFetchAsyncInterface = configs.getMonitorDataFetchAsyncInterface();
		cacuMonitorRateKeyInterface = configs.getCacuMonitorRateKeyInterface();
		monitorRateMsgAsyncInterface = configs.getMonitorRateMsgAsyncInterface();
		replenishQueue = new LinkedBlockingQueue<>(configs.getTaskLimitMax());
		taskName = configs.getTaskName();
		
		taskActuatorsExecutor = new ScheduledThreadPoolExecutor(1, (r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setName(taskName);
			t.setDaemon(false);
			return t;
		}));
		
		coreTaskExecutor = new ThreadPoolExecutor(configs.getThreadCount(), configs.getThreadCount(), 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(configs.getTaskLimitMax()), (r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setName(taskName + "-task-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
			t.setDaemon(false);
			return t;
		}));
		lastFatchData = new ArrayList<>(configs.getTaskLimitMax());
		monitorRates = new LimitSizeMap<>();
		monitorRates.setMaxSize(cacuMonitorRateKeyInterface.getMonitorSize());
		fillMonitorRateExecutor = Executors.newSingleThreadScheduledExecutor((r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setName(taskName + "-fillMonitorRate-" + RandomUtil.randomString(6) + RandomUtil.randomNumbers(4));
			t.setDaemon(false);
			return t;
		}));
		fillMonitorRateExecutor.scheduleWithFixedDelay(() -> {
			final String monitorFormat = cacuMonitorRateKeyInterface.getMonitorFormat();
			addMonitorRate(monitorFormat);
		}, 10, 500, TimeUnit.MICROSECONDS);
		
		
	}
	
	private void fetchData() {
		try {
			if (this.stop || configs.wait) {
				ThreadUtil.sleep(1000);
				return;
			}
			if (lastFetchTime != null) {
				long diff = ChronoUnit.SECONDS.between(lastFetchTime, LocalDateTime.now());
				if (diff < configs.getPollMinLimit()) return;
			}
			
			int fetchCount = replenishQueue.remainingCapacity();
			if (fetchCount < configs.getBatchLimitMin()) {
				if (monitorDataFetchAsyncInterface != null)
					ThreadUtil.execAsync(() -> monitorDataFetchAsyncInterface.monitorFetch(MonitorDataFetchDto.builder().taskName(taskName).status(-1).eplenishQueueRemain(fetchCount).build()));
				return;
			}
			
			try {
				Opt.ofNullable(monitorDataFetchAsyncInterface).ifPresent(a -> ThreadUtil.execAsync(() -> monitorDataFetchAsyncInterface.monitorFetch(MonitorDataFetchDto.builder().taskName(taskName).status(1).eplenishQueueRemain(fetchCount).build())));
				List<T> ts = dataFetchSyncInterface.didDataFetch(DataFetchDto.<T>builder()
						                                                 .fetchCount(fetchCount)
						                                                 .taskName(taskName)
						                                                 .lastFetchData(lastFatchData)
						                                                 .build());
				if (CollUtil.isEmpty(ts)) {
					lastFetchCount = 0;
					Opt.ofNullable(monitorDataFetchAsyncInterface).ifPresent(a -> ThreadUtil.execAsync(() -> monitorDataFetchAsyncInterface.monitorFetch(MonitorDataFetchDto.builder().taskName(taskName).status(-1).eplenishQueueRemain(fetchCount).build())));
					return;
				}
				lastFetchCount = ts.size();
				lastFatchData.addAll(ts);
				replenishQueue.addAll(ts);
				Opt.ofNullable(monitorDataFetchAsyncInterface).ifPresent(a -> ThreadUtil.execAsync(() -> monitorDataFetchAsyncInterface.monitorFetch(MonitorDataFetchDto.builder().taskName(taskName).status(2).eplenishQueueRemain(fetchCount).fetchedCount(lastFetchCount).build())));
			} catch (Exception e) {
				log.error(e.getMessage());
				Opt.ofNullable(monitorDataFetchAsyncInterface).ifPresent(a -> ThreadUtil.execAsync(() -> monitorDataFetchAsyncInterface.monitorFetch(MonitorDataFetchDto.builder().taskName(taskName).status(1).eplenishQueueRemain(fetchCount).e(e).build())));
			}
			lastFetchTime = LocalDateTime.now();
		} catch (Exception e) {
			Opt.ofNullable(monitorDataFetchAsyncInterface).ifPresent(a -> ThreadUtil.execAsync(() -> monitorDataFetchAsyncInterface.monitorFetch(MonitorDataFetchDto.builder().taskName(taskName).status(-1).e(e).build())));
			log.error(e.getMessage());
		}
	}
	
	
	/**
	 * 消费任务数据，发起任务，发起数据采集
	 */
	private void consumeTaskData() {
		if (stop) {
			log.warn("退出{}", this.taskName);
			return;
		}
		try {
			if (configs.wait) {
				System.out.println("暂停");
				log.warn("暂停{}", this.taskName);
				semaphoreForWait.acquire();
				return;
			}
			//利用poll的延迟时间实现超时(定时)更新
			final T poll = replenishQueue.poll(configs.getPollMaxLimit(), TimeUnit.SECONDS);
			if (poll == null && dataFetchSyncInterface != null) {
				fetchData();
				return;
			}
			pollTaskAndExecuteCall(poll);
			
		} catch (Throwable e) {
			log.error("unexpected error", e);
		}
	}
	
	/**
	 * 向队列添加一个任务
	 *
	 * @param taskData 任务数据
	 * @return 添加是否成功
	 */
	public boolean putTask(T taskData) {
		if (taskData == null) throw new IllegalArgumentException("taskData is null");
		return replenishQueue.offer(taskData);
	}
	
	/**
	 * 向队列添加一个任务（一直阻塞到成功添加任务）
	 *
	 * @param taskData 任务数据
	 * @throws InterruptedException 线程被中断
	 */
	public void putTaskBlock(T taskData) throws InterruptedException {
		if (taskData == null) throw new IllegalArgumentException("taskData is null");
		replenishQueue.put(taskData);
	}
	
	/**
	 * 直接发起任务，不经过队列
	 * <p>当执行器处于暂停、关闭状态下</p>
	 *
	 * @param taskData 任务数据
	 * @throws InterruptedException 线程被中断
	 */
	public void pollTaskAndExecuteCall(T taskData) throws InterruptedException, IllegalStateException {
		if (stop || configs.wait) throw new IllegalStateException("current status is stop");
		acquireTask();
		if (!coreTaskExecutor.isTerminated()) {
			coreTaskExecutor.execute(() -> {
				doMonitorAbleTask(taskData);
				if (!stop && dataFetchSyncInterface != null) ThreadUtil.execAsync(this::fetchData);
			});
		}
	}
	
	private void doMonitorAbleTask(T poll) {
		Exception ee = null;
		boolean status = false;
		try {
			if (CollUtil.isNotEmpty(configs.getRateLimiters())) {
				configs.getRateLimiters().forEach((key, limiter) -> {
					if (rateLimitAcceptInterface.accept(key)) {
						limiter.acquire();
					}
				});
			}
			taskRunnerSyncInterface.didTaskRunner(TaskRunnerDto.<T>builder().taskName(taskName).taskData(poll).build());
			status = true;
		} catch (Exception e) {
			ee = e;
		} finally {
			if (!semaphoreForTask.isDeprecated())
				semaphoreForTask.release();
			lastFatchData.remove(poll);
		}
		
		doTaskCallBackAndMonitor(status, ee, poll);
	}
	
	private void doTaskCallBackAndMonitor(boolean status, Exception ee, T poll) {
		Opt.ofNullable(taskCallBacksyncInterface).ifPresent(a -> ThreadUtil.execAsync(() -> taskCallBacksyncInterface.callback(TaskCallBackDto.<T>builder().taskName(taskName).taskData(poll).status(status).exception(ee).build())));
		Opt.ofNullable(monitorRateMsgAsyncInterface).ifPresent(a -> ThreadUtil.execAsync(() -> monitorRate(cacuMonitorRateKeyInterface.getMonitorFormat())));
	}
	
	private void acquireTask() throws InterruptedException {
		try {
			semaphoreForTask.acquire();
		} catch (InterruptedException e) {
			semaphoreForTask.release();
			throw e;
		}
	}
	
	private void addMonitorRate(String key) {
		rateKeyLock.lock();
		try {
			if (!monitorRates.containsKey(key)) {
				monitorRates.put(key, new AtomicInteger(0));
				Opt.ofNullable(monitorRateMsgAsyncInterface).ifPresent(a -> {
					if (monitorRates.size() == 0) return;
					final AtomicInteger lastSize = (AtomicInteger) monitorRates.values().toArray()[monitorRates.size() - 2];
					ThreadUtil.execAsync(() -> monitorRateMsgAsyncInterface.monitorRate(
							MontorRateMsgDto.builder().rate(cacuMonitorRateKeyInterface.getRate(monitorRates)).key(key).taskName(taskName).lastSize(lastSize.get()).build()
					));
				});
			}
		} finally {
			rateKeyLock.unlock();
		}
		
	}
	
	private void monitorRate(String key) {
		if (!monitorRates.containsKey(key)) {
			addMonitorRate(key);
		}
		monitorRates.get(key).incrementAndGet();
		
	}
	
	/**
	 * 暂停
	 */
	public void waiting() {
		configs.wait = true;
	}
	
	/**
	 * 恢复
	 */
	public void resume() {
		updateConfigLock.lock();
		try {
			if (configs.wait) {
				configs.wait = false;
				semaphoreForWait.release();
			}
		} finally {
			updateConfigLock.unlock();
		}
	}
	
	/**
	 * 更新核心线程数量
	 *
	 * @param threadCount 核心线程数量
	 */
	public void updateThreadCount(int threadCount) {
		if (threadCount <= 1) {
			throw new IllegalArgumentException("threadCount should be greater than 1. ");
		}
		updateConfigLock.lock();
		try {
			this.coreTaskExecutor.setCorePoolSize(threadCount);
			this.coreTaskExecutor.setMaximumPoolSize(threadCount);
			semaphoreForTask.updateMaxPermits(threadCount);
		} finally {
			updateConfigLock.unlock();
		}
		
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
	
	public boolean isStop() {
		return stop;
	}
	
	public boolean isWaiting() {
		return configs.wait;
	}
	
	/**
	 * 获取运行状态与参数
	 * return 运行状态
	 */
	public ThreadPoolStatus getTaskPoolStatus() {
		ThreadPoolStatus threadPoolStatus = new ThreadPoolStatus();
		threadPoolStatus.setTaskName(taskName);
		threadPoolStatus.setAvailablePermits(this.semaphoreForTask.getAvailablePermits());
		threadPoolStatus.setCorePoolSize(coreTaskExecutor.getCorePoolSize());
		threadPoolStatus.setMaximumPoolSize(coreTaskExecutor.getMaximumPoolSize());
		threadPoolStatus.setActiveCount(coreTaskExecutor.getActiveCount());
		threadPoolStatus.setQueueRemainingCapacity(this.replenishQueue.remainingCapacity());
		threadPoolStatus.setThreadQueueSize(this.coreTaskExecutor.getQueue().size());
		threadPoolStatus.setQueueSize(this.replenishQueue.size());
		threadPoolStatus.setQueueMax(configs.getTaskLimitMax());
		threadPoolStatus.setBatchMinLimit(configs.getBatchLimitMin());
		threadPoolStatus.setPollWaitMin(configs.getPollMinLimit());
		threadPoolStatus.setPollWaitMax(configs.getPollMaxLimit());
		threadPoolStatus.setLastFetchCount(this.getLastFetchCount());
		threadPoolStatus.setLastFetchTime(this.getLastFetchTime());
		threadPoolStatus.setStopping(stop);
		threadPoolStatus.setWaiting(configs.wait);
		threadPoolStatus.setThreadPoolStopped(this.taskActuatorsExecutor.isShutdown() && taskActuatorsExecutor.isTerminated());
		threadPoolStatus.setTaskActuatorsExecutorStopped(this.coreTaskExecutor.isShutdown() && coreTaskExecutor.isTerminated());
		threadPoolStatus.setFetchDataCount(this.getLastFatchData().size());
		threadPoolStatus.setFillMonitorRateExecutorStopped(this.fillMonitorRateExecutor.isShutdown() && fillMonitorRateExecutor.isTerminated());
		return threadPoolStatus;
	}
	
	
}
