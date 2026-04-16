package com.nexora.billing.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class BillingRepository {

  private static final TypeReference<Map<String, Object>> JSON_MAP = new TypeReference<>() {};

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  public BillingRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
    this.jdbcClient = jdbcClient;
    this.objectMapper = objectMapper;
  }

  // ── Plans ────────────────────────────────────────────────────────────────

  public List<PlanRecord> findAllActivePlans() {
    return jdbcClient.sql(
            """
            SELECT id, code, name, billing_interval, price_minor, currency,
                   seat_limit, social_account_limit, monthly_post_limit,
                   features, is_public, active, created_at
            FROM plans
            WHERE active = TRUE AND is_public = TRUE
            ORDER BY price_minor ASC
            """)
        .query(this::mapPlan)
        .list();
  }

  public Optional<PlanRecord> findPlanByCode(String code) {
    return jdbcClient.sql("SELECT * FROM plans WHERE code = :code")
        .param("code", code)
        .query(this::mapPlan)
        .optional();
  }

  // ── Subscriptions ────────────────────────────────────────────────────────

  public Optional<SubscriptionRecord> findActiveSubscription(UUID workspaceId) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, plan_id, provider, provider_subscription_id,
                   status, seat_count, trial_ends_at, current_period_start,
                   current_period_end, cancel_at_period_end, canceled_at, created_at
            FROM subscriptions
            WHERE workspace_id = :workspaceId
              AND status IN ('trialing', 'active', 'past_due')
            """)
        .param("workspaceId", workspaceId)
        .query(this::mapSubscription)
        .optional();
  }

  // ── Invoices ─────────────────────────────────────────────────────────────

  public List<InvoiceRecord> findInvoices(UUID subscriptionId) {
    return jdbcClient.sql(
            """
            SELECT id, subscription_id, provider_invoice_id, status, amount_due_minor,
                   amount_paid_minor, currency, hosted_invoice_url, issued_at,
                   due_at, paid_at
            FROM invoices
            WHERE subscription_id = :subscriptionId
            ORDER BY issued_at DESC
            """)
        .param("subscriptionId", subscriptionId)
        .query(this::mapInvoice)
        .list();
  }

  // ── Entitlements ─────────────────────────────────────────────────────────

  public Optional<EntitlementSnapshotRecord> findLatestEntitlement(UUID workspaceId) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, subscription_id, plan_code, limits, features, generated_at
            FROM entitlement_snapshots
            WHERE workspace_id = :workspaceId
            ORDER BY generated_at DESC
            LIMIT 1
            """)
        .param("workspaceId", workspaceId)
        .query(this::mapEntitlement)
        .optional();
  }

  // ── Row Mappers ────────────────────────────────────────────────────────────

  private PlanRecord mapPlan(ResultSet rs, int rowNum) throws SQLException {
    return new PlanRecord(
        rs.getObject("id", UUID.class),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("billing_interval"),
        rs.getInt("price_minor"),
        rs.getString("currency"),
        rs.getObject("seat_limit", Integer.class),
        rs.getObject("social_account_limit", Integer.class),
        rs.getObject("monthly_post_limit", Integer.class),
        fromJson(rs.getString("features")),
        rs.getBoolean("is_public"),
        rs.getBoolean("active"),
        rs.getTimestamp("created_at").toInstant());
  }

  private SubscriptionRecord mapSubscription(ResultSet rs, int rowNum) throws SQLException {
    return new SubscriptionRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("workspace_id", UUID.class),
        rs.getObject("plan_id", UUID.class),
        rs.getString("provider"),
        rs.getString("provider_subscription_id"),
        rs.getString("status"),
        rs.getInt("seat_count"),
        rs.getTimestamp("trial_ends_at") != null ? rs.getTimestamp("trial_ends_at").toInstant() : null,
        rs.getTimestamp("current_period_start").toInstant(),
        rs.getTimestamp("current_period_end").toInstant(),
        rs.getBoolean("cancel_at_period_end"),
        rs.getTimestamp("canceled_at") != null ? rs.getTimestamp("canceled_at").toInstant() : null,
        rs.getTimestamp("created_at").toInstant());
  }

  private InvoiceRecord mapInvoice(ResultSet rs, int rowNum) throws SQLException {
    return new InvoiceRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("subscription_id", UUID.class),
        rs.getString("provider_invoice_id"),
        rs.getString("status"),
        rs.getInt("amount_due_minor"),
        rs.getInt("amount_paid_minor"),
        rs.getString("currency"),
        rs.getString("hosted_invoice_url"),
        rs.getTimestamp("issued_at").toInstant(),
        rs.getTimestamp("due_at") != null ? rs.getTimestamp("due_at").toInstant() : null,
        rs.getTimestamp("paid_at") != null ? rs.getTimestamp("paid_at").toInstant() : null);
  }

  private EntitlementSnapshotRecord mapEntitlement(ResultSet rs, int rowNum) throws SQLException {
    return new EntitlementSnapshotRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("workspace_id", UUID.class),
        rs.getObject("subscription_id", UUID.class),
        rs.getString("plan_code"),
        fromJson(rs.getString("limits")),
        fromJson(rs.getString("features")),
        rs.getTimestamp("generated_at").toInstant());
  }

  private Map<String, Object> fromJson(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, JSON_MAP);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JSON: " + json, e);
    }
  }

  // ── Records ────────────────────────────────────────────────────────────────

  public record PlanRecord(
      UUID id, String code, String name, String billingInterval, int priceMinor,
      String currency, Integer seatLimit, Integer socialAccountLimit,
      Integer monthlyPostLimit, Map<String, Object> features,
      boolean isPublic, boolean active, Instant createdAt) {}

  public record SubscriptionRecord(
      UUID id, UUID workspaceId, UUID planId, String provider,
      String providerSubscriptionId, String status, int seatCount,
      Instant trialEndsAt, Instant currentPeriodStart, Instant currentPeriodEnd,
      boolean cancelAtPeriodEnd, Instant canceledAt, Instant createdAt) {}

  public record InvoiceRecord(
      UUID id, UUID subscriptionId, String providerInvoiceId, String status,
      int amountDueMinor, int amountPaidMinor, String currency, String hostedInvoiceUrl,
      Instant issuedAt, Instant dueAt, Instant paidAt) {}

  public record EntitlementSnapshotRecord(
      UUID id, UUID workspaceId, UUID subscriptionId, String planCode,
      Map<String, Object> limits, Map<String, Object> features, Instant generatedAt) {}
}
