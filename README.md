# 任务执行器

  这不是一个特别高性能的任务执行框架!
  相比各类简单/复杂的任务调度框架，这只是一个任务执行器。唯一的优点就是简单。
  设计他的主要目的，是做大量任务时可以简单的占满资源，并且可以通过比较简单的方式调控，能够做到简单的监控。
  基本模型，核心线程池负责执行任务，阻塞队列负责填充任务队列，任务源源不断的放入队列，然后又被核心线程池消费。如此可以保证任务数量限流，任务执行不间断，核心线程池的大小根据需要进行调整，从而达到资源利用率的最大化。

  优点：任务执行效率高，任务数量限流，任务执行不间断，资源利用率最大化。
  缺点：目前还非常简陋，只完成了基本功能。即便后期完善了，依旧不可能代替任务调度。

  他可能的使用场景：
  - 长期的文件传输任务（或类似的高io任务）
  - 长期的计算任务（或类似的高cpu任务）
  - 限流的三方调用任务

  后期准备要做的功能：
  - 与springboot集成（这会新开一个starter项目）
  - 任务并发数自动计算（基于io和cpu负载）
  - 提供任务执行探针接口，减少无用消耗
  - 提供完善的状态查询接口
  - 更新文档
  - 更多的监控机制
    
  后期文档更新可能先更新到 http://dawnings.cn 上。

- 使用方式
    ```java
    @Test
    public void test3() {
        TaskActuatorBuilder<TaskData> builder = TaskActuatorBuilder.<TaskData>builder();
    //    builder.dataFetchInterface(...)
    //            .taskRunnerInterface(...)
        // 其他配置...;
        builder.build();
    }
    
    ```
    其中，dataFetchInterface 是数据获取接口实例，taskRunnerInterface 是任务执行实例。 此二必不可少，
    详细接口文档后期后空会补充。

- 支持基于时间令牌的任务执行率
```txt
2024-11-29-23-36-10  -----平均5s39.50个任务，最近5s执行了79个任务
2024-11-29-23-36-15  -----平均5s86.00个任务，最近5s执行了179个任务
2024-11-29-23-36-20  -----平均5s66.00个任务，最近5s执行了6个任务
2024-11-29-23-36-25  -----平均5s52.80个任务，最近5s执行了0个任务
2024-11-29-23-36-30  -----平均5s44.00个任务，最近5s执行了0个任务
2024-11-29-23-36-35  -----平均5s53.71个任务，最近5s执行了112个任务
2024-11-29-23-36-40  -----平均5s48.88个任务，最近5s执行了15个任务
2024-11-29-23-36-45  -----平均5s43.44个任务，最近5s执行了0个任务
2024-11-29-23-36-50  -----平均5s39.10个任务，最近5s执行了0个任务
2024-11-29-23-36-55  -----平均5s45.36个任务，最近5s执行了108个任务
```
- 依赖
必须依赖 `slf4j-api >=2.0.16` 和 `hutool-all >=5.8.26`