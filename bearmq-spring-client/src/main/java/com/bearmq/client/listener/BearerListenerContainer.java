package com.bearmq.client.listener;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.bearmq.client.BearMessagingTemplate;
import com.bearmq.client.config.BearConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BearerListenerContainer implements Closeable {
  public static final byte SPACE = 0x20;
  public static final byte HORIZONTAL_TAB = 0x09;
  public static final byte LINE_FEED = 0x0A;
  public static final byte CARRIAGE_RETURN = 0x0D;

  private static final Logger LOGGER = LoggerFactory.getLogger(BearerListenerContainer.class);
  private static final int SCHEDULED_THREAD_POOL_SIZE = 8;
  private static final int RETRY_ATTEMPT_OFFSET = 1;

  private final ObjectMapper objectMapper;
  private final BearConfig config;
  private final BearMessagingTemplate template;
  private final DeadLetterQueueRouter dlqRouter;
  private final Map<String, List<Handler>> handlersByQueue;
  private ScheduledExecutorService executor;

  public BearerListenerContainer(
      final ObjectMapper objectMapper,
      final BearMessagingTemplate template,
      final BearConfig config,
      final DeadLetterQueueRouter dlqRouter) {
    this.objectMapper = objectMapper;
    this.template = template;
    this.config = config;
    this.dlqRouter = dlqRouter;
    this.handlersByQueue = new ConcurrentHashMap<>();
  }

  public void register(final Map<String, List<Handler>> map) {
    this.handlersByQueue.putAll(map);
  }

  public void start() {
    final int handlerCount = this.handlersByQueue.values().stream().mapToInt(List::size).sum();
    final int poolSize = Math.max(SCHEDULED_THREAD_POOL_SIZE, handlerCount);
    this.executor = Executors.newScheduledThreadPool(poolSize);

    final int initDelay = this.config.getInitialDelayMs();
    final int period = Math.max(50, this.config.getPeriodMs());

    for (final Map.Entry<String, List<Handler>> entry : this.handlersByQueue.entrySet()) {
      final String queue = entry.getKey();
      for (final Handler handler : entry.getValue()) {
        this.executor.scheduleWithFixedDelay(
            () -> this.runOne(queue, handler), initDelay, period, MILLISECONDS);
      }
    }
  }

  private void runOne(final String queue, final Handler handler) {
    try {
      final Optional<byte[]> bytesOpt = this.template.receive(queue);
      if (bytesOpt.isEmpty()) {
        return;
      }
      final byte[] messageBody = bytesOpt.get();
      this.invokeWithRetry(queue, handler, messageBody);
    } catch (final Exception e) {
      LOGGER.warn("Listener invoke failed for {}: {}", queue, e.toString());
    }
  }

  private void invokeWithRetry(
      final String queue, final Handler handler, final byte[] messageBody) {
    final int maxAttempts = handler.maxRetries() + RETRY_ATTEMPT_OFFSET;
    int attempt = 0;
    Exception lastException = null;

    while (attempt < maxAttempts) {
      try {
        final Method method = handler.method();
        if (method.getParameterCount() == 0) {
          method.invoke(handler.bean());
          return;
        }
        final Class<?> paramType = method.getParameterTypes()[0];
        this.mapAndInvoke(messageBody, paramType, method, handler);
        return;
      } catch (final Exception e) {
        attempt++;
        lastException = e;
        LOGGER.warn(
            "Attempt {}/{} failed for queue '{}': {}", attempt, maxAttempts, queue, e.toString());
      }
    }

    this.handleExhaustedRetries(queue, handler, messageBody, lastException);
  }

  private void handleExhaustedRetries(
      final String queue, final Handler handler, final byte[] messageBody, final Exception cause) {
    final String dlq = handler.deadLetterQueue();
    if (dlq != null && !dlq.isBlank()) {
      LOGGER.warn("Max retries exhausted for queue '{}'. Routing to DLQ '{}'.", queue, dlq);
      this.dlqRouter.route(dlq, messageBody);
    } else {
      LOGGER.warn(
          "Max retries exhausted for queue '{}'. No DLQ configured, message dropped: {}",
          queue,
          cause != null ? cause.toString() : "unknown");
    }
  }

  private void mapAndInvoke(
      final byte[] body, final Class<?> paramType, final Method method, final Handler handler)
      throws InvocationTargetException, IllegalAccessException {
    if (body == null) {
      return;
    }

    if (paramType.isPrimitive()) {
      throw new IllegalArgumentException("Primitive param unsupported: " + paramType);
    }

    final Object arg;
    if (paramType == byte[].class) {
      arg = body;
    } else if (isStringable(paramType)) {
      arg = new String(body, StandardCharsets.UTF_8);
    } else {
      if (!looksLikeJson(body)) {
        return;
      }
      try {
        arg = objectMapper.readValue(body, objectMapper.constructType(paramType));
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        LOGGER.debug("JSON parse failed for {}: {}", paramType, e.getMessage());
        return;
      } catch (java.io.IOException e) {
        LOGGER.error("IO Exception for {}: {}", paramType, e.getMessage());
        throw new RuntimeException(e);
      }
    }

    method.invoke(handler.bean(), arg);
  }

  private boolean isStringable(final Class<?> t) {
    return t == Object.class || CharSequence.class.isAssignableFrom(t);
  }

  private boolean looksLikeJson(final byte[] body) {
    int i = 0;
    final int n = body.length;

    while (i < n) {
      final byte b = body[i];
      if (b == SPACE || b == HORIZONTAL_TAB || b == LINE_FEED || b == CARRIAGE_RETURN) {
        i++;
        continue;
      }
      return b == '{'
          || b == '['
          || b == '"'
          || b == '-'
          || (b >= '0' && b <= '9')
          || b == 't'
          || b == 'f'
          || b == 'n';
    }
    return false;
  }

  @Override
  public void close() {
    if (this.executor != null) {
      this.executor.shutdown();
    }
  }
}
