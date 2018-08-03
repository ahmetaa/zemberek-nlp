package zemberek.workqueue;

import java.util.concurrent.BlockingQueue;

public abstract class Worker implements Runnable {
  private final BlockingQueue<WorkItem> workQueue;

  public Worker(BlockingQueue<WorkItem> workQueue) {
    this.workQueue = workQueue;
  }

  @Override
  public void run() {
    while(!workQueue.isEmpty()) {
      try {
        WorkItem item = workQueue.take();
        process(item);
      } catch (InterruptedException e) {
        // Thread interrupted, silently exits.
      }
    }
  }

  public abstract void process(WorkItem item);
}
