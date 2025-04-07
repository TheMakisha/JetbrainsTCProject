package org.example;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GHPage<T> {
  private final GithubClient githubClient;
  private final TypeReference<T> typeReference;
  private String nextUrl;
  private final T data;

  public GHPage(T data, String linkHeader, GithubClient client, TypeReference<T> typeReference) {
    this.typeReference = typeReference;
    this.githubClient = client;
    this.data = data;
    updateNextUrl(linkHeader);
  }

  public T getData() {
    return data;
  }

  private void updateNextUrl(String linkHeader) {
    if (linkHeader == null) return;

    Pattern pattern = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    Matcher matcher = pattern.matcher(linkHeader);
    if (matcher.find()) {
      nextUrl = matcher.group(1);
    } else {
      nextUrl = null;
    }
  }

  public boolean hasNext() {
    return nextUrl != null;
  }

  public T nextPage() {
    if (nextUrl == null) return null;
    String current = nextUrl;
    return fetchNextPage(current);
  }

  private T fetchNextPage(String url) {
    var request = new GHRequest(nextUrl);
    var response = githubClient.send(request, typeReference).response;
    updateNextUrl(response.nextUrl);
    return response.data;
  }
}
