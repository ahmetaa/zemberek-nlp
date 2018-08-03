package zemberek.workqueue;

public abstract class WorkItem<T> {
    int seqId;
    T work;
}
