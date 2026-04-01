package com.bearmq.api.broker.dtos.read;

public record QueueSummaryDto(
    String id,
    String name,
    String actualName,
    boolean durable,
    boolean exclusive,
    boolean autoDelete,
    boolean deadLetter,
    String status) {}
