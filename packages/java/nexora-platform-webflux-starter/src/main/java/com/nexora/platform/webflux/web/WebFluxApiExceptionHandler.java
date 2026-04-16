package com.nexora.platform.webflux.web;

import com.nexora.platform.core.api.ApiErrorDetail;
import com.nexora.platform.core.api.ApiErrorResponse;
import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.UnauthorizedException;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class WebFluxApiExceptionHandler {

  @ExceptionHandler(WebExchangeBindException.class)
  ResponseEntity<ApiErrorResponse> handleValidationException(
      WebExchangeBindException exception,
      ServerWebExchange exchange) {
    List<ApiErrorDetail> details = exception.getFieldErrors().stream()
        .map(fieldError -> new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
        .toList();
    return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", exchange, details);
  }

  @ExceptionHandler(ServerWebInputException.class)
  ResponseEntity<ApiErrorResponse> handleWebInputException(
      ServerWebInputException exception,
      ServerWebExchange exchange) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getReason(), exchange, List.of());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception,
      ServerWebExchange exchange) {
    List<ApiErrorDetail> details = exception.getConstraintViolations().stream()
        .map(violation -> new ApiErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
        .toList();
    return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", exchange, details);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception,
      ServerWebExchange exchange) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), exchange, List.of());
  }

  @ExceptionHandler(UnauthorizedException.class)
  ResponseEntity<ApiErrorResponse> handleUnauthorized(
      UnauthorizedException exception,
      ServerWebExchange exchange) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), exchange, List.of());
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<ApiErrorResponse> handleForbidden(
      ForbiddenException exception,
      ServerWebExchange exchange) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), exchange, List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnhandledException(
      Exception exception,
      ServerWebExchange exchange) {
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Unexpected platform error",
        exchange,
        List.of());
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status,
      String message,
      ServerWebExchange exchange,
      List<ApiErrorDetail> details) {
    NexoraRequestContext requestContext =
        exchange.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);

    return ResponseEntity.status(status)
        .body(
            new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                requestContext != null ? requestContext.correlationId() : null,
                MDC.get("traceId"),
                details));
  }
}
