package com.qmyan.fujitestdemo.common;

import android.support.annotation.NonNull;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author MichealYan
 * @date 2018/11/28
 * Email: 956462326@qq.com
 * Describe: 线程池管理
 */
public class ThreadPoolManager {
    private static ThreadPoolManager mInstance = new ThreadPoolManager();

    public static ThreadPoolManager getInstance() {
        return mInstance;
    }

    private final ThreadPoolExecutor executor;

    private ThreadPoolManager() {
        // 处理器核心数
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        // 线程池的核心线程数
        int corePoolSize = availableProcessors + 1;
        // 线程池所能容纳的最大的线程数，设置为当前设备可用处理器核心数*2 + 1,能够让cpu的效率得到最大程度执行（有研究论证的）
        int maximumPoolSize = availableProcessors * 2 + 1;
        // 线程闲置时的超时时长（默认只对非核心线程有效，
        // 当ThreadPoolExecutor的allowCoreThreadTimeOut属性设置为true时，才会作用于核心线程）
        long keepAliveTime = 1;
        // 用于指定超时时长的时间单位
        TimeUnit timeUnit = TimeUnit.SECONDS;
        // 线程池中的任务队列，当活动线程数大于核心线程时，新的任务就会被添加到其中进行等待
        BlockingDeque<Runnable> blockingDeque = new LinkedBlockingDeque<>(128);
        // 线程工厂，为线程池提供创建新线程的功能
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "ThreadPoolManager #" + mCount.getAndIncrement());
            }
        };
        // 指定无法执行新任务时的处理决策
        // 默认为AbortPolicy，会抛出一个RejectedExecutionException
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
        executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                timeUnit,
                blockingDeque,
                threadFactory,
                rejectedExecutionHandler
        );
    }

    /**
     * 执行任务
     */
    public void execute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        executor.execute(runnable);
    }

    /**
     * 从线程池中移除任务
     */
    public void remove(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        executor.remove(runnable);
    }
}

