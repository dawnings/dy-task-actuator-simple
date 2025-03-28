package cn.dawnings.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true, fluent = true)
public class MontorTaskDto<T> {
    private T taskData;
    private String taskName;
    private boolean status;
    private String taskMsg;
    private Exception exception;
}
