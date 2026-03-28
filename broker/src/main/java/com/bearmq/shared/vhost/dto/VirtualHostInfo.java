package com.bearmq.shared.vhost.dto;

import java.time.Instant;

public record VirtualHostInfo(
    String id,
    String name,
    String username,
    String password,
    String domain,
    String url,
    Instant createdAt,
    String status) {}
