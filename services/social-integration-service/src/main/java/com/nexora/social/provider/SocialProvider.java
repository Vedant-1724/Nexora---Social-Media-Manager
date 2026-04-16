package com.nexora.social.provider;

import java.util.Arrays;

public enum SocialProvider {
  META("meta"),
  LINKEDIN("linkedin"),
  X("x");

  private final String code;

  SocialProvider(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static SocialProvider fromCode(String rawValue) {
    return Arrays.stream(values())
        .filter(candidate -> candidate.code.equalsIgnoreCase(rawValue))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + rawValue));
  }
}
