package zemberek.core.concurrency;

import zemberek.core.logging.Log;

import java.util.concurrent.*;


public class BlockingExecutor extends ThreadPoolExecutor {

    final Semaphore semaphore;

    public BlockingExecutor(int poolSize, int queueSize) {
        super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        semaphore = new Semaphore(poolSize + queueSize);
    }

    public BlockingExecutor(int poolSize) {
        this(poolSize, poolSize);
    }

    @Override
    public void execute(Runnable command) {
        boolean acquired = false;
        do {
            try {
                semaphore.acquire();
                acquired = true;
            } catch (InterruptedException e) {
                Log.warn("Interrupted Exception when acquiring semaphore.", e);
            }
        } while (!acquired);
        try {
            super.execute(command);
        } catch (final RejectedExecutionException e) {
            semaphore.release();
            throw e;
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        semaphore.release();
    }
}