package com.bearmq.server.metrics.runner;

import com.bearmq.server.metrics.ThreadMetrics;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "bearmq.server.metrics.enabled", havingValue = "true")
@Slf4j
@NamedInterface
@RequiredArgsConstructor
@Data
public class MetricServer implements Closeable {

  private static final int INITIAL_DELAY_S = 3;
  private static final int INTERVAL_S = 5;

  private final DatagramSocket datagramSocket;
  private final List<Thread> threads = new ArrayList<>();
  private final ScheduledExecutorService scheduledPool;

  @Value("${bearmq.server.metrics.destination-host:127.0.0.1}")
  private String destinationHost;

  @Value("${bearmq.server.metrics.destination-port:${bearmq.server.metrics.port:6668}}")
  private int destinationPort;

  @Override
  public void close() throws IOException {
    this.scheduledPool.shutdown();
    this.datagramSocket.close();
  }

  public void run() {
    log.info(
        "MetricServer UDP broadcaster started — sending to {}:{} every {}s",
        this.destinationHost,
        this.destinationPort,
        INTERVAL_S);

    this.scheduledPool.scheduleAtFixedRate(
        this::broadcastMetrics, INITIAL_DELAY_S, INTERVAL_S, TimeUnit.SECONDS);
  }

  private void broadcastMetrics() {
    try {
      final String json = ThreadMetrics.snapshot();
      final byte[] data = json.getBytes(StandardCharsets.UTF_8);
      final InetAddress addr = InetAddress.getByName(this.destinationHost);
      final DatagramPacket pkt = new DatagramPacket(data, data.length, addr, this.destinationPort);
      this.datagramSocket.send(pkt);
      log.debug(
          "MetricServer UDP sent {} bytes to {}:{}",
          data.length,
          this.destinationHost,
          this.destinationPort);
    } catch (final IOException e) {
      log.warn("MetricServer UDP send failed: {}", e.getMessage());
    }
  }
}
