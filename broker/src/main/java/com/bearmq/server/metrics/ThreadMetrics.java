package com.bearmq.server.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

public final class ThreadMetrics {

  private static final long BYTES_PER_MB = 1024L * 1024L;
  private static final long NS_PER_MS = 1_000_000L;
  private static final long MS_PER_S = 1_000L;
  private static final double CPU_SCALE = 1000.0;
  private static final double CPU_DIV = 10.0;

  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
  private static final OperatingSystemMXBean OS_MX_BEAN =
      ManagementFactory.getOperatingSystemMXBean();

  static {
    if (THREAD_MX_BEAN.isThreadCpuTimeSupported() && !THREAD_MX_BEAN.isThreadCpuTimeEnabled()) {
      THREAD_MX_BEAN.setThreadCpuTimeEnabled(true);
    }
  }

  private ThreadMetrics() {}

  public static String snapshot() {

    final MemoryUsage heap = MEMORY_MX_BEAN.getHeapMemoryUsage();
    final MemoryUsage nonHeap = MEMORY_MX_BEAN.getNonHeapMemoryUsage();

    final long heapMb = heap.getUsed() / BYTES_PER_MB;
    final long heapCmtMb = heap.getCommitted() / BYTES_PER_MB;
    final long rawMax = heap.getMax();
    final long heapMaxMb = rawMax > 0 ? rawMax / BYTES_PER_MB : 0;
    final long denominator = rawMax > 0 ? rawMax : heap.getCommitted();
    final int heapPct = denominator > 0 ? (int) ((heap.getUsed() * 100L) / denominator) : 0;
    final long nonHeapMb = nonHeap.getUsed() / BYTES_PER_MB;
    final int threads = THREAD_MX_BEAN.getThreadCount();
    final long uptimeS = ManagementFactory.getRuntimeMXBean().getUptime() / MS_PER_S;

    double procCpu = -1;
    double sysCpu = -1;
    if (OS_MX_BEAN instanceof final com.sun.management.OperatingSystemMXBean sun) {
      final double p = sun.getProcessCpuLoad();
      final double s = sun.getCpuLoad();
      procCpu = p >= 0 ? Math.round(p * CPU_SCALE) / CPU_DIV : -1;
      sysCpu = s >= 0 ? Math.round(s * CPU_SCALE) / CPU_DIV : -1;
    }

    return String.format(
        "{\"heap_mb\":%d,\"heap_committed_mb\":%d,\"heap_max_mb\":%d,\"heap_pct\":%d,"
            + "\"non_heap_mb\":%d,\"proc_cpu\":%.1f,\"sys_cpu\":%.1f,"
            + "\"threads\":%d,\"uptime_s\":%d}",
        heapMb, heapCmtMb, heapMaxMb, heapPct, nonHeapMb, procCpu, sysCpu, threads, uptimeS);
  }

  public static long cpuTimeMs(final Thread thread) {

    final long ns = THREAD_MX_BEAN.getThreadCpuTime(thread.threadId());
    return ns < 0 ? -1 : ns / NS_PER_MS;
  }
}
