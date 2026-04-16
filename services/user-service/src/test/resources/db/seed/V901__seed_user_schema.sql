SET search_path TO user_service;

INSERT INTO workspaces (
  id,
  slug,
  name,
  status,
  billing_email,
  default_locale,
  default_timezone
) VALUES (
  '10000000-0000-0000-0000-000000000001',
  'northstar-creative',
  'Northstar Creative',
  'active',
  'finance@northstar.example',
  'en',
  'Asia/Calcutta'
);

INSERT INTO roles (
  id,
  workspace_id,
  scope,
  code,
  name,
  description,
  is_system
) VALUES
  (
    '10000000-0000-0000-0000-000000000011',
    NULL,
    'system',
    'workspace_admin',
    'Workspace Admin',
    'Owns workspace administration and approvals.',
    TRUE
  ),
  (
    '10000000-0000-0000-0000-000000000012',
    NULL,
    'system',
    'approver',
    'Approver',
    'Can approve queued posts.',
    TRUE
  ),
  (
    '10000000-0000-0000-0000-000000000013',
    NULL,
    'system',
    'content_editor',
    'Content Editor',
    'Can draft and schedule content.',
    TRUE
  );

INSERT INTO role_permissions (role_id, permission_key) VALUES
  ('10000000-0000-0000-0000-000000000011', 'workspace.manage'),
  ('10000000-0000-0000-0000-000000000011', 'posts.approve'),
  ('10000000-0000-0000-0000-000000000012', 'posts.approve'),
  ('10000000-0000-0000-0000-000000000013', 'posts.create');

INSERT INTO workspace_memberships (
  id,
  workspace_id,
  user_id,
  role_id,
  status,
  joined_at
) VALUES (
  '10000000-0000-0000-0000-000000000021',
  '10000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000101',
  '10000000-0000-0000-0000-000000000011',
  'active',
  NOW()
);

INSERT INTO workspace_invites (
  id,
  workspace_id,
  email,
  role_id,
  invited_by_user_id,
  token_hash,
  status,
  expires_at
) VALUES (
  '10000000-0000-0000-0000-000000000031',
  '10000000-0000-0000-0000-000000000001',
  'approver@northstar.example',
  '10000000-0000-0000-0000-000000000012',
  '00000000-0000-0000-0000-000000000101',
  'seeded-invite-token-hash',
  'pending',
  NOW() + INTERVAL '7 days'
);

INSERT INTO approval_routes (
  id,
  workspace_id,
  name,
  description,
  min_approvers,
  is_default
) VALUES (
  '10000000-0000-0000-0000-000000000041',
  '10000000-0000-0000-0000-000000000001',
  'Default Campaign Approval',
  'Single-step approval for scheduled posts.',
  1,
  TRUE
);

INSERT INTO approval_route_steps (
  id,
  approval_route_id,
  step_order,
  role_id,
  approver_user_id,
  required_approvals
) VALUES (
  '10000000-0000-0000-0000-000000000051',
  '10000000-0000-0000-0000-000000000041',
  1,
  '10000000-0000-0000-0000-000000000012',
  NULL,
  1
);

INSERT INTO audit_entries (
  id,
  workspace_id,
  actor_user_id,
  source_service,
  action_type,
  target_type,
  target_id,
  metadata
) VALUES (
  '10000000-0000-0000-0000-000000000061',
  '10000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000101',
  'user-service',
  'workspace.member.invited',
  'workspace_invites',
  '10000000-0000-0000-0000-000000000031',
  '{"email": "approver@northstar.example"}'::jsonb
);
