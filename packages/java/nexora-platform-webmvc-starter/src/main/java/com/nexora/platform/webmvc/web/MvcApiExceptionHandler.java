package com.nexora.platform.webmvc.web;

import com.nexora.platform.core.api.ApiErrorDetail;
import com.nexora.platform.core.api.ApiErrorResponse;
import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.UnauthorizedException;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MvcApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(MvcApiExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception,
      HttpServletRequest request) {
    List<ApiErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
        .map(this::toDetail)
        .toList();
    return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception,
      HttpServletRequest request) {
    List<ApiErrorDetail> details = exception.getConstraintViolations().stream()
        .map(violation -> new ApiErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
        .toList();
    return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception,
      HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(UnauthorizedException.class)
  ResponseEntity<ApiErrorResponse> handleUnauthorized(
      UnauthorizedException exception,
      HttpServletRequest request) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<ApiErrorResponse> handleForbidden(
      ForbiddenException exception,
      HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnhandledException(
      Exception exception,
      HttpServletRequest request) {
    log.error("Unhandled platform exception for path {}", request.getRequestURI(), exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Unexpected platform error",
        request,
        List.of());
  }

  private ApiErrorDetail toDetail(FieldError fieldError) {
    return new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      List<ApiErrorDetail> details) {
    NexoraRequestContext requestContext =
        (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);

    return ResponseEntity.status(status)
        .body(
            new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                requestContext != null ? requestContext.correlationId() : null,
                MDC.get("traceId"),
                details));
  }
}
