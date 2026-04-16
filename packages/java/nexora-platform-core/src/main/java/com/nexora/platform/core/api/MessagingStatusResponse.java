package com.nexora.platform.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MessagingStatusResponse(
    boolean enabled,
    String exchange,
    String queue,
    String routingKey,
    MessageSnapshot lastConsumed) {}
