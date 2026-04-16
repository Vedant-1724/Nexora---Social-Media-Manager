CREATE SCHEMA IF NOT EXISTS social_service;
SET search_path TO social_service;

CREATE TABLE connected_accounts (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  provider TEXT NOT NULL CHECK (provider IN ('meta', 'linkedin', 'x')),
  external_account_id TEXT NOT NULL,
  external_organization_id TEXT,
  display_name TEXT NOT NULL,
  username TEXT,
  status TEXT NOT NULL DEFAULT 'active'
    CHECK (status IN ('active', 'expired', 'revoked', 'reauthorization_required')),
  access_token_ciphertext TEXT NOT NULL,
  refresh_token_ciphertext TEXT,
  token_expires_at TIMESTAMPTZ,
  scopes JSONB NOT NULL DEFAULT '[]'::jsonb,
  connected_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (provider, external_account_id)
);

CREATE INDEX idx_connected_accounts_workspace_status
  ON connected_accounts (workspace_id, status);

CREATE TABLE account_capabilities (
  connected_account_id UUID NOT NULL REFERENCES connected_accounts(id) ON DELETE CASCADE,
  capability_code TEXT NOT NULL,
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  configured_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (connected_account_id, capability_code)
);

CREATE TABLE webhook_subscriptions (
  id UUID PRIMARY KEY,
  connected_account_id UUID NOT NULL REFERENCES connected_accounts(id) ON DELETE CASCADE,
  provider_subscription_id TEXT NOT NULL UNIQUE,
  callback_path TEXT NOT NULL,
  secret_ciphertext TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending', 'active', 'disabled')),
  subscribed_at TIMESTAMPTZ,
  last_validated_at TIMESTAMPTZ
);

CREATE TABLE inbound_inbox_events (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  connected_account_id UUID NOT NULL REFERENCES connected_accounts(id) ON DELETE CASCADE,
  provider TEXT NOT NULL CHECK (provider IN ('meta', 'linkedin', 'x')),
  external_event_id TEXT NOT NULL,
  external_thread_id TEXT,
  event_type TEXT NOT NULL CHECK (event_type IN ('comment', 'mention', 'dm')),
  actor_handle TEXT,
  event_payload JSONB NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (provider, external_event_id)
);

CREATE INDEX idx_inbound_inbox_events_workspace_received_at
  ON inbound_inbox_events (workspace_id, received_at DESC);
