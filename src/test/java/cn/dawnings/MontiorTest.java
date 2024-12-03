package cn.dawnings;

import cn.dawnings.actuators.TaskActuator;
import cn.dawnings.config.CoreConfig;
import cn.dawnings.defaults.MonitorCacuRateFor5s;
import cn.dawnings.init.TaskActuatorBuilder;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.RandomUtil;
import org.junit.Test;

import java.util.ArrayList;
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
                .monitorTaskInterface((taskData) -> {
                    final String name = Thread.currentThread().getName();
                })
                .monitorRateDealInterface((monitorRates, key, count, lastSize) -> {
                    if (monitorRates.values().size() > 2) {
                        System.out.println("-----平均每分钟" + count + "个任务");
//                        final String name = Thread.currentThread().getName();
//                        System.out.println("mrdi:" + name);
//                        System.out.println("\n 1111 exit");
//                        TaskActuatorBuilder.taskActuatorMap.get(name).exitActuator();
                        latch.countDown();
                    }
//                    monitorRates.
                })
                .build();
        build.start();
        System.out.println("2");
//        Thread.sleep(1000*60*1);
        latch.await();
    }

    @Test
    public void test2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("start");

        CoreConfig coreConfig = new CoreConfig();
        coreConfig.setTaskLimitMax(100);
        TaskActuator<TaskData> build = TaskActuatorBuilder.<TaskData>builder()
                .threadCount(10)
                .taskLimitMax(5000)
                .batchLimitMin(100)
                .dataFetchInterface((lastList, fetchCount) -> {
                    List<TaskData> list = new ArrayList<>();
                    for (int i = 0; i < fetchCount; i++) {
                        TaskData taskData = new TaskData();
                        taskData.setId(new Snowflake().nextId());
                        list.add(taskData);
                    }
//                    final String name = Thread.currentThread().getName();
//                    System.out.println("fetchThread:" + name);
//                    System.out.println("\nfetchdata：" + list.size() + "条数据");
                    return list;
                })
                .taskRunnerInterface((taskData) -> {
//                    final String name = Thread.currentThread().getName();
//                    System.out.println("runt:" + name);
                    Thread.sleep(RandomUtil.randomInt(500, 3000));
//                    System.out.println(".");
                })
                .monitorTaskInterface((taskData) -> {
                    final String name = Thread.currentThread().getName();
                })
                .monitorCacuRateInterface(new MonitorCacuRateFor5s())
                .monitorRateDealInterface((monitorRates, key, count, lastSize) -> {
//                    if (monitorRates.values().size() > 1) {
                    System.out.println(key + "  -----平均5s" + count + "个任务，最近5s执行了" + lastSize + "个任务");
//                        final String name = Thread.currentThread().getName();
//                        System.out.println("mrdi:" + name);
//                        System.out.println("\n 1111 exit");
//                        TaskActuatorBuilder.taskActuatorMap.get(name).exitActuator();
                    if (monitorRates.values().size() > 10) {
                        latch.countDown();
                    }
//                    }
//                    monitorRates.
                })
                .build();
        build.start();
        System.out.println("2");
//        Thread.sleep(1000*60*1);
        latch.await();
    }


    @Test
    public void test3() {
        TaskActuatorBuilder<TaskData> builder = TaskActuatorBuilder.<TaskData>builder();
        builder.build();

    }
}
