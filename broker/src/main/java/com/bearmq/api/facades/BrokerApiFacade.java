package com.bearmq.api.facades;

import com.bearmq.api.broker.dtos.BrokerRequest;
import com.bearmq.api.common.exceptions.ForbiddenException;
import com.bearmq.shared.binding.Binding;
import com.bearmq.shared.binding.BindingService;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.broker.runtime.BrokerRuntimePort;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.exchange.ExchangeService;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.queue.QueueService;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.dto.TenantInfo;
import com.bearmq.shared.vhost.VirtualHost;
import com.bearmq.shared.vhost.VirtualHostService;
import com.bearmq.shared.vhost.dto.VirtualHostInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerApiFacade {

  private final VirtualHostService virtualHostService;
  private final ExchangeService exchangeService;
  private final QueueService queueService;
  private final BindingService bindingService;
  private final BrokerRuntimePort brokerRuntime;

  @Transactional
  public void createBrokerObjects(final BrokerRequest request, final TenantInfo tenantInfo) {

    final VirtualHost vhost = this.virtualHostService.findByVhostName(request.vhost());

    if (vhost.getStatus() != Status.ACTIVE) {
      throw new IllegalArgumentException("Virtual host is not active");
    }

    if (!request.queues().isEmpty()) {
      this.queueService.createAll(vhost, request.queues());
    }

    if (!request.exchanges().isEmpty()) {
      this.exchangeService.createAll(vhost, request.exchanges());
    }

    final List<Queue> allQueues = this.queueService.findAllByVhostId(vhost.getId());
    final List<Exchange> allExchanges = this.exchangeService.findAllByVhostId(vhost.getId());

    List<Binding> bindings = List.of();

    if (!request.bindings().isEmpty()) {
      bindings = this.bindingService.createAll(vhost, allExchanges, allQueues, request.bindings());
    }

    this.brokerRuntime.reloadVhostRuntime(vhost.getId());

    log.info(
        "Topology applied: tenantId={} vhostId={} queues={} exchanges={} bindings={}",
        tenantInfo.id(),
        vhost.getId(),
        allQueues.size(),
        allExchanges.size(),
        bindings.size());
  }

  public VirtualHostInfo createVirtualHost(final TenantInfo tenantInfo) {

    return this.virtualHostService.create(tenantInfo);
  }

  @Transactional
  public VirtualHostInfo updateVirtualHostStatus(final String vhostId, final Status status) {

    return this.virtualHostService.updateStatus(vhostId, status);
  }

  @Transactional
  public void deleteQueue(final String vhostId, final String queueId) {

    this.queueService.softDeleteById(vhostId, queueId);
    this.brokerRuntime.reloadVhostRuntime(vhostId);
  }

  @Transactional
  public void deleteExchange(final String vhostId, final String exchangeId) {

    this.exchangeService.softDeleteById(vhostId, exchangeId);
    this.brokerRuntime.reloadVhostRuntime(vhostId);
  }

  @Transactional
  public void deleteBinding(final String vhostId, final String bindingId) {

    this.bindingService.softDeleteById(vhostId, bindingId);
    this.brokerRuntime.reloadVhostRuntime(vhostId);
  }

  @Transactional
  public void deleteVirtualHost(final String vhostId, final TenantInfo tenant) {

    final VirtualHost vhost = this.virtualHostService.requireEntityById(vhostId);
    final boolean admin = tenant.role() == TenantRole.ADMIN;
    if (!admin && !vhost.getTenant().getId().equals(tenant.id())) {
      throw new ForbiddenException("You cannot delete this instance.");
    }
    this.virtualHostService.markDeleted(vhost);
  }

  public Page<VirtualHostInfo> findAllVhosts(final Pageable pageable) {

    return this.virtualHostService.findAllActive(pageable);
  }
}
