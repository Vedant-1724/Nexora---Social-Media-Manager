package com.nexora.notification.repository;

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
public class NotificationRepository {

  private static final TypeReference<Map<String, Object>> JSON_MAP = new TypeReference<>() {};

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper;

  public NotificationRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
    this.jdbcClient = jdbcClient;
    this.objectMapper = objectMapper;
  }

  // ── Notifications ────────────────────────────────────────────────────────

  public List<NotificationRecord> findInAppNotifications(UUID workspaceId, UUID userId) {
    return jdbcClient.sql(
            """
            SELECT n.id, n.workspace_id, n.recipient_user_id, n.template_id, n.channel,
                   n.status, n.payload, n.scheduled_for, n.sent_at, n.read_at, n.created_at,
                   t.code as template_code, t.name as template_name, t.body_template
            FROM notifications n
            JOIN notification_templates t ON n.template_id = t.id
            WHERE n.workspace_id = :workspaceId
              AND n.recipient_user_id = :userId
              AND n.channel = 'in_app'
            ORDER BY n.created_at DESC
            LIMIT 50
            """)
        .param("workspaceId", workspaceId)
        .param("userId", userId)
        .query(this::mapNotification)
        .list();
  }

  public void markAsRead(UUID workspaceId, UUID userId, UUID notificationId) {
    jdbcClient.sql(
            """
            UPDATE notifications
            SET status = 'read', read_at = NOW()
            WHERE id = :notificationId
              AND workspace_id = :workspaceId
              AND recipient_user_id = :userId
              AND status != 'read'
            """)
        .param("notificationId", notificationId)
        .param("workspaceId", workspaceId)
        .param("userId", userId)
        .update();
  }

  // ── Preferences ──────────────────────────────────────────────────────────

  public List<PreferenceRecord> getUserPreferences(UUID workspaceId, UUID userId) {
    return jdbcClient.sql(
            """
            SELECT id, workspace_id, user_id, event_code, channel, is_enabled, created_at
            FROM notification_preferences
            WHERE workspace_id = :workspaceId AND user_id = :userId
            """)
        .param("workspaceId", workspaceId)
        .param("userId", userId)
        .query(this::mapPreference)
        .list();
  }

  public void updatePreference(UUID workspaceId, UUID userId, String eventCode, String channel, boolean isEnabled) {
    jdbcClient.sql(
            """
            INSERT INTO notification_preferences (id, workspace_id, user_id, event_code, channel, is_enabled)
            VALUES (:id, :workspaceId, :userId, :eventCode, :channel, :isEnabled)
            ON CONFLICT (workspace_id, user_id, event_code, channel) 
            DO UPDATE SET is_enabled = EXCLUDED.is_enabled
            """)
        .param("id", UUID.randomUUID())
        .param("workspaceId", workspaceId)
        .param("userId", userId)
        .param("eventCode", eventCode)
        .param("channel", channel)
        .param("isEnabled", isEnabled)
        .update();
  }

  // ── Internal System ──────────────────────────────────────────────────────

  public Optional<UUID> findTemplateIdByCode(String code, String channel) {
    return jdbcClient.sql("SELECT id FROM notification_templates WHERE code = :code AND channel = :channel")
        .param("code", code)
        .param("channel", channel)
        .query(UUID.class)
        .optional();
  }

  public void insertNotification(
      UUID id, UUID workspaceId, UUID userId, UUID templateId, String channel, Map<String, Object> payload) {
    jdbcClient.sql(
            """
            INSERT INTO notifications (id, workspace_id, recipient_user_id, template_id, channel, status, payload)
            VALUES (:id, :workspaceId, :userId, :templateId, :channel, 'sent', :payload::jsonb)
            """)
        .param("id", id)
        .param("workspaceId", workspaceId)
        .param("userId", userId)
        .param("templateId", templateId)
        .param("channel", channel)
        .param("payload", toJson(payload))
        .update();
  }

  // ── Row Mappers ──────────────────────────────────────────────────────────

  private NotificationRecord mapNotification(ResultSet rs, int rowNum) throws SQLException {
    return new NotificationRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("workspace_id", UUID.class),
        rs.getObject("recipient_user_id", UUID.class),
        rs.getObject("template_id", UUID.class),
        rs.getString("channel"),
        rs.getString("status"),
        fromJson(rs.getString("payload")),
        rs.getString("template_code"),
        rs.getString("template_name"),
        rs.getString("body_template"),
        rs.getTimestamp("scheduled_for") != null ? rs.getTimestamp("scheduled_for").toInstant() : null,
        rs.getTimestamp("sent_at") != null ? rs.getTimestamp("sent_at").toInstant() : null,
        rs.getTimestamp("read_at") != null ? rs.getTimestamp("read_at").toInstant() : null,
        rs.getTimestamp("created_at").toInstant());
  }

  private PreferenceRecord mapPreference(ResultSet rs, int rowNum) throws SQLException {
    return new PreferenceRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("workspace_id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getString("event_code"),
        rs.getString("channel"),
        rs.getBoolean("is_enabled"),
        rs.getTimestamp("created_at").toInstant());
  }

  private Map<String, Object> fromJson(String json) {
    if (json == null || json.isBlank()) return Map.of();
    try {
      return objectMapper.readValue(json, JSON_MAP);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JSON", e);
    }
  }

  private String toJson(Map<String, Object> map) {
    try {
      return objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to write JSON", e);
    }
  }

  // ── Records ──────────────────────────────────────────────────────────────

  public record NotificationRecord(
      UUID id, UUID workspaceId, UUID recipientUserId, UUID templateId,
      String channel, String status, Map<String, Object> payload,
      String templateCode, String templateName, String bodyTemplate,
      Instant scheduledFor, Instant sentAt, Instant readAt, Instant createdAt) {}

  public record PreferenceRecord(
      UUID id, UUID workspaceId, UUID userId, String eventCode, String channel,
      boolean isEnabled, Instant createdAt) {}
}
