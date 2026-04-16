SET search_path TO social_service;

INSERT INTO connected_accounts (
  id,
  workspace_id,
  provider,
  external_account_id,
  external_organization_id,
  display_name,
  username,
  status,
  access_token_ciphertext,
  refresh_token_ciphertext,
  token_expires_at,
  scopes,
  connected_by_user_id
) VALUES (
  '30000000-0000-0000-0000-000000000011',
  '10000000-0000-0000-0000-000000000001',
  'linkedin',
  'urn:li:organization:12345',
  'urn:li:organization:12345',
  'Northstar Creative',
  'northstarcreative',
  'active',
  'enc:access-token',
  'enc:refresh-token',
  NOW() + INTERVAL '45 days',
  '["w_member_social", "r_organization_social"]'::jsonb,
  '00000000-0000-0000-0000-000000000101'
);

INSERT INTO account_capabilities (
  connected_account_id,
  capability_code,
  is_enabled
) VALUES
  ('30000000-0000-0000-0000-000000000011', 'publish.text', TRUE),
  ('30000000-0000-0000-0000-000000000011', 'publish.image', TRUE),
  ('30000000-0000-0000-0000-000000000011', 'inbox.comments', TRUE);

INSERT INTO webhook_subscriptions (
  id,
  connected_account_id,
  provider_subscription_id,
  callback_path,
  secret_ciphertext,
  status,
  subscribed_at,
  last_validated_at
) VALUES (
  '30000000-0000-0000-0000-000000000021',
  '30000000-0000-0000-0000-000000000011',
  'li-webhook-subscription-001',
  '/api/v1/social/webhooks/linkedin',
  'enc:webhook-secret',
  'active',
  NOW(),
  NOW()
);

INSERT INTO inbound_inbox_events (
  id,
  workspace_id,
  connected_account_id,
  provider,
  external_event_id,
  external_thread_id,
  event_type,
  actor_handle,
  event_payload
) VALUES (
  '30000000-0000-0000-0000-000000000031',
  '10000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000011',
  'linkedin',
  'evt-001',
  'thread-001',
  'comment',
  '@brand-lead',
  '{"message": "Looks ready for launch.", "sentiment": "positive"}'::jsonb
);

INSERT INTO oauth_link_states (
  id,
  workspace_id,
  provider,
  connected_by_user_id,
  state_token_hash,
  code_verifier,
  requested_scopes,
  expires_at
) VALUES (
  '30000000-0000-0000-0000-000000000041',
  '10000000-0000-0000-0000-000000000001',
  'linkedin',
  '00000000-0000-0000-0000-000000000101',
  'seeded-state-hash',
  NULL,
  '["w_member_social", "r_liteprofile"]'::jsonb,
  NOW() + INTERVAL '15 minutes'
);
