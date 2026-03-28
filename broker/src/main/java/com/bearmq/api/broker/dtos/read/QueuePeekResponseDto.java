package com.bearmq.api.broker.dtos.read;

import java.util.List;

public record QueuePeekResponseDto(
    boolean runtimeLoaded, String queueName, boolean truncated, List<PeekedMessageDto> messages) {}
