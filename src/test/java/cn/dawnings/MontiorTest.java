package cn.dawnings;

import cn.dawnings.actuators.TaskActuator;
import cn.dawnings.config.CoreConfig;
import cn.dawnings.defaults.CacuMonitorRateKeyFor5S;
import cn.dawnings.dto.ThreadPoolStatus;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.RandomUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MontiorTest {


    @Test
    public void test1() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("start");


        TaskActuator<TaskData> build = TaskActuatorBuilder.<TaskData>builder(new CoreConfig<>())
                .dataFetchInterface((lastList, fetchCount) -> {
                    List<TaskData> list = new ArrayList<>();
                    for (int i = 0; i < fetchCount; i++) {
                        TaskData taskData = new TaskData();
                        taskData.setId(new Snowflake().nextId());
                        list.add(taskData);
                    }
                    final String name = Thread.currentThread().getName();
//                    System.out.println("fetchThread:" + name);
//                    System.out.println("\nfetchdata：" + list.size() + "条数据");
                    return list;
                })
                .taskRunnerInterface((taskData) -> {
                    final String name = Thread.currentThread().getName();
//                    System.out.println("runt:" + name);
                    Thread.sleep(RandomUtil.randomInt(1000, 4000));
                    System.out.print(".");
                })
                .monitorRateDealInterface((m) -> {
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
                .dataFetchInterface((lastList, fetchCount) -> {
                    List<TaskData> list = new ArrayList<>();
                    for (int i = 0; i < fetchCount; i++) {
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
                .taskRunnerInterface((taskData) -> {
                    Thread.sleep(RandomUtil.randomInt(500, 3000));
                })
                .monitorCacuRateInterface(new CacuMonitorRateKeyFor5S())
                .monitorRateDealInterface((m) -> {
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
        TaskActuatorBuilder<TaskData> builder = TaskActuatorBuilder.<TaskData>builder();
        builder.build();

    }
}
