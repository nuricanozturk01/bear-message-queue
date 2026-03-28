package com.bearmq.api.metrics;

import com.bearmq.api.metrics.dto.ResourceMetricsDto;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsSSEService {

  static final long INTERVAL_MS = 3_000L;
  private static final long EMITTER_TIMEOUT_MS = 5L * 60L * 1_000L;

  private final MetricsService metricsService;
  private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

  public SseEmitter subscribe() {
    final SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
    this.emitters.add(emitter);
    emitter.onCompletion(() -> this.emitters.remove(emitter));
    emitter.onTimeout(() -> this.emitters.remove(emitter));
    emitter.onError(ignored -> this.emitters.remove(emitter));
    log.debug("SSE client subscribed, active={}", this.emitters.size());
    return emitter;
  }

  @Scheduled(fixedDelay = INTERVAL_MS)
  public void broadcast() {
    if (this.emitters.isEmpty()) {
      return;
    }
    final ResourceMetricsDto snapshot = this.metricsService.resourceMetrics();
    this.emitters.removeIf(
        emitter -> {
          try {
            emitter.send(SseEmitter.event().name("metrics").data(snapshot));
            return false;
          } catch (final Exception ex) {
            log.debug("SSE emitter removed (send failed): {}", ex.getMessage());
            return true;
          }
        });
  }
}
