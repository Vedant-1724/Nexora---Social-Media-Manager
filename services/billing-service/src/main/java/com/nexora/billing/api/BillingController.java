package com.nexora.billing.api;

import com.nexora.billing.service.BillingService;
import com.nexora.platform.core.auth.ForbiddenException;
import com.nexora.platform.core.auth.RequireScopes;
import com.nexora.platform.core.web.NexoraRequestAttributes;
import com.nexora.platform.core.web.NexoraRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

  private final BillingService billingService;

  public BillingController(BillingService billingService) {
    this.billingService = billingService;
  }

  // ── Public / System Endpoints ──────────────────────────────────────────

  @GetMapping("/plans")
  public List<BillingService.PlanView> getAvailablePlans() {
    // No specific workspace context required, public pricing tier reading.
    return billingService.getAvailablePlans();
  }

  // ── Workspace Endpoints ──────────────────────────────────────────────

  @GetMapping("/workspaces/{workspaceId}/subscription")
  @RequireScopes("workspace.manage")
  public ResponseEntity<BillingService.SubscriptionView> getSubscription(
      @PathVariable("workspaceId") UUID workspaceId, HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return billingService.getActiveSubscription(workspaceId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/workspaces/{workspaceId}/invoices")
  @RequireScopes("workspace.manage")
  public List<BillingService.InvoiceView> getInvoices(
      @PathVariable("workspaceId") UUID workspaceId, HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return billingService.getBillingHistory(workspaceId);
  }

  @GetMapping("/workspaces/{workspaceId}/entitlements")
  @RequireScopes("workspace.manage")
  public ResponseEntity<BillingService.EntitlementView> getEntitlements(
      @PathVariable("workspaceId") UUID workspaceId, HttpServletRequest request) {
    ensureWorkspaceContext(workspaceId, request);
    return billingService.getEntitlements(workspaceId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  // ── Provider Webhooks (Placeholder) ──────────────────────────────────

  /**
   * TODO: Razorpay webhook ingress.
   * Expected headers: 'X-Razorpay-Signature'
   * 
   * @PostMapping("/webhooks/razorpay")
   * public ResponseEntity<Void> handleRazorpayWebhook(
   *     @RequestHeader("X-Razorpay-Signature") String signature,
   *     @RequestBody String payload) {
   *   // billingService.handleProviderWebhook("razorpay", payload, signature);
   *   return ResponseEntity.ok().build();
   * }
   */

  private void ensureWorkspaceContext(UUID workspaceId, HttpServletRequest request) {
    NexoraRequestContext ctx =
        (NexoraRequestContext) request.getAttribute(NexoraRequestAttributes.REQUEST_CONTEXT);
    if (ctx == null || ctx.workspaceId() == null || !workspaceId.toString().equals(ctx.workspaceId())) {
      throw new ForbiddenException(
          "The authenticated workspace context does not match the requested workspace");
    }
  }
}
