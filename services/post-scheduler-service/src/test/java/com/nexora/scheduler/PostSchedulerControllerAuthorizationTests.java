package com.nexora.scheduler;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import com.nexora.platform.webmvc.web.MvcApiExceptionHandler;
import com.nexora.platform.webmvc.web.MvcAuthorizationInterceptor;
import com.nexora.scheduler.api.PostSchedulerController;
import com.nexora.scheduler.service.PostSchedulerService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PostSchedulerControllerAuthorizationTests {

  private MockMvc mockMvc;
  private PostSchedulerService postSchedulerService;

  @BeforeEach
  void setUp() {
    postSchedulerService = Mockito.mock(PostSchedulerService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PostSchedulerController(postSchedulerService))
            .addInterceptors(new MvcAuthorizationInterceptor())
            .setControllerAdvice(new MvcApiExceptionHandler())
            .build();
  }

  @Test
  void draftsEndpointRejectsMissingPostsCreateScope() throws Exception {
    mockMvc.perform(
            get("/api/v1/workspaces/{workspaceId}/posts/drafts", "10000000-0000-0000-0000-000000000001")
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
  void draftsEndpointAllowsPostsCreateScope() throws Exception {
    when(postSchedulerService.listDrafts(UUID.fromString("10000000-0000-0000-0000-000000000001")))
        .thenReturn(
            List.of(
                new PostSchedulerService.DraftSummaryView(
                    UUID.fromString("40000000-0000-0000-0000-000000000021"),
                    UUID.fromString("10000000-0000-0000-0000-000000000001"),
                    UUID.fromString("00000000-0000-0000-0000-000000000101"),
                    "Spring Launch",
                    "Body",
                    "draft",
                    "Asia/Calcutta",
                    null,
                    "Campaign",
                    Map.of(),
                    Map.of(),
                    null,
                    null,
                    null,
                    Instant.parse("2026-04-01T12:00:00Z"),
                    Instant.parse("2026-04-01T12:00:00Z"),
                    Instant.parse("2026-04-01T12:00:00Z"))));

    mockMvc.perform(
            get("/api/v1/workspaces/{workspaceId}/posts/drafts", "10000000-0000-0000-0000-000000000001")
                .requestAttr(
                    NexoraRequestAttributes.REQUEST_CONTEXT,
                    new NexoraRequestContext(
                        "corr-1",
                        "00000000-0000-0000-0000-000000000101",
                        "10000000-0000-0000-0000-000000000001",
                        List.of("posts.create"),
                        "trace-1")))
        .andExpect(status().isOk());
  }

  @Test
  void approvalDecisionEndpointRequiresPostsApproveScope() throws Exception {
    mockMvc.perform(
            post(
                    "/api/v1/workspaces/{workspaceId}/posts/drafts/{draftId}/approval-decisions",
                    "10000000-0000-0000-0000-000000000001",
                    "40000000-0000-0000-0000-000000000021")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepNumber\":1,\"decision\":\"approved\"}")
                .requestAttr(
                    NexoraRequestAttributes.REQUEST_CONTEXT,
                    new NexoraRequestContext(
                        "corr-1",
                        "00000000-0000-0000-0000-000000000101",
                        "10000000-0000-0000-0000-000000000001",
                        List.of("posts.create"),
                        "trace-1")))
        .andExpect(status().isForbidden());
  }
}
