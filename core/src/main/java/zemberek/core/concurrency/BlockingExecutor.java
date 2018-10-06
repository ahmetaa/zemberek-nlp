package zemberek.core.concurrency;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import zemberek.core.logging.Log;


public class BlockingExecutor extends ThreadPoolExecutor {

  private final Semaphore semaphore;

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