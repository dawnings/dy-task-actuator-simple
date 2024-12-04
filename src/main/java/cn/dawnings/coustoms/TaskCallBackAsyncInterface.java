package cn.dawnings.coustoms;

import cn.dawnings.dto.TaskCallBackDto;

public interface TaskCallBackAsyncInterface<T>{
    public void callback(TaskCallBackDto<T> data);
}
