package com.bearmq.shared.exchange;

import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.RandomStringUtils.secure;

import com.bearmq.shared.broker.Status;
import com.bearmq.shared.broker.dto.ExchangeRequest;
import com.bearmq.shared.converter.BrokerConverter;
import com.bearmq.shared.vhost.VirtualHost;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExchangeService {

  private static final int MIN_DIGITS = 8;
  private static final int MAX_DIGITS = 30;

  private final ExchangeRepository exchangeRepository;
  private final BrokerConverter brokerConverter;
  private final Random random;

  public List<Exchange> createAll(final VirtualHost vhost, final List<ExchangeRequest> exchanges) {

    final List<Exchange> exchangeObjects =
        exchanges.stream().map(this.brokerConverter::toExchange).toList();

    final Set<String> existingNames =
        this.exchangeRepository.findAllByVhostId(vhost.getId()).stream()
            .map(Exchange::getName)
            .collect(Collectors.toSet());

    for (final Exchange exchange : exchangeObjects) {
      if (existingNames.contains(exchange.getName())) {
        continue;
      }
      final String actualName =
          String.format(
              "exchange-%s",
              secure()
                  .next(this.random.nextInt(MIN_DIGITS, MAX_DIGITS), true, false)
                  .toLowerCase(ROOT));

      exchange.setId(UlidCreator.getUlid().toString());
      exchange.setVhost(vhost);
      exchange.setActualName(actualName);
      exchange.setStatus(Status.ACTIVE);
    }

    return this.exchangeRepository.saveAll(
        exchangeObjects.stream().filter(q -> !existingNames.contains(q.getName())).toList());
  }

  public List<Exchange> findAllByVhostId(final String id) {

    return this.exchangeRepository.findListByVhostId(id);
  }

  @Transactional
  public void softDeleteById(final String vhostId, final String exchangeId) {

    final Exchange e =
        this.exchangeRepository
            .findById(exchangeId)
            .orElseThrow(() -> new IllegalArgumentException("Exchange not found"));
    if (e.getVhost() == null || !vhostId.equals(e.getVhost().getId())) {
      throw new IllegalArgumentException("Exchange not in this virtual host");
    }
    e.setDeleted(true);
    this.exchangeRepository.save(e);
  }
}
