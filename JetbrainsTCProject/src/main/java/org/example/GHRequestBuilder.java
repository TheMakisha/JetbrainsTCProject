package org.example;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class GHRequestBuilder {
  private static final String BASE_URL = "https://api.github.com";

  private final StringBuilder pathBuilder = new StringBuilder();
  private final Map<String, String> queryParams = new LinkedHashMap<>();

  private GHRequestBuilder() {}

  public static ListCommitsEndpoint listCommits(String owner, String repo) {
    GHRequestBuilder builder = new GHRequestBuilder();
    builder.pathBuilder.append(BASE_URL)
            .append("/repos/")
            .append(URLEncoder.encode(owner, StandardCharsets.UTF_8))
            .append("/")
            .append(URLEncoder.encode(repo, StandardCharsets.UTF_8))
            .append("/commits");
    return new ListCommitsEndpoint(builder);
  }

  public static GetCommitEndPoint getCommit(String owner, String repo, String ref) {
    GHRequestBuilder builder = new GHRequestBuilder();
    builder.pathBuilder.append(BASE_URL)
            .append("/repos/")
            .append(URLEncoder.encode(owner, StandardCharsets.UTF_8))
            .append("/")
            .append(URLEncoder.encode(repo, StandardCharsets.UTF_8))
            .append("/commits/")
            .append(URLEncoder.encode(ref, StandardCharsets.UTF_8));
    return new GetCommitEndPoint(builder);
  }

  public GHRequest build() {
    if (!queryParams.isEmpty()) {
      pathBuilder.append("?");
      queryParams.forEach((k, v) -> {
        pathBuilder.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(v, StandardCharsets.UTF_8))
                .append("&");
      });
      pathBuilder.setLength(pathBuilder.length() - 1);
    }
    return new GHRequest(pathBuilder.toString());
  }

  public static class ListCommitsEndpoint {

    private final GHRequestBuilder builder;

    private ListCommitsEndpoint(GHRequestBuilder builder) {
      this.builder = builder;
    }

    public ListCommitsEndpoint sha(String sha) {
      builder.queryParams.put("sha", sha);
      return this;
    }

    public ListCommitsEndpoint path(String path) {
      builder.queryParams.put("path", path);
      return this;
    }

    public ListCommitsEndpoint author(String author) {
      builder.queryParams.put("author", author);
      return this;
    }

    public ListCommitsEndpoint since(String isoDate) {
      builder.queryParams.put("since", isoDate);
      return this;
    }

    public ListCommitsEndpoint until(String isoDate) {
      builder.queryParams.put("until", isoDate);
      return this;
    }

    public ListCommitsEndpoint perPage(int count) {
      builder.queryParams.put("per_page", String.valueOf(count));
      return this;
    }

    public ListCommitsEndpoint page(int page) {
      builder.queryParams.put("page", String.valueOf(page));
      return this;
    }

    public GHRequest build() {
      return builder.build();
    }
  }

  public static class GetCommitEndPoint {
    private final GHRequestBuilder builder;

    private GetCommitEndPoint(GHRequestBuilder builder) {
      this.builder = builder;
    }

    public GetCommitEndPoint perPage(int count) {
      builder.queryParams.put("per_page", String.valueOf(count));
      return this;
    }

    public GetCommitEndPoint page(int page) {
      builder.queryParams.put("page", String.valueOf(page));
      return this;
    }

    public GHRequest build() {
      return builder.build();
    }
  }
}
