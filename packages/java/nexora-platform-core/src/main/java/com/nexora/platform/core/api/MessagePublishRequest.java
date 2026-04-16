package com.nexora.platform.core.api;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record MessagePublishRequest(
    @NotBlank(message = "Event type is required") String type,
    String routingKey,
    String source,
    Map<String, Object> payload) {}
