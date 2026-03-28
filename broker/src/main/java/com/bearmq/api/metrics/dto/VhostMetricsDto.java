package com.bearmq.api.metrics.dto;

import java.util.List;

public record VhostMetricsDto(
    String vhostId,
    String vhostName,
    boolean runtimeLoaded,
    int queueCount,
    int exchangeCount,
    int bindingCount,
    List<QueueMetricDto> queues) {}
