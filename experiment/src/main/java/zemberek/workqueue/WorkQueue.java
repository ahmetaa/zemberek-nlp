package zemberek.workqueue;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkQueue {
    ExecutorService service;
    ConcurrentLinkedDeque<WorkItem> workItems;

    public WorkQueue() {
      service = Executors.newCachedThreadPool();
      workItems = new ConcurrentLinkedDeque<>();
    }

    public void submit(WorkItem item) {
      workItems.add(item);
    }

}
