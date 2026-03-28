package com.bearmq.api.common.exceptions;

public class ForbiddenException extends RuntimeException {

  public ForbiddenException(final String message) {

    super(message);
  }
}
