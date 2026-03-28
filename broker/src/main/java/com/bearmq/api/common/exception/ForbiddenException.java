package com.bearmq.api.common.exception;

public class ForbiddenException extends RuntimeException {

  public ForbiddenException(final String message) {
    super(message);
  }
}
