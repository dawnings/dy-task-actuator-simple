package cn.dawnings;

import cn.dawnings.actuators.TaskActuator;
import cn.dawnings.config.CoreConfig;
import cn.dawnings.defaults.CacuMonitorRateKeyFor1M;
import cn.dawnings.defaults.CacuMonitorRateKeyFor5S;
import cn.dawnings.dto.ThreadPoolStatus;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.RandomUtil;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Data;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MontiorTest {
    static TaskActuator<FileSendDataDto> fileTaskActuator = null;

    @Data
    public static class FileSendDataDto {
        private Long id;
        private String filePath;
        private String fileName;
        private Integer fileType;
        private String projectCode;
        private String supplierName;
        private String bidName;
        private String packageName;
        private String bigClass;
        private String smallClass;

        private Integer errorCount;
        /**
         * 0表示未开始
         */
        private int fullLength;

        private double rate;
        //用于计算进度百分比
        private int successLength;
    }

    @Test
    public void test5() throws InterruptedException {
        CoreConfig<FileSendDataDto> coreConfig = new CoreConfig<>();

        fileTaskActuator = TaskActuatorBuilder.builder(coreConfig)
                .initDelay(5000)
                .threadCount(20)
                .taskLimitMax(500)
                .batchLimitMin(150)
                .pollMinLimit(15)
                .pollMaxLimit(120)
                .dataFetchSyncInterface((a) -> {
                    return new ArrayList<>();
                })
                .taskRunnerSyncInterface((a) -> {
                    return;
                })
                .cacuMonitorRateKeyInterface(new CacuMonitorRateKeyFor1M())
                .monitorRateMsgAsyncInterface((m) -> {
                    System.out.println(m.key() + "  -----平均1min" + m.rate() + "个任务，最近1min执行了" + m.lastSize() + "个任务");
                })
                .build();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
//                CheckAliveDto checkAliveDto = innerfsInterface.checkAlive();
//                boolean isDangerous = checkAliveDto.getDangerous();
//                log.info("内网服务器状态：{}",checkAliveDto);
                // 确保 getDangerous() 返回值是可信的
//                if (isDangerous) {
//                    fileTaskActuator.waiting();
//                } else if (coreConfig.wait) {
//                    fileTaskActuator.resume();
//                }
//                System.out.println("检测ing");
                fileTaskActuator.waiting();
//                System.out.println("检测end");
            } catch (Exception e) {
//                log.error("checkAliveError,推送暂停:", e);
//                if (!coreConfig.wait) fileTaskActuator.waiting();
            }

        }, 2, 2,TimeUnit.SECONDS);

        fileTaskActuator.start();
//        CoreConfig<FileSendDataDto> coreConfig = new CoreConfig<>();
//        fileTaskActuator = TaskActuatorBuilder.builder(coreConfig)
//                .initDelay(1000)
//                .threadCount(15)
//                .taskLimitMax(500)
//                .batchLimitMin(200)
//                .pollMinLimit(5)
//                .pollMaxLimit(8)
//                .dataFetchSyncInterface((x)->{
//                    List<FileSendDataDto> data = new ArrayList<>();
//                    data.add(new FileSendDataDto());
//                    return data;
//                })
//                .taskRunnerSyncInterface((a)->{
//                    System.out.println("run");
//                })
//                .cacuMonitorRateKeyInterface(new CacuMonitorRateKeyFor1M())
//                .monitorRateMsgAsyncInterface((m) -> {
//                    System.out.println(m.key() + "  -----平均1min" + m.rate() + "个任务，最近1min执行了" + m.lastSize() + "个任务");
//                })
//                .build();
//        fileTaskActuator.start();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }


    @Test
    public void test1() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("start");


        TaskActuator<TaskData> build = TaskActuatorBuilder.<TaskData>builder(new CoreConfig<>())
                .dataFetchSyncInterface((f) -> {
                    List<TaskData> list = new ArrayList<>();
                    for (int i = 0; i < f.fetchCount(); i++) {
                        TaskData taskData = new TaskData();
                        taskData.setId(new Snowflake().nextId());
                        list.add(taskData);
                    }
                    final String name = Thread.currentThread().getName();
//                    System.out.println("fetchThread:" + name);
//                    System.out.println("\nfetchdata：" + list.size() + "条数据");
                    return list;
                })
                .taskRunnerSyncInterface((taskData) -> {
                    final String name = Thread.currentThread().getName();
//                    System.out.println("runt:" + name);
                    try {
                        Thread.sleep(RandomUtil.randomInt(1000, 4000));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.print(".");
                })
                .monitorRateMsgAsyncInterface((m) -> {
                    System.out.println("-----平均每分钟" + m.rate() + "个任务");
//                        final String name = Thread.currentThread().getName();
//                        System.out.println("mrdi:" + name);
//                        System.out.println("\n 1111 exit");
//                        TaskActuatorBuilder.taskActuatorMap.get(name).exitActuator();
                    latch.countDown();
//                    monitorRates.
                })
                .build();
        build.start();

//        Thread.sleep(1000*60*1);
        latch.await();
    }

    @Test
    public void test2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        TaskActuator<TaskData> build = TaskActuatorBuilder.<TaskData>builder()
                .threadCount(10)
                .taskLimitMax(800)
                .batchLimitMin(200)
                .pollMinLimit(2)
                .dataFetchSyncInterface((f) -> {
                    List<TaskData> list = new ArrayList<>();
                    for (int i = 0; i < f.fetchCount(); i++) {
                        TaskData taskData = new TaskData();
//                        taskData.setId(new Snowflake().nextId());
                        list.add(taskData);
                    }
                    return list;
                })
