package com.bearmq.api.broker.controllers;

import com.bearmq.api.auth.services.TenantContext;
import com.bearmq.api.broker.dtos.BrokerRequest;
import com.bearmq.api.broker.dtos.UpdateVhostStatusRequest;
import com.bearmq.api.broker.dtos.read.BindingSummaryDto;
import com.bearmq.api.broker.dtos.read.ExchangeSummaryDto;
import com.bearmq.api.broker.dtos.read.QueueSummaryDto;
import com.bearmq.api.facades.BrokerApiFacade;
import com.bearmq.api.facades.BrokerReadFacade;
import com.bearmq.shared.tenant.dto.TenantInfo;
import com.bearmq.shared.vhost.dto.VirtualHostInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/broker")
@RestController
@RequiredArgsConstructor
public class BearBrokerController {

  private final BrokerApiFacade brokerApiFacade;
  private final BrokerReadFacade brokerReadFacade;
  private final TenantContext tenantContext;

  @PostMapping
  public ResponseEntity<Boolean> create(@RequestBody final BrokerRequest brokerRequest) {

    final TenantInfo tenantInfo = this.tenantContext.requireTenant();
    this.brokerApiFacade.createBrokerObjects(brokerRequest, tenantInfo);
    return ResponseEntity.ok(true);
  }

  @PostMapping("/vhost")
  public ResponseEntity<VirtualHostInfo> createVhost() {

    final TenantInfo tenantInfo = this.tenantContext.requireTenant();
    final VirtualHostInfo vhost = this.brokerApiFacade.createVirtualHost(tenantInfo);
    return ResponseEntity.ok(vhost);
  }

  @PatchMapping("/vhost/{vhostId}/status")
  public ResponseEntity<VirtualHostInfo> updateVhostStatus(
      @PathVariable final String vhostId, @Valid @RequestBody final UpdateVhostStatusRequest body) {
    this.tenantContext.requireTenant();
    return ResponseEntity.ok(this.brokerApiFacade.updateVirtualHostStatus(vhostId, body.status()));
  }

  @GetMapping("/vhost")
  public ResponseEntity<Page<VirtualHostInfo>> listVhosts(
      @PageableDefault final @NotNull Pageable pageable) {
    this.tenantContext.requireTenant();
    final Page<VirtualHostInfo> vhosts = this.brokerApiFacade.findAllVhosts(pageable);
    return ResponseEntity.ok(vhosts);
  }

  @DeleteMapping("/vhost/{vhostId}")
  public ResponseEntity<Void> deleteVhost(@PathVariable final String vhostId) {

    final TenantInfo tenant = this.tenantContext.requireTenant();
    this.brokerApiFacade.deleteVirtualHost(vhostId, tenant);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/vhost/{vhostId}/queues")
  public ResponseEntity<List<QueueSummaryDto>> listQueues(@PathVariable final String vhostId) {

    this.tenantContext.requireTenant();
    return ResponseEntity.ok(this.brokerReadFacade.listQueues(vhostId));
  }

  @GetMapping("/vhost/{vhostId}/exchanges")
  public ResponseEntity<List<ExchangeSummaryDto>> listExchanges(
      @PathVariable final String vhostId) {
    this.tenantContext.requireTenant();
    return ResponseEntity.ok(this.brokerReadFacade.listExchanges(vhostId));
  }

  @GetMapping("/vhost/{vhostId}/bindings")
  public ResponseEntity<List<BindingSummaryDto>> listBindings(@PathVariable final String vhostId) {

    this.tenantContext.requireTenant();
    return ResponseEntity.ok(this.brokerReadFacade.listBindings(vhostId));
  }

  @DeleteMapping("/vhost/{vhostId}/queues/{queueId}")
  public ResponseEntity<Void> deleteQueue(
      @PathVariable final String vhostId, @PathVariable final String queueId) {
    this.tenantContext.requireTenant();
    this.brokerApiFacade.deleteQueue(vhostId, queueId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/vhost/{vhostId}/exchanges/{exchangeId}")
  public ResponseEntity<Void> deleteExchange(
      @PathVariable final String vhostId, @PathVariable final String exchangeId) {
    this.tenantContext.requireTenant();
    this.brokerApiFacade.deleteExchange(vhostId, exchangeId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/vhost/{vhostId}/bindings/{bindingId}")
  public ResponseEntity<Void> deleteBinding(
      @PathVariable final String vhostId, @PathVariable final String bindingId) {
    this.tenantContext.requireTenant();
    this.brokerApiFacade.deleteBinding(vhostId, bindingId);
    return ResponseEntity.noContent().build();
  }
}
