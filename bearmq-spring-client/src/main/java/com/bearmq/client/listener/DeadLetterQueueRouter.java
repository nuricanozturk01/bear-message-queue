package com.bearmq.client.listener;

import com.bearmq.client.BearMQException;
import com.bearmq.client.BearTemplate;
import com.bearmq.client.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class DeadLetterQueueRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeadLetterQueueRouter.class);

  private final BearTemplate template;

  public DeadLetterQueueRouter(final BearTemplate template) {
    this.template = template;
  }

  public void route(final String deadLetterQueue, final byte[] messageBody) {
    try {
      this.template.send(deadLetterQueue, new Message(messageBody));
    } catch (final BearMQException e) {
      LOGGER.error(
          "Failed to route message to dead letter queue '{}': {}",
          deadLetterQueue,
          e.getMessage(),
          e);
    }
  }
}
