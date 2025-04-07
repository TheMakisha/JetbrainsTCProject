package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Commit {
  public User author;
  public User committer;
  public String message;
  public Tree tree;
  public String url;
  @JsonProperty("comment_count")
  public int commentCount;
  public Verification verification;
}
