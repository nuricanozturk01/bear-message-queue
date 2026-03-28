package com.bearmq.shared.event;

import java.time.Instant;

public record VirtualHostDeletedEvent(String tenantId, String virtualHostId, Instant occurredAt) {}
