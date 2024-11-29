package cn.dawnings.dto;

import lombok.Data;

@Data
public class MontorTaskDto<T> {
    T taskData;
    boolean taskStatus;
    String taskMsg;
    Exception exception;
}
