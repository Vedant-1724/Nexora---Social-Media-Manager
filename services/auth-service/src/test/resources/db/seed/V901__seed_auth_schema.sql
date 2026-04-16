SET search_path TO auth_service;

INSERT INTO user_identities (
  id,
  email,
  display_name,
  account_status,
  locale_code,
  default_timezone
) VALUES (
  '00000000-0000-0000-0000-000000000101',
  'admin@nexora.dev',
  'Nexora Admin',
  'active',
  'en',
  'Asia/Calcutta'
);

INSERT INTO auth_methods (
  id,
  user_id,
  method_type,
  subject,
  secret_hash,
  is_primary,
  verified_at,
  metadata
) VALUES (
  '00000000-0000-0000-0000-000000000111',
  '00000000-0000-0000-0000-000000000101',
  'password',
  'admin@nexora.dev',
  '$argon2id$v=19$m=65536,t=3,p=1$phase2seed',
  TRUE,
  NOW(),
  '{"seeded": true}'::jsonb
);

INSERT INTO refresh_sessions (
  id,
  user_id,
  token_hash,
  workspace_context_id,
  status,
  issued_at,
  expires_at,
  ip_address,
  user_agent
) VALUES (
  '00000000-0000-0000-0000-000000000121',
  '00000000-0000-0000-0000-000000000101',
  'seeded-refresh-token-hash',
  '10000000-0000-0000-0000-000000000001',
  'active',
  NOW(),
  NOW() + INTERVAL '30 days',
  '127.0.0.1',
  'Phase2SeedAgent'
);

INSERT INTO verification_challenges (
  id,
  user_id,
  challenge_type,
  token_hash,
  status,
  expires_at,
  metadata
) VALUES (
  '00000000-0000-0000-0000-000000000131',
  '00000000-0000-0000-0000-000000000101',
  'email_verification',
  'seeded-verification-token-hash',
  'pending',
  NOW() + INTERVAL '2 days',
  '{"channel": "email"}'::jsonb
);
