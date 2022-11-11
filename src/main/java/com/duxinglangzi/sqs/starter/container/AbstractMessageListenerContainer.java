package com.duxinglangzi.sqs.starter.container;

import com.duxinglangzi.sqs.starter.listener.ApplicationReadyListener;
import org.springframework.context.SmartLifecycle;

/**
 * 抽象一个消息拉取器,生命周期交给spring进行管理
 *
 * @author wuqiong 2022/6/25
 */
public abstract class AbstractMessageListenerContainer implements SmartLifecycle {
    protected boolean isRunning = false;
    protected Long SLEEP_TIME_MILLI_SECONDS = 1000L;

    protected abstract void doStart();
    protected abstract void doInit();

    @Override
    public void start() {
        setRunning(true);
        new Thread(() -> {
            // 待spring 应用程序准备就绪后,再开始拉取消息
            while (!ApplicationReadyListener.START_LISTENER_CONTAINER.get()) sleep(5L * SLEEP_TIME_MILLI_SECONDS);
            doInit();// 初始化部分参数
            while (isRunning() && !Thread.currentThread().isInterrupted()) doStart();
        }).start();
    }

    @Override
    public void stop() {
        setRunning(false);
    }

    @Override
    public void stop(Runnable callback) {
        callback.run();
        setRunning(false);
        sleep(SLEEP_TIME_MILLI_SECONDS);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    protected void setRunning(boolean bool) {
        isRunning = bool;
    }

    protected void sleep(long sleepTimeMilliSeconds) {
        try {
            Thread.sleep(sleepTimeMilliSeconds);
        } catch (InterruptedException e) {
        }
    }


}
