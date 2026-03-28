package com.bearmq.shared.event;

import java.time.Instant;

/**
 * @param purgePersistentData when true (instance removed), TCP storage under the vhost is deleted
 *     after unload; when false (e.g. status no longer ACTIVE), runtime is unloaded but DB topology
 *     and queue files stay for reactivation.
 */
public record VirtualHostDeletedEvent(
    String tenantId, String virtualHostId, Instant occurredAt, boolean purgePersistentData) {}
