CREATE SCHEMA IF NOT EXISTS scheduler_service;
SET search_path TO scheduler_service;

CREATE TABLE media_assets (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  storage_provider TEXT NOT NULL CHECK (storage_provider IN ('minio', 's3')),
  bucket_name TEXT NOT NULL,
  object_key TEXT NOT NULL,
  mime_type TEXT NOT NULL,
  media_kind TEXT NOT NULL CHECK (media_kind IN ('image', 'video', 'carousel', 'document')),
  size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
  sha256_checksum CHAR(64) NOT NULL,
  uploaded_by_user_id UUID NOT NULL,
  alt_text TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (bucket_name, object_key)
);

CREATE TABLE content_templates (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  name TEXT NOT NULL,
  template_kind TEXT NOT NULL CHECK (template_kind IN ('caption', 'campaign', 'approval_note')),
  body TEXT NOT NULL,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE post_drafts (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  author_user_id UUID NOT NULL,
  title TEXT NOT NULL,
  lifecycle_status TEXT NOT NULL
    CHECK (lifecycle_status IN ('draft', 'pending_approval', 'approved', 'scheduled', 'publishing', 'published', 'failed', 'cancelled')),
  primary_timezone TEXT NOT NULL,
  approval_route_id UUID,
  campaign_label TEXT,
  scheduled_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE draft_asset_links (
  draft_id UUID NOT NULL REFERENCES post_drafts(id) ON DELETE CASCADE,
  asset_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
  sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
  PRIMARY KEY (draft_id, asset_id)
);

CREATE TABLE post_variants (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL REFERENCES post_drafts(id) ON DELETE CASCADE,
  provider TEXT NOT NULL CHECK (provider IN ('meta', 'linkedin', 'x')),
  caption TEXT NOT NULL,
  link_url TEXT,
  first_comment TEXT,
  provider_options JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (draft_id, provider)
);

CREATE TABLE approval_requests (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL REFERENCES post_drafts(id) ON DELETE CASCADE UNIQUE,
  approval_route_id UUID,
  status TEXT NOT NULL
    CHECK (status IN ('pending', 'approved', 'rejected', 'changes_requested', 'cancelled')),
  requested_by_user_id UUID NOT NULL,
  requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMPTZ
);

CREATE TABLE approval_decisions (
  id UUID PRIMARY KEY,
  approval_request_id UUID NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
  step_number INTEGER NOT NULL CHECK (step_number > 0),
  decision TEXT NOT NULL CHECK (decision IN ('approved', 'rejected', 'changes_requested')),
  acted_by_user_id UUID NOT NULL,
  comment_text TEXT,
  acted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (approval_request_id, step_number, acted_by_user_id)
);

CREATE TABLE scheduled_jobs (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL REFERENCES post_drafts(id) ON DELETE CASCADE UNIQUE,
  scheduled_for TIMESTAMPTZ NOT NULL,
  timezone TEXT NOT NULL,
  status TEXT NOT NULL
    CHECK (status IN ('queued', 'locked', 'dispatching', 'published', 'failed', 'cancelled')),
  retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
  next_attempt_at TIMESTAMPTZ,
  last_error_code TEXT,
  last_error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE publish_attempts (
  id UUID PRIMARY KEY,
  scheduled_job_id UUID NOT NULL REFERENCES scheduled_jobs(id) ON DELETE CASCADE,
  provider TEXT NOT NULL CHECK (provider IN ('meta', 'linkedin', 'x')),
  attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
  outcome TEXT NOT NULL CHECK (outcome IN ('pending', 'success', 'failure')),
  provider_post_id TEXT,
  error_details JSONB NOT NULL DEFAULT '{}'::jsonb,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  UNIQUE (scheduled_job_id, provider, attempt_number)
);

CREATE TABLE import_batches (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  source_type TEXT NOT NULL CHECK (source_type IN ('csv', 'rss')),
  status TEXT NOT NULL CHECK (status IN ('uploaded', 'processing', 'completed', 'failed')),
  uploaded_by_user_id UUID NOT NULL,
  source_reference TEXT NOT NULL,
  summary JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ
);
