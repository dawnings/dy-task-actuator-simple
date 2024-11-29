package cn.dawnings.coustoms;

public interface TaskRunnerInterface <T>{
    public void didTaskRunner(T taskData) throws InterruptedException;
}
