package cn.dawnings.coustoms;

public interface TaskRunnerSyncInterface<T>{
    public void didTaskRunner(T taskData) throws InterruptedException;
}
