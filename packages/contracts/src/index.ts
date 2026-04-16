// ── Social API Contracts ────────────────────────────────────────────────────

export type SocialProvider = "meta" | "linkedin" | "x";

export type SocialCapability =
  | "publish.text"
  | "publish.image"
  | "inbox.comments"
  | "inbox.messages"
  | "inbox.mentions";

export type ConnectedSocialAccount = {
  connectedAccountId: string;
  workspaceId: string;
  provider: SocialProvider;
  externalAccountId: string;
  externalOrganizationId: string | null;
  providerAccountType: string;
  displayName: string;
  username: string | null;
  status: "active" | "expired" | "revoked" | "reauthorization_required";
  tokenExpiresAt: string | null;
  tokenRefreshedAt: string | null;
  scopes: string[];
  capabilities: SocialCapability[];
  lastSyncAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type SocialProviderDescriptor = {
  provider: SocialProvider;
  displayName: string;
  enabled: boolean;
  pkceRequired: boolean;
  callbackPath: string;
  defaultScopes: string[];
  capabilities: SocialCapability[];
  futureProviderHints: string[];
};

export type CreateSocialLinkSessionRequest = {
  provider: SocialProvider;
  scopes?: string[];
};

export type SocialLinkSession = {
  provider: SocialProvider;
  authorizationUrl: string;
  callbackUrl: string;
  expiresAt: string;
  scopes: string[];
  capabilities: SocialCapability[];
};

export type SocialOAuthCallbackResponse = {
  status: "linked";
  provider: SocialProvider;
  account: ConnectedSocialAccount;
};

export type PublishToSocialAccountRequest = {
  connectedAccountId: string;
  message: string;
  linkUrl?: string;
  mediaUrls?: string[];
  replyToExternalPostId?: string;
  metadata?: Record<string, unknown>;
};

export type SocialPublication = {
  connectedAccountId: string;
  provider: SocialProvider;
  externalPostId: string;
  providerPermalink: string | null;
  publishedAt: string;
};

export type WebhookDeliveryStatus = {
  provider: SocialProvider;
  receivedEvents: number;
  acceptedEvents: number;
  status: string;
};

// ── Auth API Contracts ──────────────────────────────────────────────────────

export type AuthWorkspace = {
  membershipId: string;
  workspaceId: string;
  workspaceName: string;
  workspaceSlug: string;
  workspaceStatus: string;
  roleCode: string;
  roleName: string;
  membershipStatus: string;
  joinedAt: string | null;
  permissions: string[];
};

export type AuthUser = {
  userId: string;
  email: string;
  displayName: string;
  accountStatus: string;
  locale: string;
  timezone: string;
  emailVerified: boolean;
};

export type AuthSessionTokens = {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
};

export type DevelopmentTokens = {
  emailVerificationToken: string | null;
  passwordResetToken: string | null;
};

export type AuthSessionResponse = {
  user: AuthUser | null;
  currentWorkspace: AuthWorkspace | null;
  workspaces: AuthWorkspace[];
  session: AuthSessionTokens | null;
  developmentTokens: DevelopmentTokens;
};

export type ChallengeDispatchResponse = {
  challengeType: string;
  accepted: boolean;
  expiresAt: string | null;
  challengeToken: string | null;
};

export type ChallengeCompletionResponse = {
  email: string;
  challengeType: string;
  completedAt: string;
};

export type LoginRequest = {
  email: string;
  password: string;
  workspaceId?: string;
};

export type SignUpRequest = {
  email: string;
  password: string;
  displayName: string;
  workspaceName?: string;
  locale?: string;
  timezone?: string;
};

// ── Platform API Contracts ──────────────────────────────────────────────────

export type ApiEnvelope<T> = {
  data: T;
  meta?: Record<string, string | number | boolean | null>;
};

export type AuthContext = {
  userId: string;
  workspaceId: string;
  scopes: string[];
};

export type ServiceDescriptor = {
  service: string;
  version: string;
  phase: string;
  description: string;
};

export type WorkspaceSummary = {
  id: string;
  name: string;
  plan: "free" | "basic" | "pro" | "enterprise";
  seatCount: number;
};

export type DashboardSnapshot = {
  workspace: WorkspaceSummary;
  scheduledPosts: number;
  pendingApprovals: number;
  connectedChannels: number;
  weeklyReach: number;
};

// ── Scheduler API Contracts ─────────────────────────────────────────────────

export type SchedulerMediaKind = "image" | "video" | "carousel" | "document";
export type DraftLifecycleStatus =
  | "draft"
  | "pending_approval"
  | "approved"
  | "scheduled"
  | "publishing"
  | "published"
  | "failed"
  | "cancelled";

export type SchedulerJobStatus =
  | "queued"
  | "locked"
  | "dispatching"
  | "published"
  | "failed"
  | "cancelled";

export type ApprovalRequestStatus =
  | "pending"
  | "approved"
  | "rejected"
  | "changes_requested"
  | "cancelled";

export type SchedulerMediaAsset = {
  assetId: string;
  workspaceId: string;
  storageProvider: string;
  bucketName: string;
  objectKey: string;
  mimeType: string;
  mediaKind: SchedulerMediaKind;
  sizeBytes: number;
  sha256Checksum: string;
  uploadedByUserId: string;
  altText: string | null;
  sourceUrl: string | null;
  createdAt: string;
};

export type SchedulerVariant = {
  variantId: string;
  provider: SocialProvider;
  caption: string;
  linkUrl: string | null;
  firstComment: string | null;
  providerOptions: Record<string, unknown>;
  targetAccountIds: string[];
  createdAt: string;
};

export type SchedulerApprovalDecision = {
  approvalDecisionId: string;
  stepNumber: number;
  decision: "approved" | "rejected" | "changes_requested";
  actedByUserId: string;
  commentText: string | null;
  actedAt: string;
};

export type SchedulerApprovalRequest = {
  approvalRequestId: string;
  approvalRouteId: string | null;
  status: ApprovalRequestStatus;
  requestedByUserId: string;
  requestedAt: string;
  resolvedAt: string | null;
  decisions: SchedulerApprovalDecision[];
};

export type ScheduledJob = {
  scheduledJobId: string;
  draftId: string;
  scheduledFor: string;
  timezone: string;
  status: SchedulerJobStatus;
  retryCount: number;
  nextAttemptAt: string | null;
  lastErrorCode: string | null;
  lastErrorMessage: string | null;
  channels: SocialProvider[];
  targetAccountIds: string[];
  createdAt: string;
  lockedAt: string | null;
  lockedBy: string | null;
  dispatchedAt: string | null;
  publishedAt: string | null;
  cancelledAt: string | null;
};

export type DraftSummary = {
  draftId: string;
  workspaceId: string;
  authorUserId: string;
  title: string;
  body: string;
  lifecycleStatus: DraftLifecycleStatus;
  primaryTimezone: string;
  approvalRouteId: string | null;
  campaignLabel: string | null;
  scheduledSummary: Record<string, unknown>;
  metadata: Record<string, unknown>;
  scheduledJobId: string | null;
  scheduledFor: string | null;
  scheduleStatus: SchedulerJobStatus | null;
  createdAt: string;
  updatedAt: string;
  lastSavedAt: string;
};

export type DraftDetails = DraftSummary & {
  assets: SchedulerMediaAsset[];
  variants: SchedulerVariant[];
  approvalRequest: SchedulerApprovalRequest | null;
  scheduledJob: ScheduledJob | null;
};

export type SchedulerCalendarEntry = {
  scheduledJobId: string;
  draftId: string;
  title: string;
  campaignLabel: string | null;
  lifecycleStatus: DraftLifecycleStatus;
  approvalStatus: ApprovalRequestStatus | "not_requested";
  jobStatus: SchedulerJobStatus;
  scheduledFor: string;
  timezone: string;
  channels: SocialProvider[];
};

export type SaveDraftRequest = {
  title: string;
  body: string;
  primaryTimezone: string;
  approvalRouteId?: string;
  campaignLabel?: string;
  metadata?: Record<string, unknown>;
  mediaAssets: Array<{
    assetId?: string;
    storageProvider?: string;
    bucketName: string;
    objectKey: string;
    mimeType: string;
    mediaKind: SchedulerMediaKind;
    sizeBytes: number;
    sha256Checksum: string;
    altText?: string;
    sourceUrl?: string;
  }>;
  variants: Array<{
    provider: SocialProvider;
    caption: string;
    linkUrl?: string;
    firstComment?: string;
    providerOptions?: Record<string, unknown>;
    targetAccountIds?: string[];
  }>;
};

export type SubmitDraftApprovalRequest = {
  approvalRouteId?: string;
  note?: string;
};

export type RecordApprovalDecisionRequest = {
  stepNumber: number;
  decision: "approved" | "rejected" | "changes_requested";
  commentText?: string;
};

export type ScheduleDraftRequest = {
  scheduledFor: string;
  timezone: string;
};

export type DispatchScheduledJobResponse = {
  scheduledJobId: string;
  draftId: string | null;
  status: "skipped" | "awaiting_approval" | "failed" | "retrying" | "published";
  providerPostIds: Record<string, string>;
  failures: string[];
};

// ── Persistence Contracts ───────────────────────────────────────────────────

export type PersistenceOwnerService =
  | "auth-service"
  | "user-service"
  | "social-integration-service"
  | "post-scheduler-service"
  | "analytics-service"
  | "notification-service"
  | "billing-service";

export type TenantBoundary =
  | "global-user"
  | "workspace"
  | "workspace-read-model"
  | "system-catalog";

export type PersistenceContract = {
  service: PersistenceOwnerService;
  schema: string;
  table: string;
  tenantBoundary: TenantBoundary;
  primaryKey: string;
  containsPii: boolean;
  description: string;
  externalReferences: string[];
};

export const persistenceContracts: PersistenceContract[] = [
  {
    service: "auth-service",
    schema: "auth_service",
    table: "user_identities",
    tenantBoundary: "global-user",
    primaryKey: "id",
    containsPii: true,
    description: "Primary identity records, login email, lifecycle state, and locale defaults.",
    externalReferences: []
  },
  {
    service: "auth-service",
    schema: "auth_service",
    table: "refresh_sessions",
    tenantBoundary: "global-user",
    primaryKey: "id",
    containsPii: true,
    description: "Refresh token sessions with optional workspace context and revocation state.",
    externalReferences: ["user_identities.id", "user-service.workspaces.id"]
  },
  {
    service: "user-service",
    schema: "user_service",
    table: "workspaces",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: true,
    description: "Tenant root aggregate for agencies, brands, and workspace-level defaults.",
    externalReferences: []
  },
  {
    service: "user-service",
    schema: "user_service",
    table: "workspace_memberships",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: true,
    description: "Membership projection that links auth user IDs to workspace roles.",
    externalReferences: ["auth-service.user_identities.id", "user_service.roles.id"]
  },
  {
    service: "social-integration-service",
    schema: "social_service",
    table: "connected_accounts",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: true,
    description: "OAuth-backed provider accounts with encrypted tokens and account metadata.",
    externalReferences: ["user-service.workspaces.id", "auth-service.user_identities.id"]
  },
  {
    service: "social-integration-service",
    schema: "social_service",
    table: "inbound_inbox_events",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: true,
    description: "Normalized inbound comments, mentions, and DMs with dedupe keys.",
    externalReferences: ["social_service.connected_accounts.id"]
  },
  {
    service: "post-scheduler-service",
    schema: "scheduler_service",
    table: "post_drafts",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: false,
    description: "Draft post aggregate with lifecycle state, author, and approval linkage.",
    externalReferences: ["user-service.workspaces.id", "auth-service.user_identities.id"]
  },
  {
    service: "post-scheduler-service",
    schema: "scheduler_service",
    table: "scheduled_jobs",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: false,
    description: "Publish scheduling records, retry metadata, and dispatch lifecycle state.",
    externalReferences: ["scheduler_service.post_drafts.id"]
  },
  {
    service: "analytics-service",
    schema: "analytics_service",
    table: "account_daily_metrics",
    tenantBoundary: "workspace-read-model",
    primaryKey: "id",
    containsPii: false,
    description: "Daily per-account rollups for engagement and audience growth.",
    externalReferences: ["social-integration-service.connected_accounts.id"]
  },
  {
    service: "analytics-service",
    schema: "analytics_service",
    table: "listening_mentions",
    tenantBoundary: "workspace-read-model",
    primaryKey: "id",
    containsPii: true,
    description: "Search and listening read model for mentions, sentiment, and alert context.",
    externalReferences: ["analytics_service.watch_queries.id"]
  },
  {
    service: "notification-service",
    schema: "notification_service",
    table: "notifications",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: true,
    description: "Queued and delivered notifications for in-app and email channels.",
    externalReferences: ["notification_service.notification_templates.id", "auth-service.user_identities.id"]
  },
  {
    service: "billing-service",
    schema: "billing_service",
    table: "subscriptions",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: true,
    description: "Workspace subscription state, provider IDs, billing periods, and seat counts.",
    externalReferences: ["user-service.workspaces.id", "billing_service.plans.id"]
  },
  {
    service: "billing-service",
    schema: "billing_service",
    table: "entitlement_snapshots",
    tenantBoundary: "workspace",
    primaryKey: "id",
    containsPii: false,
    description: "Materialized feature limits and entitlements used for plan gating.",
    externalReferences: ["billing_service.subscriptions.id"]
  }
];

export const systemRoleCodes = [
  "workspace_admin",
  "content_editor",
  "analyst",
  "approver",
  "viewer",
  "billing_admin"
] as const;

export const billingPlanCodes = ["free", "basic", "pro", "enterprise"] as const;

export const usageMetricCodes = [
  "connected_accounts",
  "scheduled_posts",
  "team_members",
  "report_exports",
  "inbox_replies"
] as const;

// ── Platform Events ─────────────────────────────────────────────────────────

export type WorkspaceScopedEvent<TType extends string, TPayload> = {
  id: string;
  type: TType;
  workspaceId: string;
  occurredAt: string;
  payload: TPayload;
};

export type SocialAccountConnectedEvent = WorkspaceScopedEvent<
  "SocialAccountConnected",
  {
    socialAccountId: string;
    provider: "meta" | "linkedin" | "x";
    externalAccountId: string;
    connectedByUserId: string;
  }
>;

export type PostScheduledEvent = WorkspaceScopedEvent<
  "PostScheduled",
  {
    postId: string;
    scheduledFor: string;
    channels: Array<"meta" | "linkedin" | "x">;
    approvalState: "draft" | "pending_approval" | "approved";
  }
>;

export type PostApprovedEvent = WorkspaceScopedEvent<
  "PostApproved",
  {
    postId: string;
    approverUserId: string;
    approvedAt: string;
  }
>;

export type PostPublishRequestedEvent = WorkspaceScopedEvent<
  "PostPublishRequested",
  {
    postId: string;
    publishWindowStart: string;
  }
>;

export type PostPublishedEvent = WorkspaceScopedEvent<
  "PostPublished",
  {
    postId: string;
    providerPostIds: Record<string, string>;
    publishedAt: string;
  }
>;

export type PostPublishFailedEvent = WorkspaceScopedEvent<
  "PostPublishFailed",
  {
    postId: string;
    provider: string;
    reason: string;
    failedAt: string;
  }
>;

export type InboxEventIngestedEvent = WorkspaceScopedEvent<
  "InboxEventIngested",
  {
    inboxEventId: string;
    provider: string;
    messageType: "comment" | "mention" | "dm";
    receivedAt: string;
  }
>;

export type AnalyticsMetricsImportedEvent = WorkspaceScopedEvent<
  "AnalyticsMetricsImported",
  {
    importId: string;
    provider: string;
    intervalStart: string;
    intervalEnd: string;
  }
>;

export type SubscriptionChangedEvent = WorkspaceScopedEvent<
  "SubscriptionChanged",
  {
    subscriptionId: string;
    planCode: "free" | "basic" | "pro" | "enterprise";
    status: "trialing" | "active" | "past_due" | "canceled";
  }
>;

export type NotificationRequestedEvent = WorkspaceScopedEvent<
  "NotificationRequested",
  {
    notificationId: string;
    channel: "email" | "in_app";
    recipientUserId: string;
    templateCode: string;
  }
>;

export type NexoraPlatformEvent =
  | SocialAccountConnectedEvent
  | PostScheduledEvent
  | PostApprovedEvent
  | PostPublishRequestedEvent
  | PostPublishedEvent
  | PostPublishFailedEvent
  | InboxEventIngestedEvent
  | AnalyticsMetricsImportedEvent
  | SubscriptionChangedEvent
  | NotificationRequestedEvent;

// ── Analytics API Contracts ─────────────────────────────────────────────────

export type AnalyticsOverview = {
  impressions: number;
  reach: number;
  engagements: number;
  comments: number;
  clicks: number;
  followerDelta: number;
  engagementRate: number;
  impressionsChange: number;
  engagementsChange: number;
  reachChange: number;
  followerDeltaChange: number;
};

export type AccountMetric = {
  date: string;
  provider: string;
  impressions: number;
  reach: number;
  engagements: number;
  comments: number;
  clicks: number;
  followerDelta: number;
};

export type ContentPerformance = {
  draftId: string;
  provider: string;
  providerPostId: string;
  impressions: number;
  likes: number;
  comments: number;
  shares: number;
  clicks: number;
  saves: number;
  videoViews: number;
  engagementRate: number;
};

export type PlatformBreakdown = {
  provider: string;
  impressions: number;
  engagements: number;
  percentage: number;
};

// ── Billing API Contracts ───────────────────────────────────────────────────

export type BillingPlan = {
  id: string;
  code: string;
  name: string;
  interval: string;
  priceMinor: number;
  currency: string;
  seatLimit: number | null;
  socialAccountLimit: number | null;
  monthlyPostLimit: number | null;
  features: Record<string, any>;
};

export type WorkspaceSubscription = {
  id: string;
  workspaceId: string;
  planName: string;
  planCode: string;
  status: string;
  seatCount: number;
  trialEndsAt: string | null;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
};

export type Invoice = {
  id: string;
  invoiceNumber: string;
  status: string;
  amountDueMinor: number;
  amountPaidMinor: number;
  currency: string;
  hostedInvoiceUrl: string | null;
  issuedAt: string;
  paidAt: string | null;
};

export type WorkspaceEntitlement = {
  planCode: string;
  limits: Record<string, any>;
  features: Record<string, any>;
};

// ── Notification API Contracts ──────────────────────────────────────────────

export type InAppNotification = {
  id: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  actionUrl: string | null;
};

export type NotificationPreference = {
  eventCode: string;
  channel: string;
  enabled: boolean;
};

export type NotificationPreferenceUpdateRequest = {
  eventCode: string;
  channel: string;
  enabled: boolean;
};
