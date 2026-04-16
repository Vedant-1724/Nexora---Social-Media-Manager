package com.nexora.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.notification.repository.NotificationRepository;
import com.nexora.notification.repository.NotificationRepository.NotificationRecord;
import com.nexora.notification.repository.NotificationRepository.PreferenceRecord;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
  private final NotificationRepository repository;
  private final ObjectMapper objectMapper;

  public NotificationService(NotificationRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  // ── In-App Notifications ──────────────────────────────────────────────────

  public List<NotificationView> getUnreadInApp(UUID workspaceId, UUID userId) {
    return repository.findInAppNotifications(workspaceId, userId).stream()
        .map(r -> {
          // A very simple regex template engine replacing {{key}} with payload map values
          String title = r.templateName();
          String body = r.bodyTemplate();
          for (Map.Entry<String, Object> entry : r.payload().entrySet()) {
            String val = String.valueOf(entry.getValue());
            body = body.replace("{{" + entry.getKey() + "}}", val);
          }

          return new NotificationView(
              r.id().toString(),
              r.templateCode(),
              title,
              body,
              "read".equals(r.status()),
              r.createdAt().toString(),
              r.payload().get("link") != null ? r.payload().get("link").toString() : null);
        })
        .toList();
  }

  public void markAsRead(UUID workspaceId, UUID userId, UUID notificationId) {
    repository.markAsRead(workspaceId, userId, notificationId);
  }

  // ── Preferences ───────────────────────────────────────────────────────────

  public List<PreferenceView> getPreferences(UUID workspaceId, UUID userId) {
    return repository.getUserPreferences(workspaceId, userId).stream()
        .map(p -> new PreferenceView(p.eventCode(), p.channel(), p.isEnabled()))
        .toList();
  }

  public void setPreference(UUID workspaceId, UUID userId, String eventCode, String channel, boolean isEnabled) {
    repository.updatePreference(workspaceId, userId, eventCode, channel, isEnabled);
  }

  // ── Event Listener (Internal processing) ──────────────────────────────────

  /**
   * Listens to internal platform events. If the event is a posting event,
   * it writes a real in-app notification to the unified inbox.
   */
  @RabbitListener(
      queues = "${nexora.messaging.queue:notification-service.events}",
      autoStartup = "${nexora.messaging.enabled:false}")
  public void handlePlatformEvent(String messageJson) {
    log.info("Received event raw payload: {}", messageJson);
    try {
      Map<String, Object> event = objectMapper.readValue(messageJson, new TypeReference<>() {});
      String eventType = (String) event.get("type");
      
      if ("post.published".equals(eventType) || "post.failed".equals(eventType)) {
        log.info("Processing notification for event: {}", eventType);
        
        UUID workspaceId = UUID.fromString((String) event.get("workspaceId"));
        UUID userId = UUID.fromString((String) event.get("actorId"));
        
        // Check if user disabled in_app for this event code (post.published fallback)
        boolean isDisabled = getPreferences(workspaceId, userId).stream()
            .anyMatch(p -> p.channel().equals("in_app") && p.eventCode().equals(eventType) && !p.enabled());
            
        if (isDisabled) {
          log.info("Notification suppressed due to user preferences.");
          return;
        }

        Optional<UUID> tplOpt = repository.findTemplateIdByCode(eventType, "in_app");
        if (tplOpt.isPresent()) {
          Map<String, Object> payload = (Map<String, Object>) event.get("payload");
          repository.insertNotification(
              UUID.randomUUID(), workspaceId, userId, tplOpt.get(), "in_app", payload != null ? payload : Map.of()
          );
        }
      }
    } catch (JsonProcessingException e) {
      log.error("Failed to parse event message: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Failed processing event into notification.", e);
    }
  }

  // ── View Records ──────────────────────────────────────────────────────────

  public record NotificationView(
      String id, String type, String title, String message, boolean read,
      String createdAt, String actionUrl) {}

  public record PreferenceView(String eventCode, String channel, boolean enabled) {}
}
