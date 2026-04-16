package com.nexora.platform.webmvc.web;

import com.nexora.platform.core.web.NexoraHeaderNames;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = request.getHeader(NexoraHeaderNames.CORRELATION_ID);
    if (!StringUtils.hasText(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }

    request.setAttribute(
        NexoraRequestAttributes.REQUEST_CONTEXT,
        NexoraRequestContext.fromHeaders(
            correlationId,
            request.getHeader(NexoraHeaderNames.USER_ID),
            request.getHeader(NexoraHeaderNames.WORKSPACE_ID),
            request.getHeader(NexoraHeaderNames.SCOPES),
            MDC.get("traceId")));

    response.setHeader(NexoraHeaderNames.CORRELATION_ID, correlationId);
    MDC.put("correlationId", correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
