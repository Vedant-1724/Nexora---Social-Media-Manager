package com.nexora.user;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import com.nexora.platform.webmvc.web.MvcApiExceptionHandler;
import com.nexora.platform.webmvc.web.MvcAuthorizationInterceptor;
import com.nexora.user.api.UserWorkspaceController;
import com.nexora.user.domain.WorkspaceAccessSummary;
import com.nexora.user.service.UserWorkspaceService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserWorkspaceControllerAuthorizationTests {

  private MockMvc mockMvc;
  private UserWorkspaceService userWorkspaceService;

  @BeforeEach
  void setUp() {
    userWorkspaceService = Mockito.mock(UserWorkspaceService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new UserWorkspaceController(userWorkspaceService))
            .addInterceptors(new MvcAuthorizationInterceptor())
            .setControllerAdvice(new MvcApiExceptionHandler())
            .build();
  }

  @Test
  void membersEndpointRejectsMissingScope() throws Exception {
    mockMvc.perform(
            get("/api/v1/workspaces/{workspaceId}/members", "10000000-0000-0000-0000-000000000001")
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

  @Test
  void workspacesEndpointAllowsAuthenticatedRequest() throws Exception {
    when(userWorkspaceService.listWorkspaceAccess(UUID.fromString("00000000-0000-0000-0000-000000000101")))
        .thenReturn(
            List.of(
                new WorkspaceAccessSummary(
                    UUID.randomUUID(),
                    UUID.fromString("10000000-0000-0000-0000-000000000001"),
                    "Northstar Creative",
                    "northstar-creative",
                    "active",
                    "owner",
                    "Owner",
                    "active",
                    Instant.parse("2026-03-31T12:00:00Z"),
                    Set.of("workspace.members.read"))));

    mockMvc.perform(
            get("/api/v1/workspaces")
                .requestAttr(
                    NexoraRequestAttributes.REQUEST_CONTEXT,
                    new NexoraRequestContext(
                        "corr-1",
                        "00000000-0000-0000-0000-000000000101",
                        "10000000-0000-0000-0000-000000000001",
                        List.of("workspace.members.read"),
                        "trace-1")))
        .andExpect(status().isOk());
  }
}
