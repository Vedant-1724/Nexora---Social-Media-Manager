package com.nexora.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

  private final JdbcClient jdbcClient;

  public AuthRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Optional<UserIdentityRecord> findUserByEmail(String email) {
    return jdbcClient.sql(
            """
            SELECT id, email, display_name, account_status, locale_code, default_timezone
            FROM user_identities
            WHERE LOWER(email) = LOWER(:email)
            """)
        .param("email", email)
        .query(this::mapUserIdentity)
        .optional();
  }

  public Optional<UserIdentityRecord> findUserById(UUID userId) {
    return jdbcClient.sql(
            """
            SELECT id, email, display_name, account_status, locale_code, default_timezone
            FROM user_identities
            WHERE id = :userId
            """)
        .param("userId", userId)
        .query(this::mapUserIdentity)
        .optional();
  }

  public void insertUserIdentity(
      UUID userId,
      String email,
      String displayName,
      String accountStatus,
      String localeCode,
      String defaultTimezone) {
    jdbcClient.sql(
            """
            INSERT INTO user_identities (
              id, email, display_name, account_status, locale_code, default_timezone
            ) VALUES (
              :id, :email, :displayName, :accountStatus, :localeCode, :defaultTimezone
            )
            """)
        .param("id", userId)
        .param("email", email)
        .param("displayName", displayName)
        .param("accountStatus", accountStatus)
        .param("localeCode", localeCode)
        .param("defaultTimezone", defaultTimezone)
        .update();
  }

  public void updateUserStatus(UUID userId, String accountStatus) {
    jdbcClient.sql(
            """
            UPDATE user_identities
            SET account_status = :accountStatus, updated_at = NOW()
            WHERE id = :userId
            """)
        .param("userId", userId)
        .param("accountStatus", accountStatus)
        .update();
  }

  public Optional<AuthMethodRecord> findPrimaryPasswordMethodByEmail(String email) {
    return jdbcClient.sql(
            """
            SELECT id, user_id, method_type, subject, secret_hash, is_primary, verified_at
            FROM auth_methods
            WHERE method_type = 'password' AND LOWER(subject) = LOWER(:email)
            ORDER BY is_primary DESC, created_at ASC
            LIMIT 1
            """)
        .param("email", email)
        .query(this::mapAuthMethod)
        .optional();
  }

  public Optional<AuthMethodRecord> findPrimaryPasswordMethodByUserId(UUID userId) {
    return jdbcClient.sql(
            """
            SELECT id, user_id, method_type, subject, secret_hash, is_primary, verified_at
            FROM auth_methods
            WHERE user_id = :userId AND method_type = 'password'
            ORDER BY is_primary DESC, created_at ASC
            LIMIT 1
            """)
        .param("userId", userId)
        .query(this::mapAuthMethod)
        .optional();
  }

  public void insertPasswordAuthMethod(
      UUID authMethodId,
      UUID userId,
      String subject,
      String secretHash,
      boolean primary) {
    jdbcClient.sql(
            """
            INSERT INTO auth_methods (
              id, user_id, method_type, subject, secret_hash, is_primary, metadata
            ) VALUES (
              :id, :userId, 'password', :subject, :secretHash, :primary, '{}'::jsonb
            )
            """)
        .param("id", authMethodId)
        .param("userId", userId)
        .param("subject", subject)
        .param("secretHash", secretHash)
        .param("primary", primary)
        .update();
  }

  public void updatePasswordHash(UUID authMethodId, String secretHash) {
    jdbcClient.sql(
            """
            UPDATE auth_methods
            SET secret_hash = :secretHash, updated_at = NOW()
            WHERE id = :authMethodId
            """)
        .param("authMethodId", authMethodId)
        .param("secretHash", secretHash)
        .update();
  }

  public void markPrimaryAuthMethodsVerified(UUID userId, Instant verifiedAt) {
    jdbcClient.sql(
            """
            UPDATE auth_methods
            SET verified_at = :verifiedAt, updated_at = NOW()
            WHERE user_id = :userId AND method_type = 'password'
            """)
        .param("userId", userId)
        .param("verifiedAt", Timestamp.from(verifiedAt))
        .update();
  }

  public void insertRefreshSession(
      UUID sessionId,
      UUID userId,
      String tokenHash,
      UUID workspaceContextId,
      String status,
      Instant issuedAt,
      Instant expiresAt,
      String ipAddress,
      String userAgent) {
    jdbcClient.sql(
            """
            INSERT INTO refresh_sessions (
              id, user_id, token_hash, workspace_context_id, status, issued_at, expires_at, ip_address, user_agent
            ) VALUES (
              :id, :userId, :tokenHash, :workspaceContextId, :status, :issuedAt, :expiresAt, :ipAddress, :userAgent
            )
            """)
        .param("id", sessionId)
        .param("userId", userId)
        .param("tokenHash", tokenHash)
        .param("workspaceContextId", workspaceContextId)
        .param("status", status)
        .param("issuedAt", Timestamp.from(issuedAt))
        .param("expiresAt", Timestamp.from(expiresAt))
        .param("ipAddress", ipAddress)
        .param("userAgent", userAgent)
        .update();
  }

  public Optional<RefreshSessionRecord> findRefreshSessionByTokenHash(String tokenHash) {
    return jdbcClient.sql(
            """
            SELECT id, user_id, token_hash, workspace_context_id, status, issued_at, expires_at, revoked_at, ip_address, user_agent
            FROM refresh_sessions
            WHERE token_hash = :tokenHash
            """)
        .param("tokenHash", tokenHash)
        .query(this::mapRefreshSession)
        .optional();
  }

  public Optional<RefreshSessionRecord> findRefreshSessionById(UUID sessionId) {
    return jdbcClient.sql(
            """
            SELECT id, user_id, token_hash, workspace_context_id, status, issued_at, expires_at, revoked_at, ip_address, user_agent
            FROM refresh_sessions
            WHERE id = :sessionId
            """)
        .param("sessionId", sessionId)
        .query(this::mapRefreshSession)
        .optional();
  }

  public void rotateRefreshSession(
      UUID sessionId,
      String tokenHash,
      UUID workspaceContextId,
      Instant issuedAt,
      Instant expiresAt,
      String userAgent) {
    jdbcClient.sql(
            """
            UPDATE refresh_sessions
            SET token_hash = :tokenHash,
                workspace_context_id = :workspaceContextId,
                issued_at = :issuedAt,
                expires_at = :expiresAt,
                user_agent = :userAgent,
                status = 'active',
                revoked_at = NULL
            WHERE id = :sessionId
            """)
        .param("sessionId", sessionId)
        .param("tokenHash", tokenHash)
        .param("workspaceContextId", workspaceContextId)
        .param("issuedAt", Timestamp.from(issuedAt))
        .param("expiresAt", Timestamp.from(expiresAt))
        .param("userAgent", userAgent)
        .update();
  }

  public void updateRefreshSessionWorkspace(UUID sessionId, UUID workspaceId) {
    jdbcClient.sql(
            """
            UPDATE refresh_sessions
            SET workspace_context_id = :workspaceId
            WHERE id = :sessionId
            """)
        .param("sessionId", sessionId)
        .param("workspaceId", workspaceId)
        .update();
  }

  public void revokeRefreshSession(UUID sessionId, Instant revokedAt) {
    jdbcClient.sql(
            """
            UPDATE refresh_sessions
            SET status = 'revoked', revoked_at = :revokedAt
            WHERE id = :sessionId
            """)
        .param("sessionId", sessionId)
        .param("revokedAt", Timestamp.from(revokedAt))
        .update();
  }

  public void insertVerificationChallenge(
      UUID challengeId,
      UUID userId,
      String challengeType,
      String tokenHash,
      Instant expiresAt) {
    jdbcClient.sql(
            """
            INSERT INTO verification_challenges (
              id, user_id, challenge_type, token_hash, status, expires_at, metadata
            ) VALUES (
              :id, :userId, :challengeType, :tokenHash, 'pending', :expiresAt, '{}'::jsonb
            )
            """)
        .param("id", challengeId)
        .param("userId", userId)
        .param("challengeType", challengeType)
        .param("tokenHash", tokenHash)
        .param("expiresAt", Timestamp.from(expiresAt))
        .update();
  }

  public Optional<VerificationChallengeRecord> findVerificationChallengeByTokenHash(String tokenHash) {
    return jdbcClient.sql(
            """
            SELECT id, user_id, challenge_type, token_hash, status, expires_at, consumed_at
            FROM verification_challenges
            WHERE token_hash = :tokenHash
            """)
        .param("tokenHash", tokenHash)
        .query(this::mapVerificationChallenge)
        .optional();
  }

  public void consumeVerificationChallenge(UUID challengeId, Instant consumedAt) {
    jdbcClient.sql(
            """
            UPDATE verification_challenges
            SET status = 'consumed', consumed_at = :consumedAt
            WHERE id = :challengeId
            """)
        .param("challengeId", challengeId)
        .param("consumedAt", Timestamp.from(consumedAt))
        .update();
  }

  private UserIdentityRecord mapUserIdentity(ResultSet resultSet, int rowNum) throws SQLException {
    return new UserIdentityRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getString("email"),
        resultSet.getString("display_name"),
        resultSet.getString("account_status"),
        resultSet.getString("locale_code"),
        resultSet.getString("default_timezone"));
  }

  private AuthMethodRecord mapAuthMethod(ResultSet resultSet, int rowNum) throws SQLException {
    return new AuthMethodRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("user_id", UUID.class),
        resultSet.getString("method_type"),
        resultSet.getString("subject"),
        resultSet.getString("secret_hash"),
        resultSet.getBoolean("is_primary"),
        toInstant(resultSet.getTimestamp("verified_at")));
  }

  private RefreshSessionRecord mapRefreshSession(ResultSet resultSet, int rowNum) throws SQLException {
    return new RefreshSessionRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("user_id", UUID.class),
        resultSet.getString("token_hash"),
        resultSet.getObject("workspace_context_id", UUID.class),
        resultSet.getString("status"),
        toInstant(resultSet.getTimestamp("issued_at")),
        toInstant(resultSet.getTimestamp("expires_at")),
        toInstant(resultSet.getTimestamp("revoked_at")),
        resultSet.getString("ip_address"),
        resultSet.getString("user_agent"));
  }

  private VerificationChallengeRecord mapVerificationChallenge(ResultSet resultSet, int rowNum)
      throws SQLException {
    return new VerificationChallengeRecord(
        resultSet.getObject("id", UUID.class),
        resultSet.getObject("user_id", UUID.class),
        resultSet.getString("challenge_type"),
        resultSet.getString("token_hash"),
        resultSet.getString("status"),
        toInstant(resultSet.getTimestamp("expires_at")),
        toInstant(resultSet.getTimestamp("consumed_at")));
  }

  private Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  public record UserIdentityRecord(
      UUID id,
      String email,
      String displayName,
      String accountStatus,
      String localeCode,
      String defaultTimezone) {}

  public record AuthMethodRecord(
      UUID id,
      UUID userId,
      String methodType,
      String subject,
      String secretHash,
      boolean primary,
      Instant verifiedAt) {}

  public record RefreshSessionRecord(
      UUID id,
      UUID userId,
      String tokenHash,
      UUID workspaceContextId,
      String status,
      Instant issuedAt,
      Instant expiresAt,
      Instant revokedAt,
      String ipAddress,
      String userAgent) {}

  public record VerificationChallengeRecord(
      UUID id,
      UUID userId,
      String challengeType,
      String tokenHash,
      String status,
      Instant expiresAt,
      Instant consumedAt) {}
}
