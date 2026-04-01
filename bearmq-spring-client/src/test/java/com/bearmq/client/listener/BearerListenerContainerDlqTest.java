package com.bearmq.client.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bearmq.client.BearMessagingTemplate;
import com.bearmq.client.config.BearConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BearerListenerContainerDlqTest {

  private static final String QUEUE_NAME = "test-queue";
  private static final String DLQ_NAME = "dead-letter-queue";
  private static final int MAX_RETRIES = 2;
  private static final int POLL_TIMEOUT_SECONDS = 5;

  private BearMessagingTemplate template;
  private DeadLetterQueueRouter dlqRouter;
  private BearerListenerContainer container;

  @BeforeEach
  void setUp() {
    this.template = mock(BearMessagingTemplate.class);
    this.dlqRouter = mock(DeadLetterQueueRouter.class);
    final BearConfig config = new BearConfig();
    config.setPeriodMs(50);
    config.setInitialDelayMs(0);
    this.container =
        new BearerListenerContainer(new ObjectMapper(), this.template, config, this.dlqRouter);
  }

  @AfterEach
  void tearDown() {
    this.container.close();
  }

  @Test
  void routesToDlq_afterMaxRetriesExhausted() throws Exception {
    final byte[] payload = "failing-message".getBytes();
    when(this.template.receive(QUEUE_NAME))
        .thenReturn(Optional.of(payload))
        .thenReturn(Optional.empty());

    final CountDownLatch dlqLatch = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              dlqLatch.countDown();
              return null;
            })
        .when(this.dlqRouter)
        .route(eq(DLQ_NAME), any());

    final AtomicInteger invokeCount = new AtomicInteger(0);
    final FailingBean bean = new FailingBean(invokeCount);
    final Method method = FailingBean.class.getMethod("handle", byte[].class);
    final Handler handler = new Handler(bean, method, MAX_RETRIES, DLQ_NAME);

    this.container.register(Map.of(QUEUE_NAME, List.of(handler)));
    this.container.start();

    final boolean dlqRouted = dlqLatch.await(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    assertThat(dlqRouted).isTrue();
    verify(this.dlqRouter).route(eq(DLQ_NAME), eq(payload));
    assertThat(invokeCount.get()).isEqualTo(MAX_RETRIES + 1);
  }

  @Test
  void doesNotRouteToDlq_whenHandlerSucceeds() throws Exception {
    final byte[] payload = "success-message".getBytes();
    when(this.template.receive(QUEUE_NAME))
        .thenReturn(Optional.of(payload))
        .thenReturn(Optional.empty());

    final CountDownLatch successLatch = new CountDownLatch(1);
    final SucceedingBean bean = new SucceedingBean(successLatch);
    final Method method = SucceedingBean.class.getMethod("handle", byte[].class);
    final Handler handler = new Handler(bean, method, MAX_RETRIES, DLQ_NAME);

    this.container.register(Map.of(QUEUE_NAME, List.of(handler)));
    this.container.start();

    final boolean succeeded = successLatch.await(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    assertThat(succeeded).isTrue();
    verify(this.dlqRouter, never()).route(any(), any());
  }

  @Test
  void doesNotRouteToDlq_whenNoDlqConfigured() throws Exception {
    final byte[] payload = "failing-message".getBytes();
    when(this.template.receive(QUEUE_NAME))
        .thenReturn(Optional.of(payload))
        .thenReturn(Optional.empty());

    final int retries = 1;
    final AtomicInteger invokeCount = new AtomicInteger(0);
    final FailingBean bean = new FailingBean(invokeCount);
    final Method method = FailingBean.class.getMethod("handle", byte[].class);
    final Handler handler = new Handler(bean, method, retries, "");

    this.container.register(Map.of(QUEUE_NAME, List.of(handler)));
    this.container.start();

    verify(this.dlqRouter, after(500).never()).route(any(), any());
  }

  @Test
  void retriesCorrectNumberOfTimes_beforeRoutingToDlq() throws Exception {
    final byte[] payload = "retry-message".getBytes();
    when(this.template.receive(QUEUE_NAME))
        .thenReturn(Optional.of(payload))
        .thenReturn(Optional.empty());

    final int retries = 3;
    final CountDownLatch dlqLatch = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              dlqLatch.countDown();
              return null;
            })
        .when(this.dlqRouter)
        .route(eq(DLQ_NAME), any());

    final AtomicInteger invokeCount = new AtomicInteger(0);
    final FailingBean bean = new FailingBean(invokeCount);
    final Method method = FailingBean.class.getMethod("handle", byte[].class);
    final Handler handler = new Handler(bean, method, retries, DLQ_NAME);

    this.container.register(Map.of(QUEUE_NAME, List.of(handler)));
    this.container.start();

    dlqLatch.await(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    verify(this.dlqRouter, times(1)).route(eq(DLQ_NAME), eq(payload));
    assertThat(invokeCount.get()).isEqualTo(retries + 1);
  }

  @Test
  void noMessageDequeued_doesNotInvokeHandlerOrDlq() throws Exception {
    when(this.template.receive(QUEUE_NAME)).thenReturn(Optional.empty());

    final CountDownLatch neverLatch = new CountDownLatch(1);
    final SucceedingBean bean = new SucceedingBean(neverLatch);
    final Method method = SucceedingBean.class.getMethod("handle", byte[].class);
    final Handler handler = new Handler(bean, method, MAX_RETRIES, DLQ_NAME);

    this.container.register(Map.of(QUEUE_NAME, List.of(handler)));
    this.container.start();

    verify(this.dlqRouter, after(200).never()).route(any(), any());
    assertThat(neverLatch.getCount()).isEqualTo(1);
  }

  public static final class FailingBean {
    private final AtomicInteger invokeCount;

    public FailingBean(final AtomicInteger invokeCount) {
      this.invokeCount = invokeCount;
    }

    public void handle(final byte[] body) {
      final int attempt = this.invokeCount.incrementAndGet();
      throw new RuntimeException("Simulated processing failure on attempt " + attempt);
    }
  }

  public static final class SucceedingBean {
    private final CountDownLatch latch;

    public SucceedingBean(final CountDownLatch latch) {
      this.latch = latch;
    }

    public void handle(final byte[] body) {
      this.latch.countDown();
    }
  }
}
