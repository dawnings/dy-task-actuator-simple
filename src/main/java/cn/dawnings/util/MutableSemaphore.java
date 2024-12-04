package cn.dawnings.util;

import lombok.Getter;
import lombok.Synchronized;

import java.util.concurrent.Semaphore;

public class MutableSemaphore {

    public MutableSemaphore(int maxPermits) {
        this.maxPermits = maxPermits;
        semaphore = new TempSemaphore(maxPermits);
    }

    public MutableSemaphore() {
        semaphore = new TempSemaphore(0);
    }

    public void updateMaxPermits(int newMax) {
        if (newMax < 1) {
            throw new IllegalArgumentException("Semaphore at least 1," + " was " + newMax);
        }

        doUpdateMaxPermits(newMax);
    }

    private final TempSemaphore semaphore;

    private volatile int maxPermits = 0;

    @Getter
    private volatile boolean deprecated;

    private synchronized void doUpdateMaxPermits(int newMax) {
        int delta = newMax - this.maxPermits;
        if (delta == 0) {
            return;
        } else if (delta > 0) {
            this.semaphore.release(delta);
        } else {
            delta *= -1;
            this.semaphore.reducePermits(delta);
        }
        this.maxPermits = newMax;
    }

    public void release() {
        if (deprecated) throw new IllegalArgumentException("this semaphore is deprecated");
        this.semaphore.release();
    }

    public void release(int n) {
        if (deprecated) throw new IllegalArgumentException("this semaphore is deprecated");
        this.semaphore.release(n);
    }

    public void acquire() throws InterruptedException {
        if (deprecated) throw new IllegalArgumentException("this semaphore is deprecated");
        this.semaphore.acquire();
    }

    public void acquire(int n) throws InterruptedException {
        if (deprecated) throw new IllegalArgumentException("this semaphore is deprecated");
        this.semaphore.acquire(n);
    }

    public int getAvailablePermits() {
        return this.semaphore.availablePermits();
    }

    public void deprecated() {
        deprecated = true;
        doUpdateMaxPermits(0);
    }

    private static class TempSemaphore extends Semaphore {

        public TempSemaphore(int permits) {
            super(permits);
        }

        @Override
        protected void reducePermits(int reduction) {
            super.reducePermits(reduction);
        }
    }
}
