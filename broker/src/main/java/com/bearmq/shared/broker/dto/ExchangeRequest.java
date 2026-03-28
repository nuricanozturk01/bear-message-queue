package com.bearmq.shared.broker.dto;

import java.util.Map;

public record ExchangeRequest(
    String name,
    String type,
    boolean durable,
    boolean internal,
    boolean delayed,
    Map<String, Object> args) {}
