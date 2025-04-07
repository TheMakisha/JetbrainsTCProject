package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Verification {
  public boolean verified;
  public String reason;
  public String signature;
  public String payload;
  @JsonProperty("verified_at")
  public String verifiedAt;
}
