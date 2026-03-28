package com.bearmq.api.common.exception;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(final String message) {
    super(message);
  }
}
