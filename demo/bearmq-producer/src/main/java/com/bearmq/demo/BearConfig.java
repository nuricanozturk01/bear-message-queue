package com.bearmq.demo;

import com.bearmq.client.model.BearBinding;
import com.bearmq.client.model.BearExchange;
import com.bearmq.client.model.BearQueue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the <em>full</em> routing graph used by {@link MessageSender}. The broker only enqueues
 * published payloads when it can resolve at least one target queue; bindings that exist only in a
 * separate consumer app are not visible until that app registers, so the producer would otherwise
 * “send into the void” if it starts first.
 */
@Configuration
public class BearConfig {
  @Bean
  BearExchange exchangeA() {
    return new BearExchange.Builder()
            .name("exchangeA")
            .type(BearExchange.Type.FANOUT)
            .durable(true)
            .build();
  }

  @Bean
  BearExchange exchangeB() {
    return new BearExchange.Builder()
            .name("exchangeB")
            .type(BearExchange.Type.FANOUT)
            .durable(true)
            .build();
  }

  @Bean
  BearQueue queueZ() {
    return new BearQueue.Builder().name("queueZ").durable(true).build();
  }

  @Bean
  BearQueue queueT() {
    return new BearQueue.Builder().name("queueT").durable(true).build();
  }

  @Bean
  BearQueue queueX() {
    return new BearQueue.Builder().name("queueX").durable(true).build();
  }

  @Bean
  BearQueue queueY() {
    return new BearQueue.Builder().name("queueY").durable(true).build();
  }

  @Bean
  BearBinding bindingExchangeAToB(
      @Qualifier("exchangeA") final BearExchange exchangeA,
      @Qualifier("exchangeB") final BearExchange exchangeB) {
    return new BearBinding.Builder()
        .exchange(exchangeA.name())
        .destination(exchangeB.name())
        .destinationType(BearBinding.DestinationType.EXCHANGE)
        .build();
  }

  @Bean
  BearBinding bindExchangeAQueueZ() {
    return new BearBinding.Builder()
        .exchange("exchangeA")
        .destination("queueZ")
        .destinationType(BearBinding.DestinationType.QUEUE)
        .build();
  }

  @Bean
  BearBinding bindExchangeAQueueT() {
    return new BearBinding.Builder()
        .exchange("exchangeA")
        .destination("queueT")
        .destinationType(BearBinding.DestinationType.QUEUE)
        .build();
  }

  @Bean
  BearBinding bindExchangeBQueueX() {
    return new BearBinding.Builder()
        .exchange("exchangeB")
        .destination("queueX")
        .destinationType(BearBinding.DestinationType.QUEUE)
        .build();
  }

  @Bean
  BearBinding bindExchangeBQueueY() {
    return new BearBinding.Builder()
        .exchange("exchangeB")
        .destination("queueY")
        .destinationType(BearBinding.DestinationType.QUEUE)
        .build();
  }
}
