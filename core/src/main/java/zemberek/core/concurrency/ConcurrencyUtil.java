package zemberek.core.concurrency;

import zemberek.core.logging.Log;

public class ConcurrencyUtil {

    /**
     * Validates cpu bound [threadCount] value. If number is larger than the N = Runtime.getRuntime().availableProcessors()
     * it will set it to N.
     *
     * @param threadCount input thread count value to verify.
     * @return if threadCount is positive and not larger than N, returns the same. Otherwise throws exception or returns N.
     * @throws IllegalArgumentException if threadCount is not positive.
     */
    public static int validateCpuThreadCount(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count cannot be less than 1. But it is " + threadCount);
        }
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (threadCount > availableProcessors) {
            Log.warn("Thread count %d is larger than the CPU count %d. Available CPU count %d will be used.",
                    threadCount, availableProcessors, availableProcessors);
            return availableProcessors;
        } else {
            return threadCount;
        }
    }

}
