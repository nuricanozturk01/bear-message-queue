package com.bearmq.api.broker.dtos.read;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PeekedMessageDto(int sequence, JsonNode json, String text, String base64) {}
