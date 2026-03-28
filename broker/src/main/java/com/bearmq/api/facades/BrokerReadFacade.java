package com.bearmq.api.facades;

import com.bearmq.api.broker.dtos.read.BindingSummaryDto;
import com.bearmq.api.broker.dtos.read.ExchangeSummaryDto;
import com.bearmq.api.broker.dtos.read.QueueSummaryDto;
import com.bearmq.api.broker.mapper.BrokerReadMapper;
import com.bearmq.shared.binding.BindingRepository;
import com.bearmq.shared.exchange.ExchangeRepository;
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
  private final BrokerReadMapper brokerReadMapper;

  @Transactional(readOnly = true)
  public List<QueueSummaryDto> listQueues(final String vhostId) {

    this.requireVhost(vhostId);

    return this.queueRepository.findAllByVhostId(vhostId).stream()
        .filter(q -> !q.isDeleted())
        .map(this.brokerReadMapper::toQueueSummary)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ExchangeSummaryDto> listExchanges(final String vhostId) {

    this.requireVhost(vhostId);

    return this.exchangeRepository.findListByVhostId(vhostId).stream()
        .filter(e -> !e.isDeleted())
        .map(this.brokerReadMapper::toExchangeSummary)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<BindingSummaryDto> listBindings(final String vhostId) {

    this.requireVhost(vhostId);

    return this.bindingRepository.findAllActiveForReadByVhostId(vhostId).stream()
        .map(this.brokerReadMapper::toBindingSummary)
        .toList();
  }

  private void requireVhost(final String vhostId) {

    this.virtualHostRepository
        .findById(vhostId)
        .filter(v -> !v.isDeleted())
        .orElseThrow(() -> new IllegalArgumentException("Virtual host not found"));
  }
}
