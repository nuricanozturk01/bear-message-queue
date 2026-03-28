package com.bearmq.shared.broker.runtime;

/**
 * Approximate unread count from the broker consumer offset to the end of the Chronicle queue.
 *
 * @param approximateCount {@code -1} when the queue is not loaded in broker runtime
 */
public record QueuePendingSnapshot(long approximateCount, boolean capped) {

  public static final QueuePendingSnapshot NOT_LOADED = new QueuePendingSnapshot(-1L, false);

  public boolean notLoaded() {

    return this.approximateCount < 0L;
  }
}
