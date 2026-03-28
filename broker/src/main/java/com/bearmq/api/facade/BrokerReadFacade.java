package com.bearmq.api.facade;

import com.bearmq.api.broker.dto.read.BindingSummaryDto;
import com.bearmq.api.broker.dto.read.ExchangeSummaryDto;
import com.bearmq.api.broker.dto.read.QueueSummaryDto;
import com.bearmq.shared.binding.Binding;
import com.bearmq.shared.binding.BindingRepository;
import com.bearmq.shared.binding.DestinationType;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.exchange.ExchangeRepository;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.queue.QueueRepository;
import com.bearmq.shared.vhost.VirtualHostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrokerReadFacade {

  private final VirtualHostRepository virtualHostRepository;
  private final QueueRepository queueRepository;
  private final ExchangeRepository exchangeRepository;
  private final BindingRepository bindingRepository;

  @Transactional(readOnly = true)
  public List<QueueSummaryDto> listQueues(final String vhostId) {
    this.requireVhost(vhostId);
    return this.queueRepository.findAllByVhostId(vhostId).stream()
        .filter(q -> !q.isDeleted())
        .map(BrokerReadFacade::toQueueSummary)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ExchangeSummaryDto> listExchanges(final String vhostId) {
    this.requireVhost(vhostId);
    return this.exchangeRepository.findListByVhostId(vhostId).stream()
        .filter(e -> !e.isDeleted())
        .map(BrokerReadFacade::toExchangeSummary)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BindingSummaryDto> listBindings(final String vhostId) {
    this.requireVhost(vhostId);
    return this.bindingRepository.findAllActiveForReadByVhostId(vhostId).stream()
        .map(BrokerReadFacade::toBindingSummary)
        .toList();
  }

  private void requireVhost(final String vhostId) {
    this.virtualHostRepository
        .findById(vhostId)
        .filter(v -> !v.isDeleted())
        .orElseThrow(() -> new IllegalArgumentException("Virtual host not found"));
  }

  private static QueueSummaryDto toQueueSummary(final Queue q) {
    return new QueueSummaryDto(
        q.getId(),
        q.getName(),
        q.getActualName(),
        q.isDurable(),
        q.isExclusive(),
        q.isAutoDelete(),
        q.getStatus() != null ? q.getStatus().name() : "");
  }

  private static ExchangeSummaryDto toExchangeSummary(final Exchange e) {
    return new ExchangeSummaryDto(
        e.getId(),
        e.getName(),
        e.getActualName(),
        e.getType() != null ? e.getType().name() : "",
        e.isDurable(),
        e.isInternal(),
        e.getStatus() != null ? e.getStatus().name() : "");
  }

  private static BindingSummaryDto toBindingSummary(final Binding b) {
    final String sourceName =
        b.getSourceExchangeRef() != null ? b.getSourceExchangeRef().getName() : "";
    final String destName = resolveDestinationName(b);
    return new BindingSummaryDto(
        b.getId(),
        sourceName,
        b.getDestinationType(),
        destName,
        b.getRoutingKey(),
        b.getStatus() != null ? b.getStatus().name() : "");
  }

  private static String resolveDestinationName(final Binding b) {
    if (b.getDestinationType() == DestinationType.EXCHANGE
        && b.getDestinationExchangeRef() != null) {
      return b.getDestinationExchangeRef().getName();
    }
    if (b.getDestinationQueueRef() != null) {
      return b.getDestinationQueueRef().getName();
    }
    return "";
  }
}
