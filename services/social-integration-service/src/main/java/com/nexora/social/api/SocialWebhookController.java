package com.nexora.social.api;

import com.nexora.social.provider.SocialProvider;
import com.nexora.social.provider.SocialProviderAdapter.WebhookChallengeResponse;
import com.nexora.social.service.SocialIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/webhooks")
public class SocialWebhookController {

  private final SocialIntegrationService socialIntegrationService;

  public SocialWebhookController(SocialIntegrationService socialIntegrationService) {
    this.socialIntegrationService = socialIntegrationService;
  }

  @GetMapping("/{provider}")
  public ResponseEntity<?> verifyWebhook(
      @PathVariable("provider") String provider,
      @RequestParam Map<String, String> queryParameters) {
    WebhookChallengeResponse response =
        socialIntegrationService.handleWebhookChallenge(
            SocialProvider.fromCode(provider),
            queryParameters);
    if (!response.handled()) {
      return ResponseEntity.noContent().build();
    }
    if (response.plainTextBody() != null) {
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(response.contentType()))
          .body(response.plainTextBody());
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(response.contentType()))
        .body(response.jsonBody());
  }

  @PostMapping(
      value = "/{provider}",
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public SocialIntegrationService.WebhookDeliveryView ingestWebhook(
      @PathVariable("provider") String provider,
      @RequestBody(required = false) String rawBody,
      HttpServletRequest request) {
    return socialIntegrationService.processWebhookEvent(
        SocialProvider.fromCode(provider),
        headerMap(request),
        rawBody == null ? "" : rawBody);
  }

  private Map<String, String> headerMap(HttpServletRequest request) {
    Map<String, String> headers = new LinkedHashMap<>();
    Enumeration<String> names = request.getHeaderNames();
    if (names == null) {
      return headers;
    }
    for (String headerName : Collections.list(names)) {
      headers.put(headerName, request.getHeader(headerName));
    }
    return headers;
  }
}
