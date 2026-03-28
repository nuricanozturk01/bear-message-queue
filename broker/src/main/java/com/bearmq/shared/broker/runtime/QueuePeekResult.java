package com.bearmq.shared.broker.runtime;

import java.util.List;

/**
 * Pending messages read from the consumer offset without advancing it.
 *
 * @param truncated {@code true} when more pending messages exist than {@link #messages()} (or the
 *     peek scan cap was reached before EOF).
 */
public record QueuePeekResult(List<byte[]> messages, boolean truncated) {

  public static final QueuePeekResult EMPTY = new QueuePeekResult(List.of(), false);
}
