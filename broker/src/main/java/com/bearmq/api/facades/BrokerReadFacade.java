package com.bearmq.api.facades;

import com.bearmq.api.broker.dtos.read.BindingSummaryDto;
import com.bearmq.api.broker.dtos.read.ExchangeSummaryDto;
import com.bearmq.api.broker.dtos.read.PeekedMessageDto;
import com.bearmq.api.broker.dtos.read.QueuePeekResponseDto;
import com.bearmq.api.broker.dtos.read.QueueSummaryDto;
import com.bearmq.api.broker.mapper.BrokerReadMapper;
import com.bearmq.shared.binding.BindingRepository;
import com.bearmq.shared.broker.runtime.BrokerRuntimePort;
import com.bearmq.shared.broker.runtime.QueuePeekResult;
import com.bearmq.shared.exchange.ExchangeRepository;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.queue.QueueRepository;
import com.bearmq.shared.vhost.VirtualHostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrokerReadFacade {

  private static final int PEEK_LAST_N = 5;

  private final VirtualHostRepository virtualHostRepository;
  private final QueueRepository queueRepository;
  private final ExchangeRepository exchangeRepository;
  private final BindingRepository bindingRepository;
  private final BrokerReadMapper brokerReadMapper;
  private final BrokerRuntimePort brokerRuntime;
  private final ObjectMapper objectMapper;

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

  /**
   * Reads the last five queue payloads before the consumer tail without dequeueing (no consumer
   * index update).
   */
  @Transactional(readOnly = true)
  public QueuePeekResponseDto peekQueue(final String vhostId, final String queueId) {

    this.requireVhost(vhostId);
    final Queue q =
        this.queueRepository
            .findById(queueId)
            .filter(queue -> !queue.isDeleted() && queue.getVhost().getId().equals(vhostId))
            .orElseThrow(() -> new IllegalArgumentException("Queue not found"));

    final boolean runtimeLoaded =
        this.brokerRuntime.getLoadedQueueNames(vhostId).contains(q.getName());
    final QueuePeekResult batch =
        this.brokerRuntime.peekPendingMessages(vhostId, q.getName(), PEEK_LAST_N);

    final List<PeekedMessageDto> messages = new ArrayList<>();
    int seq = 0;
    for (final byte[] raw : batch.messages()) {
      messages.add(this.toPeeked(seq++, raw));
    }
    return new QueuePeekResponseDto(runtimeLoaded, q.getName(), batch.truncated(), messages);
  }

  private PeekedMessageDto toPeeked(final int sequence, final byte[] body) {

    try {
      return new PeekedMessageDto(sequence, this.objectMapper.readTree(body), null, null);
    } catch (final Exception ignored) {
      try {
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(body));
        return new PeekedMessageDto(sequence, null, new String(body, StandardCharsets.UTF_8), null);
      } catch (final CharacterCodingException e) {
        return new PeekedMessageDto(sequence, null, null, Base64.getEncoder().encodeToString(body));
      }
    }
  }

  private void requireVhost(final String vhostId) {

    this.virtualHostRepository
        .findById(vhostId)
        .filter(v -> !v.isDeleted())
        .orElseThrow(() -> new IllegalArgumentException("Virtual host not found"));
  }
}
