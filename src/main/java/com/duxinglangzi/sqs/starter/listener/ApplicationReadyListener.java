package com.duxinglangzi.sqs.starter.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 监控程序是否可使用
 *
 * @author wuqiong 2022/6/25
 */
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    public static final AtomicBoolean START_LISTENER_CONTAINER = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!START_LISTENER_CONTAINER.get()) START_LISTENER_CONTAINER.set(true);
    }

}
