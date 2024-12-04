package cn.dawnings.dto.privates;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class MonitorRateDto {

    private AtomicInteger count = new AtomicInteger(0);

    private void increment(){
        count.incrementAndGet();
    }
}
