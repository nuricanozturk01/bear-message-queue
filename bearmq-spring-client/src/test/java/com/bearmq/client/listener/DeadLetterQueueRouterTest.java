package com.bearmq.client.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bearmq.client.BearMQException;
import com.bearmq.client.BearTemplate;
import com.bearmq.client.dto.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeadLetterQueueRouterTest {

  private BearTemplate template;
  private DeadLetterQueueRouter router;

  @BeforeEach
  void setUp() {
    this.template = mock(BearTemplate.class);
    this.router = new DeadLetterQueueRouter(this.template);
  }

  @Test
  void route_sendsMessageBodyToDeadLetterQueue() throws BearMQException {
    final byte[] body = "test-body".getBytes();
    final String dlq = "dead-letter-queue";

    this.router.route(dlq, body);

    verify(this.template).send(eq(dlq), any(Message.class));
  }

  @Test
  void route_doesNotThrow_whenTemplateThrowsBearMQException() throws BearMQException {
    final byte[] body = "test-body".getBytes();
    final String dlq = "dead-letter-queue";
    doThrow(new BearMQException("send failed", new RuntimeException()))
        .when(this.template)
        .send(eq(dlq), any(Message.class));

    this.router.route(dlq, body);

    verify(this.template).send(eq(dlq), any(Message.class));
  }

  @Test
  void route_sendsMessage_whenBodyIsEmpty() throws BearMQException {
    final byte[] body = new byte[0];
    final String dlq = "dead-letter-queue";

    this.router.route(dlq, body);

    verify(this.template).send(eq(dlq), any(Message.class));
  }

  @Test
  void route_callsSendWithCorrectQueue() throws BearMQException {
    final byte[] body = "payload".getBytes();
    final String dlq = "my-dlq";

    this.router.route(dlq, body);

    verify(this.template).send(eq("my-dlq"), any(Message.class));
  }

  @Test
  void route_sendsExactMessageBodyToDlq() throws BearMQException {
    final byte[] body = "exact-payload".getBytes();
    final String dlq = "dead-letter-queue";

    this.router.route(dlq, body);

    verify(this.template).send(eq(dlq), argThat(msg -> assertBodyMatches(msg, body)));
  }

  private static boolean assertBodyMatches(final Message msg, final byte[] expected) {
    assertThat(msg.body()).isEqualTo(expected);
    return true;
  }
}
