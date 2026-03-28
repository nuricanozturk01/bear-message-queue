package com.bearmq.api.broker.dto.read;

public record ExchangeSummaryDto(
    String id,
    String name,
    String actualName,
    String type,
    boolean durable,
    boolean internal,
    String status) {}
