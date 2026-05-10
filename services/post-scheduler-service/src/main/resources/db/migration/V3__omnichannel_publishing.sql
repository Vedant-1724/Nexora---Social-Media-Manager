SET search_path TO scheduler_service;

-- ── Widen provider CHECK on post_variants ────────────────────────────────────
ALTER TABLE post_variants
  DROP CONSTRAINT IF EXISTS post_variants_provider_check;

ALTER TABLE post_variants
  ADD CONSTRAINT post_variants_provider_check
    CHECK (provider IN ('meta', 'linkedin', 'x', 'instagram', 'tiktok', 'pinterest', 'youtube', 'threads', 'bluesky'));

-- ── Widen provider CHECK on publish_attempts ─────────────────────────────────
ALTER TABLE publish_attempts
  DROP CONSTRAINT IF EXISTS publish_attempts_provider_check;

ALTER TABLE publish_attempts
  ADD CONSTRAINT publish_attempts_provider_check
    CHECK (provider IN ('meta', 'linkedin', 'x', 'instagram', 'tiktok', 'pinterest', 'youtube', 'threads', 'bluesky'));

-- ── Import Batch Items ───────────────────────────────────────────────────────
CREATE TABLE import_batch_items (
  id UUID PRIMARY KEY,
  batch_id UUID NOT NULL REFERENCES import_batches(id) ON DELETE CASCADE,
  row_number INTEGER NOT NULL CHECK (row_number > 0),
  draft_id UUID REFERENCES post_drafts(id) ON DELETE SET NULL,
  raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
  validation_status TEXT NOT NULL CHECK (validation_status IN ('pending', 'valid', 'invalid', 'processed', 'failed')),
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  processed_at TIMESTAMPTZ,
  UNIQUE (batch_id, row_number)
);

CREATE INDEX idx_import_batch_items_batch_id ON import_batch_items (batch_id);
CREATE INDEX idx_import_batch_items_status ON import_batch_items (batch_id, validation_status);

-- ── Link-in-Bio Pages ────────────────────────────────────────────────────────
CREATE TABLE link_in_bio_pages (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  slug TEXT NOT NULL UNIQUE,
  title TEXT NOT NULL,
  bio_text TEXT,
  avatar_url TEXT,
  theme_config JSONB NOT NULL DEFAULT '{}'::jsonb,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_link_in_bio_pages_workspace ON link_in_bio_pages (workspace_id);
CREATE INDEX idx_link_in_bio_pages_slug ON link_in_bio_pages (slug);

-- ── Link-in-Bio Entries ──────────────────────────────────────────────────────
CREATE TABLE link_in_bio_entries (
  id UUID PRIMARY KEY,
  page_id UUID NOT NULL REFERENCES link_in_bio_pages(id) ON DELETE CASCADE,
  draft_id UUID REFERENCES post_drafts(id) ON DELETE SET NULL,
  external_url TEXT,
  thumbnail_url TEXT,
  label TEXT NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
  is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_link_in_bio_entries_page ON link_in_bio_entries (page_id, sort_order);

-- ── Optimal Send Time Cache ─────────────────────────────────────────────────
CREATE TABLE optimal_send_time_cache (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  connected_account_id UUID,
  provider TEXT NOT NULL,
  day_of_week INTEGER NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6),
  hour_utc INTEGER NOT NULL CHECK (hour_utc >= 0 AND hour_utc <= 23),
  score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  sample_size INTEGER NOT NULL DEFAULT 0,
  computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (workspace_id, provider, day_of_week, hour_utc, connected_account_id)
);

CREATE INDEX idx_optimal_send_time_workspace ON optimal_send_time_cache (workspace_id, provider);
