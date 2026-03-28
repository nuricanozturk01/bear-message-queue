package com.bearmq.api.broker.dto;

import com.bearmq.shared.broker.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateVhostStatusRequest(@NotNull Status status) {}
