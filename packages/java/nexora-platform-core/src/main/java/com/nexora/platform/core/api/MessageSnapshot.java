package com.nexora.platform.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MessageSnapshot(
    String messageId,
    String type,
    String routingKey,
    String source,
    String correlationId,
    Instant occurredAt,
    Instant receivedAt,
    Map<String, Object> payload) {}
