CREATE SCHEMA IF NOT EXISTS analytics_service;
SET search_path TO analytics_service;

CREATE TABLE watch_queries (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  query_type TEXT NOT NULL CHECK (query_type IN ('keyword', 'hashtag', 'competitor')),
  query_text TEXT NOT NULL,
  alert_config JSONB NOT NULL DEFAULT '{}'::jsonb,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_by_user_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE account_daily_metrics (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  connected_account_id UUID NOT NULL,
  provider TEXT NOT NULL CHECK (provider IN ('meta', 'linkedin', 'x')),
  metric_date DATE NOT NULL,
  impressions BIGINT NOT NULL DEFAULT 0,
  reach BIGINT NOT NULL DEFAULT 0,
  engagements BIGINT NOT NULL DEFAULT 0,
  comments BIGINT NOT NULL DEFAULT 0,
  clicks BIGINT NOT NULL DEFAULT 0,
  follower_delta INTEGER NOT NULL DEFAULT 0,
  UNIQUE (connected_account_id, metric_date)
);

CREATE TABLE content_daily_metrics (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  draft_id UUID NOT NULL,
  provider TEXT NOT NULL CHECK (provider IN ('meta', 'linkedin', 'x')),
  provider_post_id TEXT NOT NULL,
  metric_date DATE NOT NULL,
  impressions BIGINT NOT NULL DEFAULT 0,
  likes BIGINT NOT NULL DEFAULT 0,
  comments BIGINT NOT NULL DEFAULT 0,
  shares BIGINT NOT NULL DEFAULT 0,
  clicks BIGINT NOT NULL DEFAULT 0,
  saves BIGINT NOT NULL DEFAULT 0,
  video_views BIGINT NOT NULL DEFAULT 0,
  UNIQUE (provider, provider_post_id, metric_date)
);

CREATE TABLE workspace_rollups (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  granularity TEXT NOT NULL CHECK (granularity IN ('day', 'week', 'month')),
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  totals JSONB NOT NULL DEFAULT '{}'::jsonb,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (workspace_id, granularity, period_start, period_end)
);

CREATE TABLE listening_mentions (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  watch_query_id UUID NOT NULL REFERENCES watch_queries(id) ON DELETE CASCADE,
  provider TEXT NOT NULL,
  source_type TEXT NOT NULL CHECK (source_type IN ('social', 'news', 'blog')),
  reference_key TEXT NOT NULL UNIQUE,
  author_handle TEXT,
  sentiment TEXT NOT NULL CHECK (sentiment IN ('positive', 'neutral', 'negative', 'unknown')),
  content_excerpt TEXT NOT NULL,
  mentioned_at TIMESTAMPTZ NOT NULL,
  raw_payload JSONB NOT NULL
);

CREATE TABLE report_exports (
  id UUID PRIMARY KEY,
  workspace_id UUID NOT NULL,
  requested_by_user_id UUID NOT NULL,
  report_type TEXT NOT NULL,
  format TEXT NOT NULL CHECK (format IN ('csv', 'pdf')),
  filters JSONB NOT NULL DEFAULT '{}'::jsonb,
  storage_key TEXT,
  status TEXT NOT NULL CHECK (status IN ('queued', 'processing', 'completed', 'failed')),
  requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ
);
