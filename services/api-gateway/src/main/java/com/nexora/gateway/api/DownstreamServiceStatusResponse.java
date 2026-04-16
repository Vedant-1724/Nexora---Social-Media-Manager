package com.nexora.gateway.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexora.platform.core.api.ServiceInfoResponse;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DownstreamServiceStatusResponse(
    String service,
    String baseUrl,
    String status,
    boolean docsAvailable,
    ServiceInfoResponse info,
    String error) {}
