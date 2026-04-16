package com.nexora.platform.webmvc.web;

import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.RequireAuthenticated;
import com.nexora.platform.core.auth.RequireScopes;
import com.nexora.platform.core.auth.UnauthorizedException;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class MvcAuthorizationInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    RequireAuthenticated requireAuthenticated = findRequireAuthenticated(handlerMethod);
    RequireScopes requireScopes = findRequireScopes(handlerMethod);

    if (requireAuthenticated == null && requireScopes == null) {
      return true;
    }

    NexoraRequestContext requestContext =
        (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);

    if (requestContext == null || requestContext.userId() == null || requestContext.userId().isBlank()) {
      throw new UnauthorizedException("Authentication is required for this endpoint");
    }

    if (requireScopes == null) {
      return true;
    }

    Set<String> grantedScopes = new LinkedHashSet<>(requestContext.scopes());
    Set<String> requiredScopes = new LinkedHashSet<>(Arrays.asList(requireScopes.value()));

    boolean authorized =
        requireScopes.matchAll()
            ? grantedScopes.containsAll(requiredScopes)
            : requiredScopes.stream().anyMatch(grantedScopes::contains);

    if (!authorized) {
      throw new ForbiddenException("Insufficient scope for this endpoint");
    }

    return true;
  }

  private RequireAuthenticated findRequireAuthenticated(HandlerMethod handlerMethod) {
    RequireAuthenticated methodAnnotation =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireAuthenticated.class);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }

    return AnnotatedElementUtils.findMergedAnnotation(
        handlerMethod.getBeanType(),
        RequireAuthenticated.class);
  }

  private RequireScopes findRequireScopes(HandlerMethod handlerMethod) {
    RequireScopes methodAnnotation =
        AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireScopes.class);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }

    return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireScopes.class);
  }
}
