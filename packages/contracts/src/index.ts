// ── Social API Contracts ────────────────────────────────────────────────────

export type SocialProvider = "meta" | "linkedin" | "x" | "instagram" | "tiktok" | "pinterest" | "youtube" | "threads" | "bluesky";

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
  paidImpressions: number;
  organicImpressions: number;
  reach: number;
  paidReach: number;
  organicReach: number;
  engagements: number;
  comments: number;
  clicks: number;
  followerDelta: number;
  engagementRate: number;
  impressionsChange: number;
  engagementsChange: number;
  reachChange: number;
  clicksChange: number;
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

export type AnalyticsUTMConversion = {
  campaign: string;
  source: string;
  medium: string;
  clicks: number;
  conversions: number;
  revenue: number;
};

export type AnalyticsCompetitor = {
  competitorId: string;
  workspaceId: string;
  name: string;
  handle: string;
  provider: string;
  avatarUrl: string | null;
  trackedSince: string;
};

export type AnalyticsCompetitorBenchmark = {
  competitorId: string;
  date: string;
  followers: number;
  followerGrowth: number;
  engagements: number;
  postsPublished: number;
  shareOfVoicePercentage: number;
};

export type AnalyticsReportModule = {
  moduleId: string;
  type: "overview" | "top_posts" | "platform_breakdown" | "competitors" | "utm_conversions";
  title: string;
  position: number;
  config: Record<string, unknown>;
};

export type AnalyticsReportTemplate = {
  templateId: string;
  workspaceId: string;
  name: string;
  description: string | null;
  agencyLogoUrl: string | null;
  modules: AnalyticsReportModule[];
  createdAt: string;
  updatedAt: string;
};

export type AnalyticsReportSchedule = {
  scheduleId: string;
  templateId: string;
  frequency: "weekly" | "monthly";
  recipients: string[];
  isActive: boolean;
  lastSentAt: string | null;
};

