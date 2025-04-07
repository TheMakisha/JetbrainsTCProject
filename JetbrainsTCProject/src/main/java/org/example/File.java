package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class File {
  public String sha;
  @JsonProperty("filename")
  public String fileName;
  public String status;
  public int additions;
  public int deletions;
  public int changes;
  @JsonProperty("blob_url")
  public String blobUrl;
  @JsonProperty("raw_url")
  public String rawUrl;
  @JsonProperty("contents_url")
  public String contentsUrl;
  public String patch;
}
