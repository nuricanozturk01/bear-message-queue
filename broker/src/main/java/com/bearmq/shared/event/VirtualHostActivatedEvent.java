package com.bearmq.shared.event;

import java.time.Instant;

public record VirtualHostActivatedEvent(String virtualHostId, Instant occurredAt) {}
