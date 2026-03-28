package com.bearmq.api.broker.dto.read;

public record QueueSummaryDto(
    String id,
    String name,
    String actualName,
    boolean durable,
    boolean exclusive,
    boolean autoDelete,
    String status) {}
