package cashu.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

@AllArgsConstructor
@Builder
@Log
public class ThreadUtil<T extends ThreadUtil.Task> {

    private final T task;

    @Builder.Default
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    @Builder.Default
    private boolean blocking = false;

    private ReentrantLock lock;

    private static final int DEFAULT_TIMEOUT_SECONDS = 5;

    public ThreadUtil(@NonNull T task) {
        this.task = task;
    }

    public void run() throws TimeoutException {
        log.log(Level.FINE, "Executing thread on {0}...", task);
        ExecutorService threadPool = Executors.newCachedThreadPool();
        Future<?> futureTask = threadPool.submit(() -> {
            if (lock != null) {
                lock.lock();
            }
            try {
                task.execute();
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to execute task: {0}", e.getMessage());
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        });

        if (blocking) {
            try {
                log.log(Level.FINE, "Waiting for thread to complete... ");
                futureTask.get(timeoutSeconds, TimeUnit.SECONDS); // Wait for the thread to complete
                log.log(Level.FINE, "Thread execution completed!");
            } catch (InterruptedException | ExecutionException e) {
                log.log(Level.WARNING, "Failed to execute task: {0}", e.getMessage());
                throw new RuntimeException(e);
            } finally {
                threadPool.shutdown();
            }
        }
    }

    public interface Task<T> {

        T execute();
    }

    public static class Locks {

        // Used to lock the minting and melting processes
        public static final ReentrantLock LOCK45 = new ReentrantLock();
    }
}