//                .monitorDataFetchInterface((m) -> {
//                    if (m.status() == 2) {
////                        System.out.println(m.status() + " " + m.fetchedCount() + "   " + m.taskName());
//                    }
//                })
                .taskRunnerSyncInterface((taskData) -> {
                    try {
                        Thread.sleep(RandomUtil.randomInt(500, 3000));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .cacuMonitorRateKeyInterface(new CacuMonitorRateKeyFor5S())
                .monitorRateMsgAsyncInterface((m) -> {
//                    System.out.println(m.key() + "  -----平均5s" + m.rate() + "个任务，最近5s执行了" + m.lastSize() + "个任务");
                    final TaskActuator<?> taskActuator = TaskActuatorBuilder.taskActuatorMap.get(m.taskName());
                    final LinkedHashMap<String, Integer> monitorRates = taskActuator.getMonitorRates();
                    if (monitorRates.keySet().size() == 3) {
                        taskActuator.updateThreadCount(15);
                    }
                    if (monitorRates.keySet().size() == 5) {
                        taskActuator.updateThreadCount(18);
                    }
                    if (monitorRates.keySet().size() == 7) {
                        taskActuator.updateThreadCount(21);
                    }
                    if (monitorRates.keySet().size() == 9) {
                        taskActuator.updateThreadCount(23);
                    }
                    if (monitorRates.keySet().size() == 11) {
                        taskActuator.updateThreadCount(25);
                    }
//                    if (monitorRates.values().size() > 8) {
//                        taskActuator.exitActuator();
////                        latch.countDown();
//                    }
                })
                .build();
        build.start();

        new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                try {
                    Thread.sleep(1000 * 2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                final ThreadPoolStatus taskPoolStatus = build.getTaskPoolStatus();
                System.out.println(
                        "!t_active:" + taskPoolStatus.getActiveCount()
                                + "; !t_max:" + taskPoolStatus.getMaximumPoolSize()
//                                + "; t_q_s:" + taskPoolStatus.getThreadQueueSize()
                                + "; !t_p_a:" + taskPoolStatus.getAvailablePermits()
//                                + "; q_size: " + taskPoolStatus.getQueueSize()
//                                + "; l_f_c:" + taskPoolStatus.getLastFetchCount()
//                                + "; l_f_t:" + DateUtil.format(taskPoolStatus.getLastFetchTime(), "HH:mm:ss")
                                + "; s.ing:" + taskPoolStatus.isStopping()
                                + "; s.ed:" + taskPoolStatus.isStopped()
                );
            }
//            latch.countDown();
        }).start();
        latch.await();
    }


    @Test
    public void test3() {
        //使用构造器
        TaskActuatorBuilder<TaskData> builder = TaskActuatorBuilder.<TaskData>builder();
        //配置参数
        builder.initDelay(1000)
                .pollMinLimit(60)
                .pollMinLimit(10)
                .threadCount(10)
                .taskLimitMax(100)
                .taskName("yourTasName")
                .taskRunnerSyncInterface((taskData) -> {

                })
        ;
        builder.taskName("yourTasName");

//时级限流-每小时100个
        builder.addRateLimiter("hour", RateLimiter.create((double) 100 / 60 / 60));
//分级限流-每分钟50个
        builder.addRateLimiter("min", RateLimiter.create((double) 50 / 60));
//秒级限流-每秒钟10个
        builder.addRateLimiter("sec", RateLimiter.create(10));


        LinkedHashMap<String, RateLimiter> rateLimiters = new LinkedHashMap<>();
        rateLimiters.put("hour", RateLimiter.create((double) 100 / 60 / 60));
        rateLimiters.put("min", RateLimiter.create((double) 50 / 60));
        rateLimiters.put("sec", RateLimiter.create(10));
        builder.rateLimiters(rateLimiters);
        //构建执行器
        TaskActuator<TaskData> taskActuator = builder.build();
        //启动执行器
        taskActuator.start();
    }
}
