-- ═══════════════════════════════════════════════════════════════════════════
-- V2: Seed basic pricing plans and an active test subscription
-- Workspace: 10000000-0000-0000-0000-000000000001 (Northstar Creative)
-- ═══════════════════════════════════════════════════════════════════════════

SET search_path TO billing_service;

-- ── Pricing Plans ────────────────────────────────────────────────────────

INSERT INTO plans (id, code, name, billing_interval, price_minor, currency, seat_limit, social_account_limit, monthly_post_limit, features) VALUES
  ('60000000-0000-0000-0000-000000000011', 'free', 'Free Tier', 'monthly', 0, 'USD', 1, 2, 50,
   '{"aiCredits": 10, "analytics": "basic", "premiumSupport": false}'::jsonb),
  
  ('60000000-0000-0000-0000-000000000012', 'pro', 'Pro Plan', 'monthly', 2900, 'USD', 5, 10, 500,
   '{"aiCredits": 200, "analytics": "advanced", "premiumSupport": false}'::jsonb),
   
  ('60000000-0000-0000-0000-000000000013', 'enterprise', 'Enterprise', 'monthly', 9900, 'USD', null, null, null,
   '{"aiCredits": -1, "analytics": "advanced", "premiumSupport": true, "customSla": true}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- ── Subscriptions ────────────────────────────────────────────────────────

-- Test Workspace subscription (Pro Plan)
INSERT INTO subscriptions (
  id, workspace_id, plan_id, provider, provider_subscription_id,
  status, seat_count, trial_ends_at, current_period_start, current_period_end,
  cancel_at_period_end, created_at
) VALUES (
  '70000000-0000-0000-0000-000000000021',
  '10000000-0000-0000-0000-000000000001',
  '60000000-0000-0000-0000-000000000012', -- Pro
  'razorpay',
  'sub_razorpay_mock_12345',
  'active',
  3,
  NULL,
  NOW() - INTERVAL '15 days',
  NOW() + INTERVAL '15 days',
  FALSE,
  NOW() - INTERVAL '45 days'
) ON CONFLICT (workspace_id) DO NOTHING;

-- ── Invoices ─────────────────────────────────────────────────────────────

INSERT INTO invoices (
  id, subscription_id, provider_invoice_id, status, amount_due_minor,
  amount_paid_minor, currency, hosted_invoice_url, issued_at, due_at, paid_at
) VALUES 
  ('80000000-0000-0000-0000-000000000031', '70000000-0000-0000-0000-000000000021', 'inv_mock_prev_001', 'paid', 2900, 2900, 'USD', 'https://mock.invoice.url/prev', NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days'),
  ('80000000-0000-0000-0000-000000000032', '70000000-0000-0000-0000-000000000021', 'inv_mock_curr_001', 'paid', 2900, 2900, 'USD', 'https://mock.invoice.url/curr', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days')
ON CONFLICT (provider_invoice_id) DO NOTHING;

-- ── Entitlements ─────────────────────────────────────────────────────────

INSERT INTO entitlement_snapshots (
  id, workspace_id, subscription_id, plan_code, limits, features, generated_at
) VALUES (
  '90000000-0000-0000-0000-000000000041',
  '10000000-0000-0000-0000-000000000001',
  '70000000-0000-0000-0000-000000000021',
  'pro',
  '{"seatLimit": 5, "socialAccountLimit": 10, "monthlyPostLimit": 500}'::jsonb,
  '{"aiCredits": 200, "analytics": "advanced", "premiumSupport": false}'::jsonb,
  NOW()
) ON CONFLICT (workspace_id) DO NOTHING;
