package com.bearmq.shared.broker.runtime;

import java.util.Set;

public interface BrokerRuntimePort {

  void reloadVhostRuntime(String vhostId);

  boolean isVhostLoaded(String vhostId);

  Set<String> getLoadedQueueNames(String vhostId);

  /**
   * Walks from the persisted consumer offset to EOF under the queue lock (serialized with dequeue).
   * {@link QueuePendingSnapshot#NOT_LOADED} when the queue is not open in this broker process.
   */
  QueuePendingSnapshot pendingMessagesForQueue(String vhostId, String queueName);

  /**
   * Reads from the consumer offset without dequeueing, returns the {@code maxReturn} <strong>most
   * recent</strong> pending payloads (tail of backlog, capped at 5), ordered oldest→newest within
   * that window. Never updates {@code .bearmq-consumer-index}. {@link QueuePeekResult#truncated()}
   * is true when more pending messages exist than returned (or the scan cap was hit).
   */
  QueuePeekResult peekPendingMessages(String vhostId, String queueName, int maxReturn);
}
