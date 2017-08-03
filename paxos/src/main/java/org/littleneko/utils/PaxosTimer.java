package org.littleneko.utils;

import java.util.concurrent.*;

/**
 * Created by little on 2017-06-20.
 */
public class PaxosTimer {
    private ScheduledExecutorService executor;
    private ConcurrentHashMap<Integer, ScheduledFuture> scheduledFutureMap;

    /**
     * 初始化
     *
     * @param threadNum 定时器的线程数量
     */
    public PaxosTimer(int threadNum) {
        executor = Executors.newScheduledThreadPool(threadNum);
        scheduledFutureMap = new ConcurrentHashMap<>();
    }

    /**
     * 初始化，默认创建单线程的定时器
     */
    public PaxosTimer() {
        executor = Executors.newScheduledThreadPool(1);
        scheduledFutureMap = new ConcurrentHashMap<>();
    }

    /**
     * 添加一个超时定时器
     *
     * @param timerID         定时器ID
     * @param timeoutListener 超时执行函数
     * @param timeOut         超时时间
     * @param timeUnit        单位
     */
    public void addTimer(int timerID, TimeoutListener timeoutListener, int timeOut, TimeUnit timeUnit) {
        if (scheduledFutureMap.containsKey(timerID)) {
            cancelTimer(timerID);
        }

        ScheduledFuture future = executor.schedule(() -> {
            scheduledFutureMap.remove(timerID);
            timeoutListener.onTimeOut(timerID);
        }, timeOut, TimeUnit.SECONDS);

        scheduledFutureMap.put(timerID, future);
    }

    /**
     * 移除定时器
     *
     * @param timerID 定时器ID
     */
    public void cancelTimer(int timerID) {
        ScheduledFuture future = scheduledFutureMap.get(timerID);
        if (future != null) {
            scheduledFutureMap.remove(timerID);
            future.cancel(true);
        }
    }

    /**
     * 添加一个定时任务
     *
     * @param timerID         定时器ID
     * @param timeoutListener 超时执行函数
     * @param timeOut         超时时间
     * @param timeUnit        单位
     */
    public void addSchedule(int timerID, TimeoutListener timeoutListener, int timeOut, TimeUnit timeUnit) {
        if (scheduledFutureMap.containsKey(timerID)) {
            cancelTimer(timerID);
        }

        ScheduledFuture future = executor.scheduleAtFixedRate(() -> {
            scheduledFutureMap.remove(timerID);
            timeoutListener.onTimeOut(timerID);
        }, 0, timeOut, TimeUnit.SECONDS);

        scheduledFutureMap.put(timerID, future);
    }
}
