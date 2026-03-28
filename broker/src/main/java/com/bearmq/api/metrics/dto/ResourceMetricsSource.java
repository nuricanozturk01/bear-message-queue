package com.bearmq.api.metrics.dto;

public record ResourceMetricsSource(
    long heapUsedMb,
    long heapCommittedMb,
    long heapMaxMb,
    int heapUsedPct,
    long nonHeapUsedMb,
    double processCpuPct,
    double systemCpuPct,
    int threadCount,
    int availableProcessors,
    long uptimeSeconds) {}
