package cn.dawnings.monitor;

import cn.dawnings.dto.MonitorDataFetchDto;

public interface MonitorDataFetchAsyncInterface<T> {

    /**
     * 镜像方法，不应该执行长时间任务
     * @param monitorDto 状态参数
     */
    void monitorFetch(MonitorDataFetchDto monitorDto);

}
