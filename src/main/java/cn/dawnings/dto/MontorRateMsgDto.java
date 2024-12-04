package cn.dawnings.dto;

import cn.dawnings.util.LimitSizeMap;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
@Accessors(chain = true, fluent = true)
public class MontorRateMsgDto {
    private String taskName;
    private String key;
    private String rate;
    private int lastSize;
}
