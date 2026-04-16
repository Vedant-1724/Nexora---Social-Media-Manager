package com.nexora.platform.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ServiceInfoResponse(
    String service,
    String displayName,
    String description,
    String phase,
    String version,
    String environment,
    String docsPath,
    Map<String, Object> capabilities) {}
