package com.sky.Task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Slf4j
//@Component
public class MyTask {
    /**
     * 定时任务
     */
    @Scheduled(cron = "0/1 * * * * *")
    public void Task(){
        log.info("定时任务:{}", LocalDateTime.now());
    }
}
