package org.example;

import java.net.URI;

public class GHRequest {
  private final URI uri;

  public GHRequest(String uri) {
    this.uri = URI.create(uri);
  }

  public URI getUri() {
    return uri;
  }

  @Override
  public String toString() {
    return uri.toString();
  }
}
