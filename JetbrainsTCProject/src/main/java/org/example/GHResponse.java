package org.example;

public class GHResponse<T> {
  public GHPage<T> response;

  public GHResponse(GHPage<T> response) {
    this.response = response;
  }

}
