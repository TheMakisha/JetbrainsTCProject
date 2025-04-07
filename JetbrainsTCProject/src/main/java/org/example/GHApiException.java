package org.example;

public class GHApiException extends RuntimeException {

  public GHApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public GHApiException(String message) {
    super(message);
  }
}
