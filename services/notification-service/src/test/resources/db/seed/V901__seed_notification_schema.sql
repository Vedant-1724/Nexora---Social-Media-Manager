-- ═══════════════════════════════════════════════════════════════════════════
-- V2: Seed basic notification templates and test inbox state
-- Workspace: 10000000-0000-0000-0000-000000000001
-- User: 20000000-0000-0000-0000-000000000001
-- ═══════════════════════════════════════════════════════════════════════════

SET search_path TO notification_service;

-- ── Templates ────────────────────────────────────────────────────────────

INSERT INTO notification_templates (id, code, channel, name, body_template) VALUES
  ('a1000000-0000-0000-0000-000000000001', 'post.published', 'in_app', 'Post Published', 'Your scheduled post "{{postTitle}}" was successfully published to {{platform}}.'),
  ('a1000000-0000-0000-0000-000000000002', 'post.failed', 'in_app', 'Publishing Failed', 'Failed to publish "{{postTitle}}" to {{platform}}. Reason: {{error}}'),
  ('a1000000-0000-0000-0000-000000000003', 'approvals.required', 'in_app', 'Approval Required', '{{actorName}} requested approval for a new draft on {{platform}}.')
ON CONFLICT (code) DO NOTHING;

-- ── Inbox Notifications ──────────────────────────────────────────────────

INSERT INTO notifications (
  id, workspace_id, recipient_user_id, template_id, channel, status, payload, created_at
) VALUES 
  ('b1000000-0000-0000-0000-000000000001',
   '10000000-0000-0000-0000-000000000001',
   '20000000-0000-0000-0000-000000000001',
   'a1000000-0000-0000-0000-000000000001', -- post.published
   'in_app',
   'sent', -- unread
   '{"postTitle": "Summer Campaign 2026", "platform": "Instagram", "link": "/app/calendar"}',
   NOW() - INTERVAL '2 hours'),
   
  ('b1000000-0000-0000-0000-000000000002',
   '10000000-0000-0000-0000-000000000001',
   '20000000-0000-0000-0000-000000000001',
   'a1000000-0000-0000-0000-000000000003', -- approvals.required
   'in_app',
   'read', -- already read
   '{"actorName": "Priya S.", "platform": "LinkedIn"}',
   NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- ── Settings ─────────────────────────────────────────────────────────────

INSERT INTO notification_preferences (
  id, workspace_id, user_id, event_code, channel, is_enabled
) VALUES
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'post.published', 'in_app', true),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'approvals.required', 'in_app', true)
ON CONFLICT (workspace_id, user_id, event_code, channel) DO NOTHING;
