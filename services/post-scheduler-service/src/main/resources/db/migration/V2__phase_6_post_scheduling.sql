SET search_path TO scheduler_service;

ALTER TABLE media_assets
  ADD COLUMN source_url TEXT;

ALTER TABLE post_drafts
  ADD COLUMN body TEXT NOT NULL DEFAULT '',
  ADD COLUMN metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN last_saved_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE TABLE variant_targets (
  variant_id UUID NOT NULL REFERENCES post_variants(id) ON DELETE CASCADE,
  connected_account_id UUID NOT NULL,
  PRIMARY KEY (variant_id, connected_account_id)
);

ALTER TABLE scheduled_jobs
  ADD COLUMN locked_at TIMESTAMPTZ,
  ADD COLUMN locked_by TEXT,
  ADD COLUMN dispatched_at TIMESTAMPTZ,
  ADD COLUMN published_at TIMESTAMPTZ,
  ADD COLUMN cancelled_at TIMESTAMPTZ;

ALTER TABLE publish_attempts
  ADD COLUMN connected_account_id UUID,
  ADD COLUMN provider_permalink TEXT;

UPDATE publish_attempts
SET connected_account_id = '30000000-0000-0000-0000-000000000011'
WHERE connected_account_id IS NULL;

ALTER TABLE publish_attempts
  ALTER COLUMN connected_account_id SET NOT NULL;

ALTER TABLE publish_attempts
  DROP CONSTRAINT publish_attempts_scheduled_job_id_provider_attempt_number_key;

ALTER TABLE publish_attempts
  ADD CONSTRAINT uk_publish_attempts_job_account_attempt
    UNIQUE (scheduled_job_id, connected_account_id, attempt_number);

CREATE INDEX idx_variant_targets_connected_account_id
  ON variant_targets (connected_account_id);

CREATE INDEX idx_scheduled_jobs_due_queue
  ON scheduled_jobs (status, next_attempt_at);

CREATE INDEX idx_publish_attempts_job_account
  ON publish_attempts (scheduled_job_id, connected_account_id, started_at DESC);
