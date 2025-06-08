package com.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Bugee
 */
@Slf4j
public abstract class AsyncUtils {

    private final static String ASYNC_THREAD_PREFIX = "siyukio-task-";

    private final static ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private final static ExecutorService VIRTUAL_EXECUTOR_SERVICE = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name(ASYNC_THREAD_PREFIX, 0)
            .factory());
    private final static AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    public static boolean isAsyncThread() {
        Thread thread = Thread.currentThread();
        return thread.getName().startsWith(ASYNC_THREAD_PREFIX);
    }

    public static boolean isRunning() {
        return ATOMIC_INTEGER.get() > 0;
    }

    public static void submit(Runnable runnable) {
        VIRTUAL_EXECUTOR_SERVICE.submit(() -> {
            ATOMIC_INTEGER.incrementAndGet();
            try {
                runnable.run();
            } catch (Exception ex) {
                log.error("TaskClient submit runnable error", ex);
            } finally {
                ATOMIC_INTEGER.decrementAndGet();
            }
        });
    }

    public static <T> Future<T> submit(Callable<T> callable) {
        return VIRTUAL_EXECUTOR_SERVICE.submit(() -> {
            ATOMIC_INTEGER.incrementAndGet();
            try {
                return callable.call();
            } catch (Exception ex) {
                log.error("TaskClient submit callable error", ex);
            } finally {
                ATOMIC_INTEGER.decrementAndGet();
            }
            return null;
        });
    }

    public static ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
            submit(runnable);
        }, delay, unit);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            submit(runnable);
        }, initialDelay, period, unit);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        return SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(() -> {
            submit(runnable);
        }, initialDelay, delay, unit);
    }
}
