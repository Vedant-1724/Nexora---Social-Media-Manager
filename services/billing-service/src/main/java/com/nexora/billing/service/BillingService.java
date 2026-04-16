package com.nexora.billing.service;

import com.nexora.billing.repository.BillingRepository;
import com.nexora.billing.repository.BillingRepository.EntitlementSnapshotRecord;
import com.nexora.billing.repository.BillingRepository.InvoiceRecord;
import com.nexora.billing.repository.BillingRepository.PlanRecord;
import com.nexora.billing.repository.BillingRepository.SubscriptionRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

  private final BillingRepository billingRepository;

  public BillingService(BillingRepository billingRepository) {
    this.billingRepository = billingRepository;
  }

  // ── Plans ────────────────────────────────────────────────────────────────

  public List<PlanView> getAvailablePlans() {
    return billingRepository.findAllActivePlans().stream()
        .map(p -> new PlanView(
            p.id().toString(),
            p.code(),
            p.name(),
            p.billingInterval(),
            p.priceMinor(),
            p.currency(),
            p.seatLimit(),
            p.socialAccountLimit(),
            p.monthlyPostLimit(),
            p.features()))
        .toList();
  }

  // ── Subscriptions ────────────────────────────────────────────────────────

  public Optional<SubscriptionView> getActiveSubscription(UUID workspaceId) {
    Optional<SubscriptionRecord> subOpt = billingRepository.findActiveSubscription(workspaceId);
    if (subOpt.isEmpty()) {
      return Optional.empty();
    }

    SubscriptionRecord sub = subOpt.get();
    Optional<PlanRecord> planOpt = billingRepository.findAllActivePlans().stream()
        .filter(p -> p.id().equals(sub.planId()))
        .findFirst(); // In real logic we'd do a direct lookup, assuming it's in the active list for now.

    String planName = planOpt.map(PlanRecord::name).orElse("Unknown Plan");
    String planCode = planOpt.map(PlanRecord::code).orElse("unknown");

    return Optional.of(new SubscriptionView(
        sub.id().toString(),
        sub.workspaceId().toString(),
        planName,
        planCode,
        sub.status(),
        sub.seatCount(),
        sub.trialEndsAt() != null ? sub.trialEndsAt().toString() : null,
        sub.currentPeriodStart().toString(),
        sub.currentPeriodEnd().toString(),
        sub.cancelAtPeriodEnd()));
  }

  // ── Invoices ─────────────────────────────────────────────────────────────

  public List<InvoiceView> getBillingHistory(UUID workspaceId) {
    Optional<SubscriptionRecord> subOpt = billingRepository.findActiveSubscription(workspaceId);
    if (subOpt.isEmpty()) {
      return List.of();
    }

    return billingRepository.findInvoices(subOpt.get().id()).stream()
        .map(inv -> new InvoiceView(
            inv.id().toString(),
            inv.providerInvoiceId(),
            inv.status(),
            inv.amountDueMinor(),
            inv.amountPaidMinor(),
            inv.currency(),
            inv.hostedInvoiceUrl(),
            inv.issuedAt().toString(),
            inv.paidAt() != null ? inv.paidAt().toString() : null))
        .toList();
  }

  // ── Entitlements ─────────────────────────────────────────────────────────

  public Optional<EntitlementView> getEntitlements(UUID workspaceId) {
    Optional<EntitlementSnapshotRecord> entOpt = billingRepository.findLatestEntitlement(workspaceId);
    return entOpt.map(ent -> new EntitlementView(
        ent.planCode(),
        ent.limits(),
        ent.features()));
  }

  // ── Provider Interactions (Razorpay/Stripe Placeholder) ──────────────────

  /**
   * TODO: Placeholder for Checkout Session Creation
   * Will integrate with Razorpay Orders API to generate an order_id for the frontend checkout payload.
   */
  public Object createCheckoutSession(UUID workspaceId, String planCode) {
    // RazorpayClient client = new RazorpayClient("KEY_ID", "KEY_SECRET");
    // JSONObject orderRequest = new JSONObject();
    // orderRequest.put("amount", 50000); // amt in paise
    // ...
    throw new UnsupportedOperationException("Not yet implemented. Awaiting specific API integration.");
  }

  /**
   * TODO: Placeholder for Webhook handling
   * Will intercept `subscription.charged`, `subscription.cancelled` etc.
   */
  public void handleProviderWebhook(String eventType, String payloadJson, String signature) {
    // Utils.verifyWebhookSignature(payloadJson, signature, "WEBHOOK_SECRET");
    // if ("subscription.charged".equals(eventType)) { ... }
    throw new UnsupportedOperationException("Not yet implemented. Awaiting specific API integration.");
  }

  // ── View Records ───────────────────────────────────────────────────────────

  public record PlanView(
      String id, String code, String name, String interval, int priceMinor,
      String currency, Integer seatLimit, Integer socialAccountLimit,
      Integer monthlyPostLimit, Map<String, Object> features) {}

  public record SubscriptionView(
      String id, String workspaceId, String planName, String planCode,
      String status, int seatCount, String trialEndsAt, String currentPeriodStart,
      String currentPeriodEnd, boolean cancelAtPeriodEnd) {}

  public record InvoiceView(
      String id, String invoiceNumber, String status, int amountDueMinor,
      int amountPaidMinor, String currency, String hostedInvoiceUrl,
      String issuedAt, String paidAt) {}

  public record EntitlementView(
      String planCode, Map<String, Object> limits, Map<String, Object> features) {}
}
