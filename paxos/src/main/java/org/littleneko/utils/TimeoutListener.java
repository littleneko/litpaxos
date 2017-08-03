package org.littleneko.utils;

/**
 * 定时器超时
 * Created by little on 2017-06-20.
 */
public interface TimeoutListener {
    /**
     * 定时器超时后需要执行的函数
     */
    void onTimeOut(int timerID);
}
