package com.nexora.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:auth-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "management.health.redis.enabled=false"
})
@AutoConfigureMockMvc
class AuthServiceApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private StringRedisTemplate stringRedisTemplate;

  @Test
  void healthEndpointReturnsUp() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void systemInfoEndpointReturnsPhaseFourMetadata() throws Exception {
    mockMvc.perform(get("/api/v1/system/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.service").value("auth-service"))
        .andExpect(jsonPath("$.phase").value("phase-4"));
  }

  @Test
  void requestContextEndpointEchoesPlatformHeaders() throws Exception {
    mockMvc.perform(
            get("/api/v1/system/request-context")
                .header("X-Correlation-Id", "corr-auth-test")
                .header("X-Nexora-User-Id", "user-123")
                .header("X-Nexora-Workspace-Id", "workspace-456")
                .header("X-Nexora-Scopes", "posts.read,posts.write"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correlationId").value("corr-auth-test"))
        .andExpect(jsonPath("$.userId").value("user-123"))
        .andExpect(jsonPath("$.workspaceId").value("workspace-456"))
        .andExpect(jsonPath("$.scopes[0]").value("posts.read"))
        .andExpect(jsonPath("$.scopes[1]").value("posts.write"));
  }
}
