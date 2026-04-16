package com.nexora.platform.core.web;

import java.util.Arrays;
import java.util.List;

public record NexoraRequestContext(
    String correlationId,
    String userId,
    String workspaceId,
    List<String> scopes,
    String traceId) {

  public NexoraRequestContext {
    scopes = scopes == null ? List.of() : List.copyOf(scopes);
  }

  public static NexoraRequestContext fromHeaders(
      String correlationId,
      String userId,
      String workspaceId,
      String scopesHeader,
      String traceId) {
    return new NexoraRequestContext(
        correlationId,
        userId,
        workspaceId,
        parseScopes(scopesHeader),
        traceId);
  }

  public static List<String> parseScopes(String scopesHeader) {
    if (scopesHeader == null || scopesHeader.isBlank()) {
      return List.of();
    }

    return Arrays.stream(scopesHeader.split(","))
        .map(String::trim)
        .filter(scope -> !scope.isEmpty())
        .toList();
  }
}
