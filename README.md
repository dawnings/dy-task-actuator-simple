---
title: 待遇任务执行器（dy-task-actuator-simple）文档
date: 2024-12-05T12:42:27Z
lastmod: 2024-12-05T14:39:38Z
tags: [Java,dy-task-actuator-simple]
---

# 待遇任务执行器（dy-task-actuator-simple）文档

# 简介

简称 dtas 吧。这是一个尚在起步但无需太多功能的执行器。

心血来潮，做了一个任务执行器，倒不是一定要重复造轮子，而是没有发现开箱即用的任务执行器。

​`这不是一个特别高性能的任务调度框架!只是一个任务执行器。唯一的优点就是简单。`​

设计时希望 dtas 能够简单调控、限制资源使用、增大资源利用率、实现简易监控。为此采用了多级线程池、阻塞队列、信号量、令牌桶。

dtas 适用于执行大量的 cpu 密集型、io 密集型、网络请求任务，也可用作并发的异步任务控制。

# 联系

我会在每周六下午检查邮箱、留言和 issue

1. 给我发送邮件： 17625901395@163.com
2. 在博客园给我留言：[https://www.cnblogs.com/dawnings/p/18588469/treatment-task-actuator-dytaskactuatorsimple-document-zka1bj](https://www.cnblogs.com/dawnings/p/18588469/treatment-task-actuator-dytaskactuatorsimple-document-zka1bj)
3. 在 github 给我 issue：[https://github.com/dawnings/dy-task-actuator-simple](https://github.com/dawnings/dy-task-actuator-simple)

# 支持

1. 您可在博客园给我打赏。
2. 作者时间并不充裕，故暂不支持pr，您可在issue中提供支持。

# 安装

github 上 main 分支代码就是最新可用的完整代码，如有三方引用会在文档标注。

# 依赖

dtas 通过 `hutool`​ 和 `guava`​ 减少造轮子。

所有依赖如下：

|`artifactId`​|`groupId`​|`version`​|`scope`​|
| -------------------| -------------------| -------------------| -------------------|
|​`lombok`​|​`org.projectlombok`​|​`1.18.26`​|​`provided`​|
|​`slf4j-api`​|​`org.slf4j`​|​`2.0.16`​|​`provided`​|
|​`hutool-all`​|​`cn.hutool`​|​`5.8.34`​|​`compile`​|
|​`guava`​|​`com.google.guava`​|​`33.3.1-jre`​|​`compile`​|
|​`junit`​|​`junit`​|​`4.13.1`​|​`test`​|

项目构建环境如下：

|name|version|
| ---------------------| ------------------------|
|​`jdk`​|​`8`​|
|​`maven`​|​`3.6.3`​<br />|

# 功能清单

#### builder 构建工具

* [X] 配置任务名称
* [X] 通过任务名称获取任务执行器
* [X] 设定自定义通讯标记
* [X] 设定启动延迟时间
* [X] 设定限流器
* [X] 设定数据采集数量最低门限
* [X] 设定数据采集数量最高门限（任务队列极限）
* [X] 设定数据采集间隔最低门限
* [X] 设定数据采集间隔最高门限
* [X] 设定并发线程最高数量
* [X] 设定数据采集执行接口
* [X] 设定任务消费执行接口
* [X] 设定任务执行回调接口
* [X] 设定消费速率监控接口
* [X] 设定消费速率 X 轴生成器
* [X] 设定消费速率 X 轴生成通知接口
* [X] 设定限流器验证接口

#### 执行器

* [X] 添加任务到等待队列
* [X] 添加任务到等待队列（阻塞）
* [X] 添加任务到执行队列
* [X] 终止执行器
* [X] 暂定执行器
* [X] 恢复执行器
* [X] 调整最大并发线程
* [X] 获取消费速率表
* [X] 获取运行状态参数

#### 预实现接口

###### 消费速率 X 轴生成器

* [X] 每 5s 生成
* [X] 每 1 分钟生成
* [X] 每 5 分钟生成
* [X] 10 分钟生成

###### 限流验证器

* [X] 默认通过验证器

#### 工具

* [X] 可动态限制大小的 LinkedHashMap
* [X] 可动态调控数量的信号量

#### 集成

* [ ] springboot-starter集成
