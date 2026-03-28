package com.bearmq.api.broker.dto.read;

import com.bearmq.shared.binding.DestinationType;

public record BindingSummaryDto(
    String id,
    String sourceExchangeName,
    DestinationType destinationType,
    String destinationName,
    String routingKey,
    String status) {}
