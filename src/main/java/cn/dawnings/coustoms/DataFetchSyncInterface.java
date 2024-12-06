package cn.dawnings.coustoms;

import cn.dawnings.dto.DataFetchDto;

import java.util.List;

public interface DataFetchSyncInterface<T> {
    public List<T> didDataFetch(DataFetchDto<T> dataFetchDto);


}
