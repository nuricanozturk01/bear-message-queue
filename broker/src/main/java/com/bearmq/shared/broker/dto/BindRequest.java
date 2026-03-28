package com.bearmq.shared.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BindRequest(
    String source,
    String destination,
    @JsonProperty("destination_type") String destinationType,
    @JsonProperty("routing_key") String routingKey) {}
