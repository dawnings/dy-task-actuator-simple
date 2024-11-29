package cn.dawnings.coustoms;

public interface TaskCallBackInterface<T>{
    public void callbackSuccess(T data);
    public void callbackError(T data, Exception e);
}
