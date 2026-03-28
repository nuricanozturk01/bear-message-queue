package com.bearmq.client;

import com.bearmq.client.config.BearConfig;
import com.bearmq.client.dto.BrokerForm;
import com.bearmq.client.dto.BrokerForm.BindingForm;
import com.bearmq.client.dto.BrokerForm.ExchangeForm;
import com.bearmq.client.dto.BrokerForm.QueueForm;
import com.bearmq.client.model.BearBinding;
import com.bearmq.client.model.BearExchange;
import com.bearmq.client.model.BearQueue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BrokerRegisterer implements SmartInitializingSingleton {
  private static final String API_KEY_HEADER = "X-API-KEY";

  private final List<BearQueue> queues;
  private final List<BearExchange> exchanges;
  private final List<BearBinding> bindings;
  private final BearConfig props;
  private final RestClient rest;

  public BrokerRegisterer(
      final List<BearQueue> queues,
      final List<BearExchange> exchanges,
      final List<BearBinding> bindings,
      final BearConfig props) {
    this.queues = queues;
    this.exchanges = exchanges;
    this.bindings = bindings;
    this.props = props;
    this.rest = RestClient.builder().baseUrl(props.getUrl()).build();
  }

  @Override
  public void afterSingletonsInstantiated() {
    final List<ExchangeForm> exDtos =
        exchanges.stream()
            .map(
                ex ->
                    new ExchangeForm(
                        ex.name(),
                        ex.type().name(),
                        ex.durable(),
                        ex.internal(),
                        ex.delayed(),
                        Optional.ofNullable(ex.arguments()).orElse(Map.of())))
            .toList();

    final List<QueueForm> qDtos =
        queues.stream()
            .map(
                q ->
                    new QueueForm(
                        q.name(),
                        q.durable(),
                        q.exclusive(),
                        q.autoDelete(),
                        Optional.ofNullable(q.arguments()).orElse(Map.of())))
            .toList();

    final List<BindingForm> bDtos =
        bindings.stream()
            .map(
                b ->
                    new BindingForm(
                        b.getExchange(),
                        b.getDestinationType().name(),
                        b.getDestination(),
                        Optional.ofNullable(b.getRoutingKey()).orElse("")))
            .toList();

    final BrokerForm payload = new BrokerForm(props.getVirtualHost(), exDtos, qDtos, bDtos);

    rest.post()
        .uri("/api/broker")
        .header(API_KEY_HEADER, props.getApiKey())
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }
}
