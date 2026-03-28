package com.bearmq.api.broker.dtos;

import com.bearmq.shared.broker.dto.BindRequest;
import com.bearmq.shared.broker.dto.ExchangeRequest;
import com.bearmq.shared.broker.dto.QueueRequest;
import java.util.List;

public record BrokerRequest(
    String vhost,
    int schemaVersion,
    List<ExchangeRequest> exchanges,
    List<QueueRequest> queues,
    List<BindRequest> bindings) {}
