package com.bearmq.api.metrics.dto;

import com.bearmq.shared.vhost.VirtualHost;
import java.util.List;

public record VhostMetricsAssembly(
    VirtualHost vhost,
    boolean runtimeLoaded,
    int exchangeCount,
    int bindingCount,
    List<QueueMetricDto> queues) {}
