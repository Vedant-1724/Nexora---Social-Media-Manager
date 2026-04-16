CREATE SCHEMA IF NOT EXISTS auth_service;
SET search_path TO auth_service;

CREATE TABLE user_identities (
  id UUID PRIMARY KEY,
  email TEXT NOT NULL,
  display_name TEXT NOT NULL,
  account_status TEXT NOT NULL DEFAULT 'pending'
    CHECK (account_status IN ('pending', 'active', 'suspended', 'deleted')),
  locale_code TEXT NOT NULL DEFAULT 'en',
  default_timezone TEXT NOT NULL DEFAULT 'Asia/Calcutta',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_user_identities_email_lower
  ON user_identities (LOWER(email));

CREATE TABLE auth_methods (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES user_identities(id) ON DELETE CASCADE,
  method_type TEXT NOT NULL
    CHECK (method_type IN ('password', 'magic_link', 'sso_placeholder')),
  subject TEXT NOT NULL,
  secret_hash TEXT,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  verified_at TIMESTAMPTZ,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_auth_methods_provider_subject
  ON auth_methods (method_type, subject);

CREATE UNIQUE INDEX uk_auth_methods_user_primary
  ON auth_methods (user_id)
  WHERE is_primary = TRUE;

CREATE TABLE refresh_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES user_identities(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL UNIQUE,
  workspace_context_id UUID,
  status TEXT NOT NULL DEFAULT 'active'
    CHECK (status IN ('active', 'revoked', 'expired')),
  issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  ip_address TEXT,
  user_agent TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_sessions_user_status
  ON refresh_sessions (user_id, status);

CREATE TABLE verification_challenges (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES user_identities(id) ON DELETE CASCADE,
  challenge_type TEXT NOT NULL
    CHECK (challenge_type IN ('email_verification', 'password_reset', 'mfa_enrollment')),
  token_hash TEXT NOT NULL UNIQUE,
  status TEXT NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending', 'consumed', 'expired')),
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
