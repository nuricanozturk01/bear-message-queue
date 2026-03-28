package com.bearmq.api.metrics.dto;

public record MetricsSummaryDto(long usedVhosts, long usedQueues, long usedExchanges) {}
