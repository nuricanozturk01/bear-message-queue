package com.bearmq.api.metrics.services;

import com.bearmq.api.metrics.dto.MetricsSummaryDto;
import com.bearmq.api.metrics.dto.QueueMetricDto;
import com.bearmq.api.metrics.dto.ResourceMetricsDto;
import com.bearmq.api.metrics.dto.VhostMetricsDto;
import com.bearmq.api.metrics.mapper.MetricsDtoMapper;
import com.bearmq.shared.binding.BindingRepository;
import com.bearmq.shared.broker.runtime.BrokerRuntimePort;
import com.bearmq.shared.exchange.ExchangeRepository;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.queue.QueueRepository;
import com.bearmq.shared.vhost.VirtualHost;
import com.bearmq.shared.vhost.VirtualHostRepository;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetricsService {

  private static final long BYTES_PER_MB = 1024L * 1024L;
  private static final double CPU_SCALE = 1000.0;
  private static final double CPU_DIVISOR = 10.0;

  private final VirtualHostRepository virtualHostRepository;
  private final QueueRepository queueRepository;
  private final ExchangeRepository exchangeRepository;
  private final BindingRepository bindingRepository;
  private final BrokerRuntimePort brokerRuntime;
  private final MetricsDtoMapper metricsDtoMapper;

  @Transactional(readOnly = true)
  public MetricsSummaryDto summary() {

    final long usedVhosts = this.virtualHostRepository.countActiveGlobal();
    final long usedQueues = this.queueRepository.countActiveGlobal();
    final long usedExchanges = this.exchangeRepository.countActiveGlobal();

    return this.metricsDtoMapper.toSummaryCounts(usedVhosts, usedQueues, usedExchanges);
  }

  @Transactional(readOnly = true)
  public VhostMetricsDto vhostMetrics(final String vhostId) {

    final VirtualHost vhost =
        this.virtualHostRepository
            .findById(vhostId)
            .filter(v -> !v.isDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Virtual host not found"));

    final List<Queue> queues =
        this.queueRepository.findAllByVhostId(vhostId).stream()
            .filter(q -> !q.isDeleted())
            .toList();

    final int exchangeCount =
        (int)
            this.exchangeRepository.findListByVhostId(vhostId).stream()
                .filter(e -> !e.isDeleted())
                .count();

    final int bindingCount = this.bindingRepository.findAllActiveForReadByVhostId(vhostId).size();

    final boolean vhostLoaded = this.brokerRuntime.isVhostLoaded(vhostId);
    final Set<String> loaded = this.brokerRuntime.getLoadedQueueNames(vhostId);

    final List<QueueMetricDto> queueMetrics =
        queues.stream()
            .map(
                q ->
                    this.metricsDtoMapper.toQueueMetric(
                        q,
                        loaded,
                        this.brokerRuntime.pendingMessagesForQueue(vhostId, q.getName())))
            .toList();

    return this.metricsDtoMapper.assembleVhostMetrics(
        vhost, vhostLoaded, exchangeCount, bindingCount, queueMetrics);
  }

  public ResourceMetricsDto resourceMetrics() {

    final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
    final MemoryUsage heap = memBean.getHeapMemoryUsage();
    final MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

    final long heapUsedMb = heap.getUsed() / BYTES_PER_MB;
    final long heapCommittedMb = heap.getCommitted() / BYTES_PER_MB;
    final long rawMax = heap.getMax();
    final long heapMaxMb = rawMax > 0 ? rawMax / BYTES_PER_MB : 0;
    final long denominator = rawMax > 0 ? rawMax : heap.getCommitted();
    final int heapUsedPct = denominator > 0 ? (int) ((heap.getUsed() * 100L) / denominator) : 0;
    final long nonHeapUsedMb = nonHeap.getUsed() / BYTES_PER_MB;

    final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    final int threadCount = threadBean.getThreadCount();
    final long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1_000L;
    final int availableProcs = Runtime.getRuntime().availableProcessors();

    double processCpuPct = -1;
    double systemCpuPct = -1;
    final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    if (osBean instanceof final com.sun.management.OperatingSystemMXBean sunOs) {
      final double procLoad = sunOs.getProcessCpuLoad();
      final double sysLoad = sunOs.getCpuLoad();
      processCpuPct = procLoad >= 0 ? Math.round(procLoad * CPU_SCALE) / CPU_DIVISOR : -1;
      systemCpuPct = sysLoad >= 0 ? Math.round(sysLoad * CPU_SCALE) / CPU_DIVISOR : -1;
    }

    return this.metricsDtoMapper.toResourceFrom(
        heapUsedMb,
        heapCommittedMb,
        heapMaxMb,
        heapUsedPct,
        nonHeapUsedMb,
        processCpuPct,
        systemCpuPct,
        threadCount,
        availableProcs,
        uptimeSeconds);
  }
}
