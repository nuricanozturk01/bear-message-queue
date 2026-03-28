package com.bearmq.api.common.exception;

public class ConflictException extends RuntimeException {
  public ConflictException(final String message) {
    super(message);
  }
}
