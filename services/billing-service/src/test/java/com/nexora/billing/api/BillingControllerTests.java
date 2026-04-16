package com.nexora.billing.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexora.billing.service.BillingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingController.class)
public class BillingControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private BillingService billingService;

  @Test
  void shouldReturnAvailablePlans() throws Exception {
    List<BillingService.PlanView> plans = List.of(
        new BillingService.PlanView(
            "id-1", "plan-123", "Basic Plan", "month", 1000, "USD", 5, 5, 100, java.util.Map.of()));

    given(billingService.getAvailablePlans()).willReturn(plans);

    mockMvc.perform(get("/api/v1/billing/plans"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("plan-123"))
        .andExpect(jsonPath("$[0].name").value("Basic Plan"))
        .andExpect(jsonPath("$[0].priceMinor").value(1000));
  }
}
