package com.bearmq.shared.converter;

import com.bearmq.api.broker.dto.BindRequest;
import com.bearmq.api.broker.dto.ExchangeRequest;
import com.bearmq.api.broker.dto.QueueRequest;
import com.bearmq.shared.binding.Binding;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.vhost.VirtualHost;
import com.bearmq.shared.vhost.dto.VirtualHostInfo;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class BrokerConverter {

  @Mapping(ignore = true, target = "arguments")
  public abstract Queue toQueue(QueueRequest queueRequest);

  @Mapping(ignore = true, target = "arguments")
  public abstract Binding toBinding(BindRequest bindRequest);

  @Mapping(ignore = true, target = "arguments")
  public abstract Exchange toExchange(ExchangeRequest exchangeRequest);

  public VirtualHostInfo convert(final VirtualHost vhost) {
    return new VirtualHostInfo(
        vhost.getId(),
        vhost.getName(),
        vhost.getUsername(),
        decodeBase64(vhost.getPassword()),
        vhost.getDomain(),
        vhost.getUrl(),
        vhost.getCreatedAt(),
        vhost.getStatus() != null ? vhost.getStatus().name() : "");
  }

  private static String decodeBase64(final String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return encoded;
    }
    try {
      return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    } catch (final IllegalArgumentException e) {
      return encoded;
    }
  }
}
