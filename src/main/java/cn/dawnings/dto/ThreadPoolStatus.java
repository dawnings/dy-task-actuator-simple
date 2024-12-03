package cn.dawnings.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThreadPoolStatus {
    private String threadName;
    private int corePoolSize;
    private int activeCount;
    private int queueMax;
    private int queueMaxRemainingCapacity;
    private int queueSize;
    private int batchMinLimit;
    private int pollWaitMax;
    private int pollWaitMin;
    private int lastFetchCount;
    private LocalDateTime lastFetchTime;
}
