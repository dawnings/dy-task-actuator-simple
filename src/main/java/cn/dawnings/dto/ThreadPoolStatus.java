package cn.dawnings.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThreadPoolStatus {
    private String taskName;
    private int corePoolSize;
    private int maximumPoolSize;
    private int activeCount;
    private int queueMax;
    private int threadQueueSize;
    private int queueRemainingCapacity;
    private int queueSize;
    private int batchMinLimit;
    private int pollWaitMax;
    private int pollWaitMin;
    private int lastFetchCount;
    private LocalDateTime lastFetchTime;

    private boolean stopping;

    public boolean isStopped() {
        return stopping && threadPoolStopped && fillMonitorRateExecutorStopped && taskActuatorsExecutorStopped;
    }

    private boolean threadPoolStopped;
    private boolean fillMonitorRateExecutorStopped;
    private boolean taskActuatorsExecutorStopped;
    private int fetchDataCount;

    public int availablePermits;
}
