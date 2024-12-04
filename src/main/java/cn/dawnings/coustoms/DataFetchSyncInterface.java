package cn.dawnings.coustoms;

import java.util.List;

public interface DataFetchSyncInterface<T> {
    public List<T> didDataFetch(List<T> lastFetchData, int fetchCount);


}
