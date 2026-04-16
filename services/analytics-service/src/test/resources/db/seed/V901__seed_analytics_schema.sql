-- ═══════════════════════════════════════════════════════════════════════════
-- V2: Seed 30 days of realistic analytics data for the development workspace
-- Workspace: 10000000-0000-0000-0000-000000000001 (Northstar Creative)
-- ═══════════════════════════════════════════════════════════════════════════

SET search_path TO analytics_service;

-- ── Account Daily Metrics: Meta ─────────────────────────────────────────────
-- Connected account: 30000000-0000-0000-0000-000000000001 (Meta)

INSERT INTO account_daily_metrics (id, workspace_id, connected_account_id, provider, metric_date, impressions, reach, engagements, comments, clicks, follower_delta)
SELECT
  gen_random_uuid(),
  '10000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000001',
  'meta',
  d::date,
  (12000 + (random() * 8000)::int),
  (9000 + (random() * 6000)::int),
  (800 + (random() * 600)::int),
  (40 + (random() * 60)::int),
  (200 + (random() * 300)::int),
  (10 + (random() * 30)::int)
FROM generate_series(CURRENT_DATE - INTERVAL '29 days', CURRENT_DATE, '1 day') AS d
ON CONFLICT (connected_account_id, metric_date) DO NOTHING;

-- ── Account Daily Metrics: LinkedIn ─────────────────────────────────────────
-- Connected account: 30000000-0000-0000-0000-000000000011

INSERT INTO account_daily_metrics (id, workspace_id, connected_account_id, provider, metric_date, impressions, reach, engagements, comments, clicks, follower_delta)
SELECT
  gen_random_uuid(),
  '10000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000011',
  'linkedin',
  d::date,
  (8000 + (random() * 12000)::int),
  (6000 + (random() * 9000)::int),
  (600 + (random() * 800)::int),
  (30 + (random() * 50)::int),
  (150 + (random() * 250)::int),
  (20 + (random() * 50)::int)
FROM generate_series(CURRENT_DATE - INTERVAL '29 days', CURRENT_DATE, '1 day') AS d
ON CONFLICT (connected_account_id, metric_date) DO NOTHING;

-- ── Account Daily Metrics: X (Twitter) ──────────────────────────────────────
-- Connected account: 30000000-0000-0000-0000-000000000021

INSERT INTO account_daily_metrics (id, workspace_id, connected_account_id, provider, metric_date, impressions, reach, engagements, comments, clicks, follower_delta)
SELECT
  gen_random_uuid(),
  '10000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000021',
  'x',
  d::date,
  (5000 + (random() * 7000)::int),
  (3500 + (random() * 5000)::int),
  (400 + (random() * 500)::int),
  (20 + (random() * 40)::int),
  (100 + (random() * 200)::int),
  (5 + (random() * 20)::int)
FROM generate_series(CURRENT_DATE - INTERVAL '29 days', CURRENT_DATE, '1 day') AS d
ON CONFLICT (connected_account_id, metric_date) DO NOTHING;

-- ── Content Daily Metrics: Top performing posts ─────────────────────────────

INSERT INTO content_daily_metrics (id, workspace_id, draft_id, provider, provider_post_id, metric_date, impressions, likes, comments, shares, clicks, saves, video_views) VALUES
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'meta', 'fb-post-001', CURRENT_DATE - INTERVAL '2 days', 48200, 2180, 142, 89, 1240, 67, 0),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002', 'meta', 'ig-reel-001', CURRENT_DATE - INTERVAL '3 days', 35800, 3100, 98, 45, 820, 234, 28400),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000003', 'x', 'tweet-thread-001', CURRENT_DATE - INTERVAL '1 day', 22100, 890, 67, 156, 440, 0, 0),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000004', 'linkedin', 'li-article-001', CURRENT_DATE - INTERVAL '4 days', 18700, 620, 84, 42, 380, 28, 0),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000005', 'linkedin', 'li-post-002', CURRENT_DATE - INTERVAL '5 days', 15300, 480, 56, 31, 290, 19, 0),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000006', 'meta', 'ig-story-001', CURRENT_DATE - INTERVAL '1 day', 12800, 1050, 23, 12, 180, 45, 11200),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000007', 'x', 'tweet-poll-001', CURRENT_DATE - INTERVAL '6 days', 9600, 340, 112, 28, 150, 0, 0),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000008', 'linkedin', 'li-carousel-001', CURRENT_DATE - INTERVAL '2 days', 8400, 290, 38, 22, 210, 14, 0)
ON CONFLICT (provider, provider_post_id, metric_date) DO NOTHING;

-- ── Workspace Rollups ───────────────────────────────────────────────────────

INSERT INTO workspace_rollups (id, workspace_id, granularity, period_start, period_end, totals) VALUES
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'week',
   CURRENT_DATE - INTERVAL '6 days', CURRENT_DATE,
   '{"impressions": 164200, "reach": 128400, "engagements": 12800, "comments": 740, "clicks": 4200, "followerDelta": 285}'::jsonb),
  (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'month',
   CURRENT_DATE - INTERVAL '29 days', CURRENT_DATE,
   '{"impressions": 682000, "reach": 534000, "engagements": 48600, "comments": 2840, "clicks": 16800, "followerDelta": 1120}'::jsonb)
ON CONFLICT (workspace_id, granularity, period_start, period_end) DO NOTHING;
