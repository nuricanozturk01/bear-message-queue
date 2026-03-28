package com.bearmq.api.metrics.dto;

public record MetricsSummarySource(long usedVhosts, long usedQueues, long usedExchanges) {}
