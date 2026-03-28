package com.bearmq.api.metrics.mapper;

import com.bearmq.api.metrics.dto.MetricsSummaryDto;
import com.bearmq.api.metrics.dto.MetricsSummarySource;
import com.bearmq.api.metrics.dto.QueueMetricDto;
import com.bearmq.api.metrics.dto.ResourceMetricsDto;
import com.bearmq.api.metrics.dto.ResourceMetricsSource;
import com.bearmq.api.metrics.dto.VhostMetricsAssembly;
import com.bearmq.api.metrics.dto.VhostMetricsDto;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.vhost.VirtualHost;
import java.util.List;
import java.util.Set;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class MetricsDtoMapper {

  public MetricsSummaryDto toSummaryCounts(
      final long usedVhosts, final long usedQueues, final long usedExchanges) {

    return this.toSummary(new MetricsSummarySource(usedVhosts, usedQueues, usedExchanges));
  }

  public VhostMetricsDto assembleVhostMetrics(
      final VirtualHost vhost,
      final boolean runtimeLoaded,
      final int exchangeCount,
      final int bindingCount,
      final List<QueueMetricDto> queues) {

    return this.toVhostMetrics(
        new VhostMetricsAssembly(vhost, runtimeLoaded, exchangeCount, bindingCount, queues));
  }

  public ResourceMetricsDto toResourceFrom(
      final long heapUsedMb,
      final long heapCommittedMb,
      final long heapMaxMb,
      final int heapUsedPct,
      final long nonHeapUsedMb,
      final double processCpuPct,
      final double systemCpuPct,
      final int threadCount,
      final int availableProcessors,
      final long uptimeSeconds) {

    return this.toResourceMetrics(
        new ResourceMetricsSource(
            heapUsedMb,
            heapCommittedMb,
            heapMaxMb,
            heapUsedPct,
            nonHeapUsedMb,
            processCpuPct,
            systemCpuPct,
            threadCount,
            availableProcessors,
            uptimeSeconds));
  }

  protected abstract MetricsSummaryDto toSummary(MetricsSummarySource source);

  @Mapping(target = "vhostId", source = "vhost.id")
  @Mapping(target = "vhostName", source = "vhost.name")
  @Mapping(target = "runtimeLoaded", source = "runtimeLoaded")
  @Mapping(target = "queueCount", expression = "java(a.queues().size())")
  @Mapping(target = "exchangeCount", source = "exchangeCount")
  @Mapping(target = "bindingCount", source = "bindingCount")
  @Mapping(target = "queues", source = "queues")
  protected abstract VhostMetricsDto toVhostMetrics(VhostMetricsAssembly a);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "name", source = "name")
  @Mapping(target = "status", source = ".", qualifiedByName = "queueStatusName")
  @Mapping(target = "runtimeLoaded", expression = "java(loaded.contains(queue.getName()))")
  public abstract QueueMetricDto toQueueMetric(Queue queue, @Context Set<String> loaded);

  protected abstract ResourceMetricsDto toResourceMetrics(ResourceMetricsSource source);

  @Named("queueStatusName")
  protected String queueStatusName(final Queue queue) {

    final Status s = queue.getStatus();
    return s != null ? s.name() : "UNKNOWN";
  }
}
