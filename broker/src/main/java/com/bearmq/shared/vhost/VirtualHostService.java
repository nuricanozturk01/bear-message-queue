package com.bearmq.shared.vhost;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.RandomStringUtils.secure;

import com.bearmq.shared.binding.BindingRepository;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.converter.BrokerConverter;
import com.bearmq.shared.event.VirtualHostActivatedEvent;
import com.bearmq.shared.event.VirtualHostDeletedEvent;
import com.bearmq.shared.exchange.ExchangeRepository;
import com.bearmq.shared.queue.QueueRepository;
import com.bearmq.shared.tenant.Tenant;
import com.bearmq.shared.tenant.TenantRepository;
import com.bearmq.shared.tenant.dto.TenantInfo;
import com.bearmq.shared.vhost.dto.VirtualHostInfo;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualHostService {

  private static final int MIN_DIGITS = 8;
  private static final int MAX_DIGITS = 30;

  private final VirtualHostRepository repository;
  private final BrokerConverter converter;
  private final TenantRepository tenantRepository;
  private final Random random;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final BindingRepository bindingRepository;
  private final QueueRepository queueRepository;
  private final ExchangeRepository exchangeRepository;

  @Value("${bearmq.domain}")
  private String domain;

  @Transactional
  public VirtualHostInfo create(final TenantInfo tenantInfo) {

    final Tenant tenant =
        this.tenantRepository
            .findByUsername(tenantInfo.username())
            .orElseThrow(() -> new RuntimeException("Tenant Not Found"));

    final int randomDigit = this.random.nextInt(MIN_DIGITS, MAX_DIGITS);

    final String vhostDomain =
        String.format(
            "%s.%s", secure().next(randomDigit, true, false).toLowerCase(ROOT), this.domain);

    final String username = secure().next(randomDigit, true, false).toLowerCase(ROOT);

    final String password = secure().next(randomDigit, true, false);
    final String encodedPassword =
        Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));

    final String name =
        format(
            "%s-%s",
            tenantInfo.username(), secure().next(randomDigit, true, false).toLowerCase(ROOT));

    final VirtualHost vhostObj = new VirtualHost();

    vhostObj.setName(name);
    vhostObj.setUsername(username);
    vhostObj.setPassword(encodedPassword);
    vhostObj.setDomain(vhostDomain);
    vhostObj.setUrl(vhostDomain);
    vhostObj.setId(UlidCreator.getUlid().toString());
    vhostObj.setTenant(tenant);
    vhostObj.setStatus(Status.ACTIVE);
    vhostObj.setDeleted(false);

    this.repository.save(vhostObj);

    log.info(
        "VirtualHost created: id={} name={} tenantId={}",
        vhostObj.getId(),
        vhostObj.getName(),
        tenantInfo.id());

    return this.converter.toVirtualHostInfo(vhostObj);
  }

  @Transactional
  public VirtualHostInfo updateStatus(final String vhostId, final Status newStatus) {

    final VirtualHost vhost =
        this.repository
            .findById(vhostId)
            .filter(v -> !v.isDeleted())
            .orElseThrow(() -> new RuntimeException("vhost is not found!"));
    vhost.setStatus(newStatus);
    this.repository.save(vhost);
    if (newStatus == Status.ACTIVE) {
      this.applicationEventPublisher.publishEvent(
          new VirtualHostActivatedEvent(vhostId, Instant.now()));
    } else {
      this.applicationEventPublisher.publishEvent(
          new VirtualHostDeletedEvent(vhost.getTenant().getId(), vhostId, Instant.now(), false));
      log.info(
          "AUDIT vhost_deleted tenantId={} virtualHostId={}", vhost.getTenant().getId(), vhostId);
    }
    return this.converter.toVirtualHostInfo(vhost);
  }

  @Transactional(readOnly = true)
  public VirtualHost requireEntityById(final String vhostId) {

    return this.repository
        .findById(vhostId)
        .filter(v -> !v.isDeleted())
        .orElseThrow(() -> new RuntimeException("vhost is not found!"));
  }

  @Transactional(readOnly = true)
  public Page<VirtualHostInfo> findAllActive(final @NotNull Pageable pageable) {

    final Pageable effective =
        pageable.getSort().isSorted()
            ? pageable
            : PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending());

    final Page<VirtualHost> raw = this.repository.findAllByDeletedFalse(effective);
    final List<VirtualHostInfo> vhosts =
        raw.stream().map(this.converter::toVirtualHostInfo).toList();
    return new PageImpl<>(vhosts, effective, raw.getTotalElements());
  }

  @Transactional(readOnly = true)
  public VirtualHost findByVhostName(final String vhostName) {

    return this.repository
        .findByNameAndDeletedFalse(vhostName)
        .orElseThrow(() -> new RuntimeException("vhost is not found!"));
  }

  @Transactional(readOnly = true)
  public VirtualHost findByVhostInfo(
      final String vhost, final String username, final String password) {
    return this.repository
        .findActiveByCredentials(vhost, username, password)
        .orElseThrow(() -> new RuntimeException("vhost is not found!"));
  }

  @Transactional(readOnly = true)
  public List<VirtualHost> findAll() {

    return this.repository.findAll();
  }

  @Transactional
  public void markDeleted(final VirtualHost vhost) {

    if (vhost.isDeleted()) {
      return;
    }
    final String vhostId = vhost.getId();
    this.bindingRepository.softDeleteAllForVhost(vhostId);
    this.queueRepository.softDeleteAllForVhost(vhostId);
    this.exchangeRepository.softDeleteAllForVhost(vhostId);

    vhost.setDeleted(true);
    this.repository.save(vhost);
    this.applicationEventPublisher.publishEvent(
        new VirtualHostDeletedEvent(vhost.getTenant().getId(), vhostId, Instant.now(), true));
    log.info(
        "AUDIT vhost_deleted tenantId={} virtualHostId={}", vhost.getTenant().getId(), vhostId);
  }
}
