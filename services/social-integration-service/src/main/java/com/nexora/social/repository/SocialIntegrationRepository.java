package com.nexora.social.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SocialIntegrationRepository {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> STRING_MAP = new TypeReference<>() {};

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  public SocialIntegrationRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
    this.jdbcClient = jdbcClient;
    this.objectMapper = objectMapper;
  }

  public void insertOAuthLinkState(
      UUID id,
      UUID workspaceId,
      String provider,
      UUID connectedByUserId,
      String stateTokenHash,
      String codeVerifier,
      List<String> requestedScopes,
      Instant expiresAt) {
    jdbcClient.sql(
            """
            INSERT INTO oauth_link_states (
              id, workspace_id, provider, connected_by_user_id, state_token_hash, code_verifier, requested_scopes, expires_at
            ) VALUES (
              :id, :workspaceId, :provider, :connectedByUserId, :stateTokenHash, :codeVerifier, CAST(:requestedScopes AS jsonb), :expiresAt
            )
            """)
        .param("id", id)
        .param("workspaceId", workspaceId)
        .param("provider", provider)
        .param("connectedByUserId", connectedByUserId)
        .param("stateTokenHash", stateTokenHash)
        .param("codeVerifier", codeVerifier)
        .param("requestedScopes", toJson(requestedScopes))
        .param("expiresAt", Timestamp.from(expiresAt))
        .update();
  }

  public Optional<OAuthLinkStateRecord> findOAuthLinkStateByHash(String stateTokenHash) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, provider, connected_by_user_id, state_token_hash, code_verifier, requested_scopes, expires_at, consumed_at, created_at
            FROM oauth_link_states
            WHERE state_token_hash = :stateTokenHash
            """)
        .param("stateTokenHash", stateTokenHash)
        .query(this::mapOAuthLinkState)
        .optional();
  }

  public void markOAuthLinkStateConsumed(UUID stateId, Instant consumedAt) {
    jdbcClient.sql(
            """
            UPDATE oauth_link_states
            SET consumed_at = :consumedAt
            WHERE id = :stateId
            """)
        .param("stateId", stateId)
        .param("consumedAt", Timestamp.from(consumedAt))
        .update();
  }

  public Optional<ConnectedAccountRecord> findConnectedAccount(UUID workspaceId, UUID accountId) {
    return jdbcClient.sql(selectConnectedAccountsSql() + " WHERE workspace_id = :workspaceId AND id = :accountId")
        .param("workspaceId", workspaceId)
        .param("accountId", accountId)
        .query(this::mapConnectedAccount)
        .optional();
  }

  public Optional<ConnectedAccountRecord> findConnectedAccountByProviderExternalId(
      String provider,
      String externalAccountId) {
    return jdbcClient.sql(selectConnectedAccountsSql() + " WHERE provider = :provider AND external_account_id = :externalAccountId")
        .param("provider", provider)
        .param("externalAccountId", externalAccountId)
        .query(this::mapConnectedAccount)
        .optional();
  }

  public Optional<ConnectedAccountRecord> findConnectedAccountByProviderOrganizationId(
      String provider,
      String externalOrganizationId) {
    return jdbcClient.sql(selectConnectedAccountsSql() + " WHERE provider = :provider AND external_organization_id = :externalOrganizationId")
        .param("provider", provider)
        .param("externalOrganizationId", externalOrganizationId)
        .query(this::mapConnectedAccount)
        .optional();
  }

  public List<ConnectedAccountRecord> findConnectedAccounts(UUID workspaceId) {
    return jdbcClient.sql(selectConnectedAccountsSql() + " WHERE workspace_id = :workspaceId ORDER BY provider ASC, display_name ASC")
        .param("workspaceId", workspaceId)
        .query(this::mapConnectedAccount)
        .list();
  }

  public void insertConnectedAccount(
      UUID id,
      UUID workspaceId,
      String provider,
      String externalAccountId,
      String externalOrganizationId,
      String providerAccountType,
      String displayName,
      String username,
      String status,
      String accessTokenCiphertext,
      String refreshTokenCiphertext,
      String tokenEncryptionKeyId,
      Instant tokenExpiresAt,
      Instant tokenRefreshedAt,
      List<String> scopes,
      UUID connectedByUserId,
      Map<String, Object> metadata,
      Instant lastSyncAt) {
    jdbcClient.sql(
            """
            INSERT INTO connected_accounts (
              id, workspace_id, provider, external_account_id, external_organization_id, provider_account_type,
              display_name, username, status, access_token_ciphertext, refresh_token_ciphertext, token_encryption_key_id,
              token_expires_at, token_refreshed_at, scopes, connected_by_user_id, metadata, last_sync_at
            ) VALUES (
              :id, :workspaceId, :provider, :externalAccountId, :externalOrganizationId, :providerAccountType,
              :displayName, :username, :status, :accessTokenCiphertext, :refreshTokenCiphertext, :tokenEncryptionKeyId,
              :tokenExpiresAt, :tokenRefreshedAt, CAST(:scopes AS jsonb), :connectedByUserId, CAST(:metadata AS jsonb), :lastSyncAt
            )
            """)
        .param("id", id)
        .param("workspaceId", workspaceId)
        .param("provider", provider)
        .param("externalAccountId", externalAccountId)
        .param("externalOrganizationId", externalOrganizationId)
        .param("providerAccountType", providerAccountType)
        .param("displayName", displayName)
        .param("username", username)
        .param("status", status)
        .param("accessTokenCiphertext", accessTokenCiphertext)
        .param("refreshTokenCiphertext", refreshTokenCiphertext)
        .param("tokenEncryptionKeyId", tokenEncryptionKeyId)
        .param("tokenExpiresAt", timestamp(tokenExpiresAt))
        .param("tokenRefreshedAt", timestamp(tokenRefreshedAt))
        .param("scopes", toJson(scopes))
        .param("connectedByUserId", connectedByUserId)
        .param("metadata", toJson(metadata))
        .param("lastSyncAt", timestamp(lastSyncAt))
        .update();
  }

  public void updateConnectedAccount(
      UUID id,
      String externalOrganizationId,
      String providerAccountType,
      String displayName,
      String username,
      String status,
      String accessTokenCiphertext,
      String refreshTokenCiphertext,
      String tokenEncryptionKeyId,
      Instant tokenExpiresAt,
      Instant tokenRefreshedAt,
      List<String> scopes,
      Map<String, Object> metadata,
      Instant lastSyncAt) {
    jdbcClient.sql(
            """
            UPDATE connected_accounts
            SET external_organization_id = :externalOrganizationId,
                provider_account_type = :providerAccountType,
                display_name = :displayName,
                username = :username,
                status = :status,
                access_token_ciphertext = :accessTokenCiphertext,
                refresh_token_ciphertext = :refreshTokenCiphertext,
                token_encryption_key_id = :tokenEncryptionKeyId,
                token_expires_at = :tokenExpiresAt,
                token_refreshed_at = :tokenRefreshedAt,
                scopes = CAST(:scopes AS jsonb),
                metadata = CAST(:metadata AS jsonb),
                last_sync_at = :lastSyncAt,
                updated_at = NOW()
            WHERE id = :id
            """)
        .param("id", id)
        .param("externalOrganizationId", externalOrganizationId)
        .param("providerAccountType", providerAccountType)
        .param("displayName", displayName)
        .param("username", username)
        .param("status", status)
        .param("accessTokenCiphertext", accessTokenCiphertext)
        .param("refreshTokenCiphertext", refreshTokenCiphertext)
        .param("tokenEncryptionKeyId", tokenEncryptionKeyId)
        .param("tokenExpiresAt", timestamp(tokenExpiresAt))
        .param("tokenRefreshedAt", timestamp(tokenRefreshedAt))
        .param("scopes", toJson(scopes))
        .param("metadata", toJson(metadata))
        .param("lastSyncAt", timestamp(lastSyncAt))
        .update();
  }

  public void updateConnectedAccountStatus(UUID id, String status) {
    jdbcClient.sql(
            """
            UPDATE connected_accounts
            SET status = :status, updated_at = NOW()
            WHERE id = :id
            """)
        .param("id", id)
        .param("status", status)
        .update();
  }

  public List<String> findCapabilities(UUID connectedAccountId) {
    return jdbcClient.sql(
            """
            SELECT capability_code
            FROM account_capabilities
            WHERE connected_account_id = :connectedAccountId AND is_enabled = TRUE
            ORDER BY capability_code ASC
            """)
        .param("connectedAccountId", connectedAccountId)
        .query((resultSet, rowNum) -> resultSet.getString("capability_code"))
        .list();
  }

  public void replaceCapabilities(UUID connectedAccountId, List<String> capabilities) {
    jdbcClient.sql("DELETE FROM account_capabilities WHERE connected_account_id = :connectedAccountId")
        .param("connectedAccountId", connectedAccountId)
        .update();

    for (String capability : capabilities) {
      jdbcClient.sql(
              """
              INSERT INTO account_capabilities (connected_account_id, capability_code, is_enabled)
              VALUES (:connectedAccountId, :capabilityCode, TRUE)
              """)
          .param("connectedAccountId", connectedAccountId)
          .param("capabilityCode", capability)
          .update();
    }
  }

  public Optional<WebhookSubscriptionRecord> findWebhookSubscription(UUID connectedAccountId) {
    return jdbcClient.sql(
            """
            SELECT id, connected_account_id, provider_subscription_id, callback_path, secret_ciphertext, status,
                   subscribed_at, last_validated_at, metadata, updated_at
            FROM webhook_subscriptions
            WHERE connected_account_id = :connectedAccountId
            """)
        .param("connectedAccountId", connectedAccountId)
        .query(this::mapWebhookSubscription)
        .optional();
  }

  public void insertWebhookSubscription(
      UUID id,
      UUID connectedAccountId,
      String providerSubscriptionId,
      String callbackPath,
      String secretCiphertext,
      String status,
      Instant subscribedAt,
      Instant lastValidatedAt,
      Map<String, Object> metadata) {
    jdbcClient.sql(
            """
            INSERT INTO webhook_subscriptions (
              id, connected_account_id, provider_subscription_id, callback_path, secret_ciphertext, status,
              subscribed_at, last_validated_at, metadata
            ) VALUES (
              :id, :connectedAccountId, :providerSubscriptionId, :callbackPath, :secretCiphertext, :status,
              :subscribedAt, :lastValidatedAt, CAST(:metadata AS jsonb)
            )
            """)
        .param("id", id)
        .param("connectedAccountId", connectedAccountId)
        .param("providerSubscriptionId", providerSubscriptionId)
        .param("callbackPath", callbackPath)
        .param("secretCiphertext", secretCiphertext)
        .param("status", status)
        .param("subscribedAt", timestamp(subscribedAt))
        .param("lastValidatedAt", timestamp(lastValidatedAt))
        .param("metadata", toJson(metadata))
        .update();
  }

  public void updateWebhookSubscription(
      UUID id,
      String providerSubscriptionId,
      String callbackPath,
      String secretCiphertext,
      String status,
      Instant subscribedAt,
      Instant lastValidatedAt,
      Map<String, Object> metadata) {
    jdbcClient.sql(
            """
            UPDATE webhook_subscriptions
            SET provider_subscription_id = :providerSubscriptionId,
                callback_path = :callbackPath,
                secret_ciphertext = :secretCiphertext,
                status = :status,
                subscribed_at = :subscribedAt,
                last_validated_at = :lastValidatedAt,
                metadata = CAST(:metadata AS jsonb),
                updated_at = NOW()
            WHERE id = :id
            """)
        .param("id", id)
        .param("providerSubscriptionId", providerSubscriptionId)
        .param("callbackPath", callbackPath)
        .param("secretCiphertext", secretCiphertext)
        .param("status", status)
        .param("subscribedAt", timestamp(subscribedAt))
        .param("lastValidatedAt", timestamp(lastValidatedAt))
        .param("metadata", toJson(metadata))
        .update();
  }

  public boolean insertInboundInboxEvent(
      UUID id,
      UUID workspaceId,
      UUID connectedAccountId,
      String provider,
      String externalEventId,
      String externalThreadId,
      String eventType,
      String actorHandle,
      Map<String, Object> eventPayload,
      Instant receivedAt) {
    try {
      jdbcClient.sql(
              """
              INSERT INTO inbound_inbox_events (
                id, workspace_id, connected_account_id, provider, external_event_id, external_thread_id, event_type, actor_handle, event_payload, received_at
              ) VALUES (
                :id, :workspaceId, :connectedAccountId, :provider, :externalEventId, :externalThreadId, :eventType, :actorHandle, CAST(:eventPayload AS jsonb), :receivedAt
              )
              """)
          .param("id", id)
          .param("workspaceId", workspaceId)
          .param("connectedAccountId", connectedAccountId)
          .param("provider", provider)
          .param("externalEventId", externalEventId)
          .param("externalThreadId", externalThreadId)
          .param("eventType", eventType)
          .param("actorHandle", actorHandle)
          .param("eventPayload", toJson(eventPayload))
          .param("receivedAt", timestamp(receivedAt))
          .update();
      return true;
    } catch (DataIntegrityViolationException exception) {
      return false;
    }
  }

  private OAuthLinkStateRecord mapOAuthLinkState(ResultSet resultSet, int rowNum) throws SQLException {
    return new OAuthLinkStateRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("workspace_id", UUID.class),
        resultSet.getString("provider"),
        resultSet.getObject("connected_by_user_id", UUID.class),
        resultSet.getString("state_token_hash"),
        resultSet.getString("code_verifier"),
        readStringList(resultSet.getString("requested_scopes")),
        toInstant(resultSet.getTimestamp("expires_at")),
        toInstant(resultSet.getTimestamp("consumed_at")),
        toInstant(resultSet.getTimestamp("created_at")));
  }

  private ConnectedAccountRecord mapConnectedAccount(ResultSet resultSet, int rowNum) throws SQLException {
    return new ConnectedAccountRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("workspace_id", UUID.class),
        resultSet.getString("provider"),
        resultSet.getString("external_account_id"),
        resultSet.getString("external_organization_id"),
        resultSet.getString("provider_account_type"),
        resultSet.getString("display_name"),
        resultSet.getString("username"),
        resultSet.getString("status"),
        resultSet.getString("access_token_ciphertext"),
        resultSet.getString("refresh_token_ciphertext"),
        resultSet.getString("token_encryption_key_id"),
        toInstant(resultSet.getTimestamp("token_expires_at")),
        toInstant(resultSet.getTimestamp("token_refreshed_at")),
        readStringList(resultSet.getString("scopes")),
        resultSet.getObject("connected_by_user_id", UUID.class),
        readStringMap(resultSet.getString("metadata")),
        toInstant(resultSet.getTimestamp("last_sync_at")),
        toInstant(resultSet.getTimestamp("created_at")),
        toInstant(resultSet.getTimestamp("updated_at")));
  }

  private WebhookSubscriptionRecord mapWebhookSubscription(ResultSet resultSet, int rowNum) throws SQLException {
    return new WebhookSubscriptionRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("connected_account_id", UUID.class),
        resultSet.getString("provider_subscription_id"),
        resultSet.getString("callback_path"),
        resultSet.getString("secret_ciphertext"),
        resultSet.getString("status"),
        toInstant(resultSet.getTimestamp("subscribed_at")),
        toInstant(resultSet.getTimestamp("last_validated_at")),
        readStringMap(resultSet.getString("metadata")),
        toInstant(resultSet.getTimestamp("updated_at")));
  }

  private String selectConnectedAccountsSql() {
    return """
        SELECT id, workspace_id, provider, external_account_id, external_organization_id, provider_account_type,
               display_name, username, status, access_token_ciphertext, refresh_token_ciphertext, token_encryption_key_id,
               token_expires_at, token_refreshed_at, scopes, connected_by_user_id, metadata, last_sync_at, created_at, updated_at
        FROM connected_accounts
        """;
  }

  private Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private Instant toInstant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? Map.of() : value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize JSON payload", exception);
    }
  }

  private List<String> readStringList(String json) {
    try {
      return json == null ? List.of() : objectMapper.readValue(json, STRING_LIST);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to deserialize JSON list", exception);
    }
  }

  private Map<String, Object> readStringMap(String json) {
    try {
      return json == null ? Map.of() : objectMapper.readValue(json, STRING_MAP);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to deserialize JSON map", exception);
    }
  }

  public record OAuthLinkStateRecord(
      UUID id,
      UUID workspaceId,
      String provider,
      UUID connectedByUserId,
      String stateTokenHash,
      String codeVerifier,
      List<String> requestedScopes,
      Instant expiresAt,
      Instant consumedAt,
      Instant createdAt) {}

  public record ConnectedAccountRecord(
      UUID id,
      UUID workspaceId,
      String provider,
      String externalAccountId,
      String externalOrganizationId,
      String providerAccountType,
      String displayName,
      String username,
      String status,
      String accessTokenCiphertext,
      String refreshTokenCiphertext,
      String tokenEncryptionKeyId,
      Instant tokenExpiresAt,
      Instant tokenRefreshedAt,
      List<String> scopes,
      UUID connectedByUserId,
      Map<String, Object> metadata,
      Instant lastSyncAt,
      Instant createdAt,
      Instant updatedAt) {}

  public record WebhookSubscriptionRecord(
      UUID id,
      UUID connectedAccountId,
      String providerSubscriptionId,
      String callbackPath,
      String secretCiphertext,
      String status,
      Instant subscribedAt,
      Instant lastValidatedAt,
      Map<String, Object> metadata,
      Instant updatedAt) {}
}
