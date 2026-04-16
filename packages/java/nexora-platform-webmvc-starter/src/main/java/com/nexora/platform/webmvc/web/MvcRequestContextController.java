package com.nexora.platform.webmvc.web;

import com.nexora.platform.core.web.NexoraHeaderNames;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class MvcRequestContextController {

  @GetMapping("/request-context")
  public NexoraRequestContext requestContext(HttpServletRequest request) {
    NexoraRequestContext requestContext =
        (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
    if (requestContext != null) {
      return new NexoraRequestContext(
          requestContext.correlationId(),
          requestContext.userId(),
          requestContext.workspaceId(),
          requestContext.scopes(),
          MDC.get("traceId"));
    }

    return NexoraRequestContext.fromHeaders(
        request.getHeader(NexoraHeaderNames.CORRELATION_ID),
        request.getHeader(NexoraHeaderNames.USER_ID),
        request.getHeader(NexoraHeaderNames.WORKSPACE_ID),
        request.getHeader(NexoraHeaderNames.SCOPES),
        MDC.get("traceId"));
  }
}
