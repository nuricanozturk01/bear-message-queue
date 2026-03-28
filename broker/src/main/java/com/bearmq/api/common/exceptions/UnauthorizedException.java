package com.bearmq.api.common.exceptions;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(final String message) {

    super(message);
  }
}
