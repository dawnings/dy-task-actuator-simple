package cn.dawnings.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true,fluent = true)
public class MonitorDataFetchDto {
    private String taskName;
    private int status;
//    private int executeQueueRemain;
    private int eplenishQueueRemain;
    private int fetchedCount;
    private Exception e;
}