export type AnalyticsBIIntegration = {
  integrationId: string;
  workspaceId: string;
  provider: "looker_studio" | "tableau";
  status: "active" | "inactive" | "pending";
  connectionKey: string | null;
  dataPipedCount: number;
  lastSyncAt: string | null;
  createdAt: string;
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

// ── Link-in-Bio Contracts ───────────────────────────────────────────────────

export type LinkInBioPage = {
  pageId: string;
  workspaceId: string;
  slug: string;
  title: string;
  bioText: string | null;
  avatarUrl: string | null;
  themeConfig: Record<string, unknown>;
  isActive: boolean;
  entries: LinkInBioEntry[];
  createdAt: string;
  updatedAt: string;
};

export type LinkInBioEntry = {
  entryId: string;
  pageId: string;
  draftId: string | null;
  externalUrl: string | null;
  thumbnailUrl: string | null;
  label: string;
  sortOrder: number;
  isPinned: boolean;
  createdAt: string;
};

export type CreateBioPageRequest = {
  slug: string;
  title: string;
  bioText?: string;
  avatarUrl?: string;
  themeConfig?: Record<string, unknown>;
};

export type UpdateBioPageRequest = {
  title: string;
  bioText?: string;
  avatarUrl?: string;
  themeConfig?: Record<string, unknown>;
};

export type AddBioEntryRequest = {
  draftId?: string;
  externalUrl?: string;
  thumbnailUrl?: string;
  label: string;
  sortOrder: number;
  isPinned: boolean;
};

// ── Bulk Import Contracts ───────────────────────────────────────────────────

export type BulkImportResponse = {
  batchId: string;
  totalRows: number;
  validCount: number;
  invalidCount: number;
  errors: string[];
};

// ── Optimal Send Time Contracts ─────────────────────────────────────────────

export type OptimalSendTimeSlot = {
  provider: string;
  dayLabel: string;
  dayOfWeek: number;
  hourUtc: number;
  score: number;
  sampleSize: number;
};

// ── Calendar Drag Contracts ─────────────────────────────────────────────────

export type CalendarDragUpdateRequest = {
  scheduledFor: string;
  timezone: string;
};

// ── Evergreen Content Contracts ─────────────────────────────────────────────

export type EvergreenCategoryColor =
  | "sky"
  | "emerald"
  | "amber"
  | "rose"
  | "violet"
  | "orange"
  | "teal"
  | "pink"
  | "indigo"
  | "slate";

export type EvergreenCategory = {
  categoryId: string;
  workspaceId: string;
  name: string;
  color: EvergreenCategoryColor;
  icon: string;
  description: string | null;
  isEvergreen: boolean;
  postCount: number;
  activePostCount: number;
  lastPublishedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type EvergreenPostStatus = "active" | "paused" | "expired";

export type EvergreenPostVariation = {
  variationId: string;
  postId: string;
  caption: string;
  mediaUrls: string[];
  usageCount: number;
  lastUsedAt: string | null;
  createdAt: string;
};

export type EvergreenPost = {
  postId: string;
  categoryId: string;
  workspaceId: string;
  body: string;
  status: EvergreenPostStatus;
  isEvergreen: boolean;
  publishCount: number;
  lastPublishedAt: string | null;
  queuePosition: number;
  variations: EvergreenPostVariation[];
  targetProviders: SocialProvider[];
  createdAt: string;
  updatedAt: string;
};

export type EvergreenScheduleSlot = {
  slotId: string;
  workspaceId: string;
  categoryId: string;
  categoryName: string;
  categoryColor: EvergreenCategoryColor;
  dayOfWeek: number;
  timeUtc: string;
  timezone: string;
  isActive: boolean;
  nextPostTitle: string | null;
  createdAt: string;
};

export type CreateEvergreenCategoryRequest = {
  name: string;
  color: EvergreenCategoryColor;
  icon: string;
  description?: string;
  isEvergreen?: boolean;
};

export type UpdateEvergreenCategoryRequest = {
  name: string;
  color: EvergreenCategoryColor;
  icon: string;
  description?: string;
  isEvergreen?: boolean;
};

export type CreateEvergreenPostRequest = {
  body: string;
  isEvergreen?: boolean;
  targetProviders?: SocialProvider[];
  variations: Array<{
    caption: string;
    mediaUrls?: string[];
  }>;
};

export type AddEvergreenVariationRequest = {
  caption: string;
  mediaUrls?: string[];
};

export type CreateEvergreenScheduleSlotRequest = {
  categoryId: string;
  dayOfWeek: number;
  timeUtc: string;
  timezone: string;
};

export type ReorderEvergreenQueueRequest = {
  postIds: string[];
};

// ── Smart Inbox & CRM Contracts ─────────────────────────────────────────────

export type InboxMessageType = "dm" | "comment" | "mention" | "review";
export type InboxSentiment = "positive" | "neutral" | "negative";
export type InboxMessageStatus = "unread" | "read" | "replied" | "archived";
export type InboxReviewSource = "google_my_business" | "apple_app_store" | "yelp";

export type InboxSender = {
  senderId: string;
  displayName: string;
  username: string | null;
  avatarUrl: string | null;
  provider: SocialProvider | InboxReviewSource;
  followerCount: number | null;
  isVerified: boolean;
};

export type InboxTag = {
  tagId: string;
  workspaceId: string;
  label: string;
  color: "sky" | "emerald" | "amber" | "rose" | "violet" | "orange" | "teal" | "pink" | "indigo" | "slate" | "red" | "cyan";
  createdAt: string;
};

export type InboxInternalNote = {
  noteId: string;
  messageId: string;
  authorUserId: string;
  authorName: string;
  body: string;
  createdAt: string;
};

export type InboxCollisionLock = {
  messageId: string;
  lockedByUserId: string;
  lockedByName: string;
  lockedAt: string;
  expiresAt: string;
};

export type InboxMessage = {
  messageId: string;
  workspaceId: string;
  conversationId: string;
  type: InboxMessageType;
  provider: SocialProvider | InboxReviewSource;
  sender: InboxSender;
  subject: string | null;
  body: string;
  bodyHtml: string | null;
  mediaUrls: string[];
  externalPostUrl: string | null;
  parentExternalPostId: string | null;
  status: InboxMessageStatus;
  sentiment: InboxSentiment;
  tags: InboxTag[];
  assignedToUserId: string | null;
  assignedToName: string | null;
  collisionLock: InboxCollisionLock | null;
  internalNotes: InboxInternalNote[];
  reviewRating: number | null;
  reviewSource: InboxReviewSource | null;
  aiSuggestedReply: string | null;
  createdAt: string;
  updatedAt: string;
};

export type InboxConversation = {
  conversationId: string;
  workspaceId: string;
  sender: InboxSender;
  latestMessage: InboxMessage;
  messageCount: number;
  unreadCount: number;
  tags: InboxTag[];
  assignedToUserId: string | null;
  assignedToName: string | null;
  sentiment: InboxSentiment;
  firstMessageAt: string;
  lastMessageAt: string;
};

export type InboxReview = {
  reviewId: string;
  workspaceId: string;
  source: InboxReviewSource;
  reviewerName: string;
  reviewerAvatarUrl: string | null;
  rating: number;
  title: string | null;
  body: string;
  response: string | null;
  respondedAt: string | null;
  sentiment: InboxSentiment;
  createdAt: string;
};

export type InboxModerationRuleAction = "hide" | "flag" | "auto_reply" | "delete";
export type InboxModerationRuleTrigger = "keyword" | "sentiment" | "spam_score" | "profanity";

export type InboxModerationRule = {
  ruleId: string;
  workspaceId: string;
  name: string;
  description: string | null;
  trigger: InboxModerationRuleTrigger;
  triggerValue: string;
  action: InboxModerationRuleAction;
  actionValue: string | null;
  isActive: boolean;
  matchCount: number;
  createdAt: string;
  updatedAt: string;
};

export type InboxBotNodeType = "trigger" | "condition" | "action" | "delay" | "reply";

export type InboxBotNode = {
  nodeId: string;
  type: InboxBotNodeType;
  label: string;
  config: Record<string, unknown>;
  position: { x: number; y: number };
  nextNodeIds: string[];
};

export type InboxBotFlow = {
  flowId: string;
  workspaceId: string;
  name: string;
  description: string | null;
  isActive: boolean;
  nodes: InboxBotNode[];
  triggerCount: number;
  lastTriggeredAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type InboxCrmContact = {
  contactId: string;
  senderId: string;
  displayName: string;
  email: string | null;
  provider: SocialProvider | InboxReviewSource;
  avatarUrl: string | null;
  totalInteractions: number;
  sentimentScore: number;
  lifetimeValue: number | null;
  firstSeenAt: string;
  lastSeenAt: string;
  notes: string | null;
  customFields: Record<string, string>;
};

// ── Inbox Request Types ─────────────────────────────────────────────────────

export type ListInboxMessagesRequest = {
  type?: InboxMessageType;
  provider?: string;
  status?: InboxMessageStatus;
  sentiment?: InboxSentiment;
  tagId?: string;
  assignedToUserId?: string;
  search?: string;
  cursor?: string;
  limit?: number;
};

export type ReplyToInboxMessageRequest = {
  body: string;
  mediaUrls?: string[];
  isInternalNote?: boolean;
};

export type AcquireCollisionLockRequest = {
  messageId: string;
};

export type CreateInboxTagRequest = {
  label: string;
  color: InboxTag["color"];
};

export type AddInternalNoteRequest = {
  body: string;
};

export type AssignInboxMessageRequest = {
  assignedToUserId: string;
};

export type CreateModerationRuleRequest = {
  name: string;
  description?: string;
  trigger: InboxModerationRuleTrigger;
  triggerValue: string;
  action: InboxModerationRuleAction;
  actionValue?: string;
};

export type CreateBotFlowRequest = {
  name: string;
  description?: string;
  nodes: InboxBotNode[];
};

export type UpdateBotFlowRequest = {
  name?: string;
  description?: string;
  isActive?: boolean;
  nodes?: InboxBotNode[];
};

