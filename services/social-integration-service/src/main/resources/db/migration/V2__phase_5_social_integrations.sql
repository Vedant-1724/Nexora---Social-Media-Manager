SET search_path TO social_service;

CREATE TABLE oauth_link_states (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  provider TEXT NOT NULL CHECK (provider IN ('meta', 'linkedin', 'x')),
  connected_by_user_id UUID NOT NULL,
  state_token_hash TEXT NOT NULL UNIQUE,
  code_verifier TEXT,
  requested_scopes JSONB NOT NULL DEFAULT '[]'::jsonb,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_oauth_link_states_workspace_provider
  ON oauth_link_states (workspace_id, provider, expires_at DESC);

ALTER TABLE connected_accounts
  ADD COLUMN token_encryption_key_id TEXT NOT NULL DEFAULT 'legacy',
  ADD COLUMN token_refreshed_at TIMESTAMPTZ,
  ADD COLUMN provider_account_type TEXT NOT NULL DEFAULT 'profile',
  ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN last_sync_at TIMESTAMPTZ;

CREATE INDEX idx_connected_accounts_workspace_provider
  ON connected_accounts (workspace_id, provider);

ALTER TABLE webhook_subscriptions
  ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
