package com.bearmq.api.metrics.controllers;

import com.bearmq.api.auth.services.TenantContext;
import com.bearmq.api.metrics.dto.MetricsSummaryDto;
import com.bearmq.api.metrics.dto.ResourceMetricsDto;
import com.bearmq.api.metrics.dto.VhostMetricsDto;
import com.bearmq.api.metrics.services.MetricsSSEService;
import com.bearmq.api.metrics.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

  private final MetricsService metricsService;
  private final MetricsSSEService metricsSSEService;
  private final TenantContext tenantContext;

  @GetMapping("/summary")
  public ResponseEntity<MetricsSummaryDto> summary() {

    this.tenantContext.requireTenant();
    return ResponseEntity.ok(this.metricsService.summary());
  }

  @GetMapping("/vhost/{vhostId}")
  public ResponseEntity<VhostMetricsDto> vhostMetrics(@PathVariable final String vhostId) {

    this.tenantContext.requireTenant();
    return ResponseEntity.ok(this.metricsService.vhostMetrics(vhostId));
  }

  @GetMapping("/resources")
  public ResponseEntity<ResourceMetricsDto> resources() {

    this.tenantContext.requireTenant();
    return ResponseEntity.ok(this.metricsService.resourceMetrics());
  }

  @GetMapping(value = "/resources/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamResources() {

    this.tenantContext.requireTenant();
    return this.metricsSSEService.subscribe();
  }
}
