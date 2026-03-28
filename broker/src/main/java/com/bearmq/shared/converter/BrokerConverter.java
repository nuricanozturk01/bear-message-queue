package com.bearmq.shared.converter;

import com.bearmq.shared.binding.Binding;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.broker.dto.BindRequest;
import com.bearmq.shared.broker.dto.ExchangeRequest;
import com.bearmq.shared.broker.dto.QueueRequest;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.vhost.VirtualHost;
import com.bearmq.shared.vhost.dto.VirtualHostInfo;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class BrokerConverter {

  @Mapping(ignore = true, target = "arguments")
  public abstract Queue toQueue(QueueRequest queueRequest);

  @Mapping(ignore = true, target = "arguments")
  public abstract Binding toBinding(BindRequest bindRequest);

  @Mapping(ignore = true, target = "arguments")
  public abstract Exchange toExchange(ExchangeRequest exchangeRequest);

  @Mapping(target = "password", source = "password", qualifiedByName = "decodePassword")
  @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
  public abstract VirtualHostInfo toVirtualHostInfo(VirtualHost vhost);

  @Named("decodePassword")
  protected String decodePassword(final String encoded) {

    if (encoded == null || encoded.isBlank()) {
      return encoded;
    }
    try {
      return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    } catch (final IllegalArgumentException e) {
      return encoded;
    }
  }

  @Named("statusToString")
  protected String statusToString(final Status status) {

    return status == null ? "" : status.name();
  }
}
