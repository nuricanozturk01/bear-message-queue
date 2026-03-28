package com.bearmq.api.metrics.dto;

public record QueueMetricDto(String id, String name, String status, boolean runtimeLoaded) {}
