package com.nexora.platform.core.api;

import java.time.Instant;
import java.util.Map;

public record PlatformEventMessage(
    String messageId,
    String type,
    String source,
    String correlationId,
    Instant occurredAt,
    Map<String, Object> payload) {}
