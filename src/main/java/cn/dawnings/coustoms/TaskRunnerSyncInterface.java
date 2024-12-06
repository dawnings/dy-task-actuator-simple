package cn.dawnings.coustoms;

import cn.dawnings.dto.TaskRunnerDto;

public interface TaskRunnerSyncInterface<T>{
    public void didTaskRunner(TaskRunnerDto<T> dto) ;
}
