package cn.dawnings.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true, fluent = true)
public class TaskCallBackDto<T> {
    private String taskName;
    private boolean status;
    private Exception exception;
    private T taskData;
}
