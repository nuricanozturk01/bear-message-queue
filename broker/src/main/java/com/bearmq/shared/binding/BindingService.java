package com.bearmq.shared.binding;

import com.bearmq.api.broker.dto.BindRequest;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.converter.BrokerConverter;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.vhost.VirtualHost;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BindingService {

  private final BindingRepository bindingRepository;
  private final BrokerConverter brokerConverter;

  public List<Binding> createAll(
      final VirtualHost vhost,
      final List<Exchange> exchanges,
      final List<Queue> queues,
      final List<BindRequest> bindings) {

    final Map<String, Exchange> exchangeByName =
        exchanges.stream().collect(Collectors.toMap(Exchange::getName, Function.identity()));

    final Map<String, Queue> queueByName =
        queues.stream().collect(Collectors.toMap(Queue::getName, Function.identity()));

    final Set<String> existingKeys =
        this.bindingRepository.findAllByVhostId(vhost.getId()).stream()
            .filter(b -> !b.isDeleted())
            .map(BindingService::existingKey)
            .collect(Collectors.toSet());

    final List<Binding> toPersist = new ArrayList<>(bindings.size());

    for (final BindRequest req : bindings) {
      final DestinationType destType = DestinationType.valueOf(req.destinationType());

      final Exchange sourceExchange =
          Optional.ofNullable(exchangeByName.get(req.source()))
              .orElseThrow(
                  () -> new RuntimeException("source exchange not found: " + req.source()));

      final String destinationId =
          this.resolveDestinationId(destType, queueByName, req, exchangeByName);
      final String key =
          buildKey(sourceExchange.getId(), destType, destinationId, req.routingKey());

      if (existingKeys.contains(key)) {
        continue;
      }

      final Binding b = this.brokerConverter.toBinding(req);
      b.setId(UlidCreator.getUlid().toString());
      b.setVhost(vhost);
      b.setStatus(Status.ACTIVE);
      b.setDestinationType(destType);
      b.setRoutingKey(req.routingKey());
      b.setSourceExchangeRef(sourceExchange);
      b.setSourceExchangeId(sourceExchange.getId());
      this.setDestinations(destType, queueByName, req, exchangeByName, b);

      toPersist.add(b);
      existingKeys.add(key);
    }

    if (toPersist.isEmpty()) {
      return List.of();
    }
    return this.bindingRepository.saveAll(toPersist);
  }

  public List<Binding> findAllByVhostId(final String vhostId) {
    return this.bindingRepository.findAllByVhostId(vhostId);
  }

  @Transactional
  public void softDeleteById(final String vhostId, final String bindingId) {
    final Binding b =
        this.bindingRepository
            .findById(bindingId)
            .orElseThrow(() -> new IllegalArgumentException("Binding not found"));
    if (b.getVhostRef() == null || !vhostId.equals(b.getVhostRef().getId())) {
      throw new IllegalArgumentException("Binding not in this virtual host");
    }
    b.setDeleted(true);
    this.bindingRepository.save(b);
  }

  private String resolveDestinationId(
      final DestinationType destType,
      final Map<String, Queue> queueByName,
      final BindRequest req,
      final Map<String, Exchange> exchangeByName) {
    if (destType == DestinationType.QUEUE) {
      return Optional.ofNullable(queueByName.get(req.destination()))
          .map(Queue::getId)
          .orElseThrow(
              () -> new RuntimeException("destination queue not found: " + req.destination()));
    }
    return Optional.ofNullable(exchangeByName.get(req.destination()))
        .map(Exchange::getId)
        .orElseThrow(
            () -> new RuntimeException("destination exchange not found: " + req.destination()));
  }

  private void setDestinations(
      final DestinationType destType,
      final Map<String, Queue> queueByName,
      final BindRequest req,
      final Map<String, Exchange> exchangeByName,
      final Binding b) {
    if (destType == DestinationType.QUEUE) {
      final Queue q =
          Optional.ofNullable(queueByName.get(req.destination()))
              .orElseThrow(
                  () -> new RuntimeException("destination queue not found: " + req.destination()));
      b.setDestinationQueueRef(q);
      b.setDestinationQueueId(q.getId());
    } else {
      final Exchange destEx =
          Optional.ofNullable(exchangeByName.get(req.destination()))
              .orElseThrow(
                  () ->
                      new RuntimeException("destination exchange not found: " + req.destination()));
      b.setDestinationExchangeRef(destEx);
      b.setDestinationExchangeId(destEx.getId());
    }
  }

  private static String existingKey(final Binding b) {
    final String destId =
        b.getDestinationType() == DestinationType.QUEUE
            ? b.getDestinationQueueId()
            : b.getDestinationExchangeId();
    return buildKey(b.getSourceExchangeId(), b.getDestinationType(), destId, b.getRoutingKey());
  }

  private static String buildKey(
      final String sourceId,
      final DestinationType destType,
      final String destId,
      final String routingKey) {
    return sourceId
        + ':'
        + destType.name()
        + ':'
        + Objects.toString(destId, "")
        + ':'
        + Objects.toString(routingKey, "");
  }
}
