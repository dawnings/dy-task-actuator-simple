package cn.dawnings.monitor;

import cn.dawnings.dto.MonitorDataFetchDto;

public interface MonitorDataFetchAsyncInterface<T> {

    /**
     * 镜像方法，不应该执行长时间任务
     *
     * @param status              -1:error; 1: fetch start;2: fetch end
     * @param executeQueueRemain  工作队列的空闲数
     * @param eplenishQueueRemain 数据管道的空闲数
     * @param fetchCount          预期的数据获取量
     * @param fetchedCount        实际的数据获取量
     * @param e                   异常
     */
    void monitor(MonitorDataFetchDto monitorDto);

}
