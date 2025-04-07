package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GithubClient {
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String accessToken;

  public GithubClient(String accessToken) {
    this.accessToken = accessToken;
  }

  public <T> GHResponse<T> send(GHRequest request, TypeReference<T> typeReference) {
    try {
      var httpRequest = HttpRequest.newBuilder()
              .uri(request.getUri())
              .header("Authorization", "Bearer " + accessToken)
              .GET()
              .build();

      var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      var body = response.body();

      if (response.statusCode() >= 400) {
        throw new GHApiException("GitHub API returned error " + response.statusCode() +
                " for request: " + request + "\nResponse body: " + body);
      }

      var linkHeader = response.headers().firstValue("Link");

      var responseObject = mapper.readValue(body, typeReference);
      var ghPage = new GHPage<T>(responseObject, linkHeader.orElse(null), this, typeReference);

      return new GHResponse<T>(ghPage);
    }
    catch (InterruptedException | IOException e) {
      throw new GHApiException("Error sending GitHub API request: " + request, e);
    }
  }
}
