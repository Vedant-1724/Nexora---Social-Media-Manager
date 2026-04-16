package com.nexora.gateway;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.nexora.gateway.security.GatewayAccessRevocationService;
import com.nexora.gateway.security.GatewayAccessTokenVerifier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(properties = {
    "management.health.redis.enabled=false"
})
@AutoConfigureWebTestClient
class GatewayApplicationTests {

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private GatewayAccessTokenVerifier gatewayAccessTokenVerifier;

  @MockBean
  private GatewayAccessRevocationService gatewayAccessRevocationService;

  @Test
  void healthEndpointReturnsUp() {
    webTestClient
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP");
  }

  @Test
  void systemInfoEndpointReturnsPhaseFourMetadata() {
    webTestClient
        .get()
        .uri("/api/v1/system/info")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.service")
        .isEqualTo("api-gateway")
        .jsonPath("$.phase")
        .isEqualTo("phase-4");
  }

  @Test
  void serviceCatalogEndpointReturnsAllDownstreamDefinitions() {
    webTestClient
        .get()
        .uri("/api/v1/system/services/catalog")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.length()")
        .isEqualTo(7);
  }

  @Test
  void protectedEndpointRejectsMissingBearerToken() {
    webTestClient
        .get()
        .uri("/api/v1/test/protected")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void protectedEndpointPropagatesAuthenticatedHeaders() {
    when(gatewayAccessRevocationService.isAccessTokenRevoked(anyString())).thenReturn(Mono.just(false));

    String token =
        gatewayAccessTokenVerifier.issueTestToken(
            UUID.fromString("00000000-0000-0000-0000-000000000101"),
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            UUID.fromString("20000000-0000-0000-0000-000000000001"),
            "phase-4-gateway-token",
            List.of("workspace.members.read", "posts.read"),
            Instant.now().plusSeconds(300));

    webTestClient
        .get()
        .uri("/api/v1/test/protected")
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.userId")
        .isEqualTo("00000000-0000-0000-0000-000000000101")
        .jsonPath("$.workspaceId")
        .isEqualTo("10000000-0000-0000-0000-000000000001")
        .jsonPath("$.scopes[0]")
        .isEqualTo("workspace.members.read");
  }

  @TestConfiguration
  static class GatewayTestConfiguration {

    @Bean
    GatewayTestController gatewayTestController() {
      return new GatewayTestController();
    }
  }

  @RestController
  static class GatewayTestController {

    @GetMapping("/api/v1/test/protected")
    TestAuthEcho protectedEndpoint(
        @RequestHeader("X-Nexora-User-Id") String userId,
        @RequestHeader("X-Nexora-Workspace-Id") String workspaceId,
        @RequestHeader("X-Nexora-Scopes") String scopes) {
      return new TestAuthEcho(userId, workspaceId, List.of(scopes.split(",")));
    }
  }

  record TestAuthEcho(String userId, String workspaceId, List<String> scopes) {}
}
