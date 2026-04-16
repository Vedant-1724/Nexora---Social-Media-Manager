SET search_path TO scheduler_service;

INSERT INTO media_assets (
  id,
  workspace_id,
  storage_provider,
  bucket_name,
  object_key,
  mime_type,
  media_kind,
  size_bytes,
  sha256_checksum,
  uploaded_by_user_id,
  alt_text
) VALUES (
  '40000000-0000-0000-0000-000000000011',
  '10000000-0000-0000-0000-000000000001',
  'minio',
  'nexora-assets',
  'campaigns/spring-launch/hero.png',
  'image/png',
  'image',
  245760,
  'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  '00000000-0000-0000-0000-000000000101',
  'Spring launch hero visual'
);

INSERT INTO content_templates (
  id,
  workspace_id,
  name,
  template_kind,
  body,
  created_by_user_id
) VALUES (
  '40000000-0000-0000-0000-000000000012',
  '10000000-0000-0000-0000-000000000001',
  'Thought Leadership Launch',
  'caption',
  'Launching a new chapter with premium social orchestration.',
  '00000000-0000-0000-0000-000000000101'
);

INSERT INTO post_drafts (
  id,
  workspace_id,
  author_user_id,
  title,
  lifecycle_status,
  primary_timezone,
  approval_route_id,
  campaign_label,
  scheduled_summary
) VALUES (
  '40000000-0000-0000-0000-000000000021',
  '10000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000101',
  'Spring Launch Announcement',
  'scheduled',
  'Asia/Calcutta',
  '10000000-0000-0000-0000-000000000041',
  'Spring Launch',
  '{"channels": ["meta", "linkedin", "x"]}'::jsonb
);

INSERT INTO draft_asset_links (draft_id, asset_id, sort_order) VALUES (
  '40000000-0000-0000-0000-000000000021',
  '40000000-0000-0000-0000-000000000011',
  0
);

INSERT INTO post_variants (
  id,
  draft_id,
  provider,
  caption,
  link_url,
  first_comment,
  provider_options
) VALUES
  (
    '40000000-0000-0000-0000-000000000031',
    '40000000-0000-0000-0000-000000000021',
    'linkedin',
    'Launching a premium social workflow for modern teams.',
    'https://nexora.example/launch',
    NULL,
    '{"audience": "b2b"}'::jsonb
  ),
  (
    '40000000-0000-0000-0000-000000000032',
    '40000000-0000-0000-0000-000000000021',
    'x',
    'A cleaner way to plan, approve, and publish campaigns.',
    'https://nexora.example/launch',
    'Schedule smarter with Nexora.',
    '{"thread": false}'::jsonb
  );

INSERT INTO approval_requests (
  id,
  draft_id,
  approval_route_id,
  status,
  requested_by_user_id,
  requested_at,
  resolved_at
) VALUES (
  '40000000-0000-0000-0000-000000000041',
  '40000000-0000-0000-0000-000000000021',
  '10000000-0000-0000-0000-000000000041',
  'approved',
  '00000000-0000-0000-0000-000000000101',
  NOW() - INTERVAL '1 day',
  NOW() - INTERVAL '22 hours'
);

INSERT INTO approval_decisions (
  id,
  approval_request_id,
  step_number,
  decision,
  acted_by_user_id,
  comment_text
) VALUES (
  '40000000-0000-0000-0000-000000000042',
  '40000000-0000-0000-0000-000000000041',
  1,
  'approved',
  '00000000-0000-0000-0000-000000000101',
  'Approved for launch window.'
);

INSERT INTO scheduled_jobs (
  id,
  draft_id,
  scheduled_for,
  timezone,
  status,
  retry_count,
  next_attempt_at
) VALUES (
  '40000000-0000-0000-0000-000000000051',
  '40000000-0000-0000-0000-000000000021',
  NOW() + INTERVAL '8 hours',
  'Asia/Calcutta',
  'queued',
  0,
  NOW() + INTERVAL '8 hours'
);

INSERT INTO publish_attempts (
  id,
  scheduled_job_id,
  provider,
  attempt_number,
  outcome,
  provider_post_id,
  error_details,
  completed_at
) VALUES (
  '40000000-0000-0000-0000-000000000061',
  '40000000-0000-0000-0000-000000000051',
  'linkedin',
  1,
  'pending',
  NULL,
  '{}'::jsonb,
  NULL
);

INSERT INTO import_batches (
  id,
  workspace_id,
  source_type,
  status,
  uploaded_by_user_id,
  source_reference,
  summary
) VALUES (
  '40000000-0000-0000-0000-000000000071',
  '10000000-0000-0000-0000-000000000001',
  'csv',
  'completed',
  '00000000-0000-0000-0000-000000000101',
  'phase2-seed.csv',
  '{"importedDrafts": 12, "failedDrafts": 0}'::jsonb
);
