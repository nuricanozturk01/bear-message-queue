package com.bearmq.api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.Instant;
import java.util.Map;

@JsonInclude(Include.NON_EMPTY)
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> fieldErrors) {

  public static ApiErrorResponse of(
      final int status, final String error, final String message, final String path) {
    return new ApiErrorResponse(Instant.now(), status, error, message, path, null);
  }

  public static ApiErrorResponse withFields(
      final int status,
      final String error,
      final String message,
      final String path,
      final Map<String, String> fieldErrors) {
    return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
  }
}
