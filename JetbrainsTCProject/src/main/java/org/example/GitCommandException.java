package org.example;

public class GitCommandException extends RuntimeException {

  public GitCommandException(String message, Throwable cause) {
    super(message, cause);
  }

  public GitCommandException(String message) {
    super(message);
  }
}
