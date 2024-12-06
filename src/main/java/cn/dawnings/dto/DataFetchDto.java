package cn.dawnings.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@Accessors(chain = true,fluent = true)
public class DataFetchDto<T> {
    private String taskName;
    private List<T> lastFetchData;
    private int fetchCount;
}
