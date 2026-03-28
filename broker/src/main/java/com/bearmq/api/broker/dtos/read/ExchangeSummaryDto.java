package com.bearmq.api.broker.dtos.read;

public record ExchangeSummaryDto(
    String id,
    String name,
    String actualName,
    String type,
    boolean durable,
    boolean internal,
    String status) {}
