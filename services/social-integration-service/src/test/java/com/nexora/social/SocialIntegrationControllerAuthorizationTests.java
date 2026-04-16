package com.nexora.social;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import com.nexora.platform.webmvc.web.MvcApiExceptionHandler;
import com.nexora.platform.webmvc.web.MvcAuthorizationInterceptor;
import com.nexora.social.api.SocialIntegrationController;
import com.nexora.social.provider.SocialProvider;
import com.nexora.social.provider.SocialProviderAdapter;
import com.nexora.social.service.SocialIntegrationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SocialIntegrationControllerAuthorizationTests {

  private MockMvc mockMvc;
  private SocialIntegrationService socialIntegrationService;

  @BeforeEach
  void setUp() {
    socialIntegrationService = Mockito.mock(SocialIntegrationService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new SocialIntegrationController(socialIntegrationService))
            .addInterceptors(new MvcAuthorizationInterceptor())
            .setControllerAdvice(new MvcApiExceptionHandler())
            .build();
  }

  @Test
  void linkSessionEndpointRejectsMissingScope() throws Exception {
    mockMvc.perform(
            post("/api/v1/workspaces/{workspaceId}/social/link-sessions", "10000000-0000-0000-0000-000000000001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"meta\"}")
                .requestAttr(
                    NexoraRequestAttributes.REQUEST_CONTEXT,
                    new NexoraRequestContext(
                        "corr-1",
                        "00000000-0000-0000-0000-000000000101",
                        "10000000-0000-0000-0000-000000000001",
                        List.of("workspace.read"),
                        "trace-1")))
        .andExpect(status().isForbidden());
  }

  @Test
  void providersEndpointAllowsWorkspaceReadScope() throws Exception {
    when(socialIntegrationService.listProviders())
        .thenReturn(
            List.of(
                new SocialProviderAdapter.ProviderDescriptor(
                    SocialProvider.META,
                    "Meta",
                    true,
                    false,
                    "/api/v1/social/webhooks/meta",
                    List.of("pages_manage_posts"),
                    List.of("publish.text"),
                    List.of("youtube", "tiktok", "pinterest"))));

    mockMvc.perform(
            get("/api/v1/workspaces/{workspaceId}/social/providers", "10000000-0000-0000-0000-000000000001")
                .requestAttr(
                    NexoraRequestAttributes.REQUEST_CONTEXT,
                    new NexoraRequestContext(
                        "corr-1",
                        "00000000-0000-0000-0000-000000000101",
                        "10000000-0000-0000-0000-000000000001",
                        List.of("workspace.read"),
                        "trace-1")))
        .andExpect(status().isOk());
  }

  @Test
  void providersEndpointRejectsMismatchedWorkspaceContext() throws Exception {
    mockMvc.perform(
            get("/api/v1/workspaces/{workspaceId}/social/providers", "10000000-0000-0000-0000-000000000002")
                .requestAttr(
                    NexoraRequestAttributes.REQUEST_CONTEXT,
                    new NexoraRequestContext(
                        "corr-1",
                        "00000000-0000-0000-0000-000000000101",
                        "10000000-0000-0000-0000-000000000001",
                        List.of("workspace.read"),
                        "trace-1")))
        .andExpect(status().isForbidden());
  }
}
