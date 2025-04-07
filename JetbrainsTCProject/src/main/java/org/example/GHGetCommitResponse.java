package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GHGetCommitResponse {
  public String sha;
  public Commit commit;
  public List<File> files;
}