package com.bearmq.api.broker.mapper;

import com.bearmq.api.broker.dtos.read.BindingSummaryDto;
import com.bearmq.api.broker.dtos.read.ExchangeSummaryDto;
import com.bearmq.api.broker.dtos.read.QueueSummaryDto;
import com.bearmq.shared.binding.Binding;
import com.bearmq.shared.binding.DestinationType;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.exchange.ExchangeType;
import com.bearmq.shared.queue.Queue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class BrokerReadMapper {

  @Mapping(target = "status", source = "status", qualifiedByName = "statusName")
  public abstract QueueSummaryDto toQueueSummary(Queue queue);

  @Mapping(target = "type", source = "type", qualifiedByName = "exchangeTypeName")
  @Mapping(target = "status", source = "status", qualifiedByName = "statusName")
  public abstract ExchangeSummaryDto toExchangeSummary(Exchange exchange);

  @Mapping(target = "sourceExchangeName", expression = "java(sourceExchangeName(binding))")
  @Mapping(target = "destinationName", expression = "java(destinationName(binding))")
  @Mapping(target = "status", expression = "java(statusName(binding.getStatus()))")
  public abstract BindingSummaryDto toBindingSummary(Binding binding);

  @Named("statusName")
  protected String statusName(final Status status) {

    return status == null ? "" : status.name();
  }

  @Named("exchangeTypeName")
  protected String exchangeTypeName(final ExchangeType type) {

    return type == null ? "" : type.name();
  }

  protected String sourceExchangeName(final Binding binding) {

    return binding.getSourceExchangeRef() != null ? binding.getSourceExchangeRef().getName() : "";
  }

  protected String destinationName(final Binding binding) {

    if (binding.getDestinationType() == DestinationType.EXCHANGE
        && binding.getDestinationExchangeRef() != null) {
      return binding.getDestinationExchangeRef().getName();
    }
    if (binding.getDestinationQueueRef() != null) {
      return binding.getDestinationQueueRef().getName();
    }
    return "";
  }
}
