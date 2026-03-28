package com.bearmq.api.facades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bearmq.api.broker.dtos.read.QueuePeekResponseDto;
import com.bearmq.api.broker.dtos.read.QueueSummaryDto;
import com.bearmq.api.broker.mapper.BrokerReadMapper;
import com.bearmq.shared.binding.BindingRepository;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.broker.runtime.BrokerRuntimePort;
import com.bearmq.shared.broker.runtime.QueuePeekResult;
import com.bearmq.shared.exchange.ExchangeRepository;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.queue.QueueRepository;
import com.bearmq.shared.vhost.VirtualHost;
import com.bearmq.shared.vhost.VirtualHostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrokerReadFacadeTest {

  @Mock private VirtualHostRepository virtualHostRepository;
  @Mock private QueueRepository queueRepository;
  @Mock private ExchangeRepository exchangeRepository;
  @Mock private BindingRepository bindingRepository;
  @Mock private BrokerReadMapper brokerReadMapper;
  @Mock private BrokerRuntimePort brokerRuntime;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private BrokerReadFacade brokerReadFacade;

  @BeforeEach
  void objectMapperDelegatesToRealParse() throws Exception {

    lenient()
        .when(this.objectMapper.readTree(any(byte[].class)))
        .thenAnswer(inv -> new ObjectMapper().readTree((byte[]) inv.getArgument(0)));
  }

  private static VirtualHost vhost(final String id) {

    final VirtualHost v = new VirtualHost();
    v.setId(id);
    v.setName("vhost-" + id);
    v.setDeleted(false);
    return v;
  }

  @Test
  void listQueues_forExistingVhost_returnsQueues() {
    when(this.brokerReadMapper.toQueueSummary(any(Queue.class)))
        .thenAnswer(
            inv -> {
              final Queue qq = inv.getArgument(0);
              return new QueueSummaryDto(
                  qq.getId(),
                  qq.getName(),
                  qq.getActualName() == null ? "" : qq.getActualName(),
                  qq.isDurable(),
                  qq.isExclusive(),
                  qq.isAutoDelete(),
                  qq.getStatus() != null ? qq.getStatus().name() : "");
            });

    final VirtualHost v = vhost("vhost1");
    when(virtualHostRepository.findById("vhost1")).thenReturn(Optional.of(v));

    final Queue q = new Queue();
    q.setId("q1");
    q.setName("my-queue");
    q.setActualName("queue-abc");
    q.setDurable(true);
    q.setDeleted(false);
    q.setStatus(Status.ACTIVE);

    when(queueRepository.findAllByVhostId("vhost1")).thenReturn(List.of(q));

    final List<QueueSummaryDto> result = brokerReadFacade.listQueues("vhost1");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("my-queue");
    assertThat(result.get(0).status()).isEqualTo("ACTIVE");
  }

  @Test
  void listQueues_forMissingVhost_throwsIllegalArgument() {
    when(virtualHostRepository.findById("other-vhost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> brokerReadFacade.listQueues("other-vhost"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void listQueues_excludesDeletedQueues() {
    when(this.brokerReadMapper.toQueueSummary(any(Queue.class)))
        .thenAnswer(
            inv -> {
              final Queue qq = inv.getArgument(0);
              return new QueueSummaryDto(
                  qq.getId(),
                  qq.getName(),
                  qq.getActualName() == null ? "" : qq.getActualName(),
                  qq.isDurable(),
                  qq.isExclusive(),
                  qq.isAutoDelete(),
                  qq.getStatus() != null ? qq.getStatus().name() : "");
            });

    final VirtualHost v = vhost("vhost1");
    when(virtualHostRepository.findById("vhost1")).thenReturn(Optional.of(v));

    final Queue deleted = new Queue();
    deleted.setId("q2");
    deleted.setName("deleted-queue");
    deleted.setDeleted(true);

    final Queue active = new Queue();
    active.setId("q3");
    active.setName("active-queue");
    active.setDeleted(false);
    active.setStatus(Status.ACTIVE);

    when(queueRepository.findAllByVhostId("vhost1")).thenReturn(List.of(deleted, active));

    final List<QueueSummaryDto> result = brokerReadFacade.listQueues("vhost1");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("active-queue");
  }

  @Test
  void peekQueue_returnsParsedJsonAndDoesNotRequireMapperBeyondReadTree() {

    final VirtualHost v = vhost("vhost1");
    when(this.virtualHostRepository.findById("vhost1")).thenReturn(Optional.of(v));

    final Queue q = new Queue();
    q.setId("q1");
    q.setName("jobs");
    q.setDeleted(false);
    q.setVhost(v);
    when(this.queueRepository.findById("q1")).thenReturn(Optional.of(q));

    when(this.brokerRuntime.getLoadedQueueNames("vhost1")).thenReturn(Set.of("jobs"));
    when(this.brokerRuntime.peekPendingMessages(eq("vhost1"), eq("jobs"), eq(5)))
        .thenReturn(
            new QueuePeekResult(
                List.of("{\"orderId\":42}".getBytes(StandardCharsets.UTF_8)), false));

    final QueuePeekResponseDto out = this.brokerReadFacade.peekQueue("vhost1", "q1");

    assertThat(out.runtimeLoaded()).isTrue();
    assertThat(out.queueName()).isEqualTo("jobs");
    assertThat(out.truncated()).isFalse();
    assertThat(out.messages()).hasSize(1);
    assertThat(out.messages().get(0).sequence()).isZero();
    assertThat(out.messages().get(0).json().get("orderId").asInt()).isEqualTo(42);
  }
}
