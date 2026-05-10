import axios from "axios";
import type {
  AccountMetric,
  AddBioEntryRequest,
  AnalyticsOverview,
  BillingPlan,
  BulkImportResponse,
  ConnectedSocialAccount,
  ContentPerformance,
  CreateBioPageRequest,
  DraftDetails,
  DraftSummary,
  InAppNotification,
  Invoice,
  LinkInBioEntry,
  LinkInBioPage,
  NotificationPreference,
  NotificationPreferenceUpdateRequest,
  OptimalSendTimeSlot,
  PlatformBreakdown,
  SaveDraftRequest,
  ScheduleDraftRequest,
  SchedulerApprovalRequest,
  SchedulerCalendarEntry,
  ScheduledJob,
  UpdateBioPageRequest,
  WorkspaceEntitlement,
  WorkspaceSubscription
} from "@nexora/contracts";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:18080",
  headers: {
    "Content-Type": "application/json"
  }
});

export function authorizationHeader(accessToken: string) {
  return {
    Authorization: `Bearer ${accessToken}`
  };
}

// ── Scheduler API ───────────────────────────────────────────────────────────
// Merged from features/scheduler/scheduler-api.ts

export async function listDrafts(workspaceId: string, accessToken: string) {
  return apiClient
    .get<DraftSummary[]>(`/api/v1/workspaces/${workspaceId}/posts/drafts`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function getDraft(workspaceId: string, draftId: string, accessToken: string) {
  return apiClient
    .get<DraftDetails>(`/api/v1/workspaces/${workspaceId}/posts/drafts/${draftId}`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function saveDraft(
  workspaceId: string,
  accessToken: string,
  request: SaveDraftRequest,
  draftId?: string
) {
  const url = draftId
    ? `/api/v1/workspaces/${workspaceId}/posts/drafts/${draftId}`
    : `/api/v1/workspaces/${workspaceId}/posts/drafts`;
  const method = draftId ? apiClient.put<DraftDetails> : apiClient.post<DraftDetails>;
  return method(url, request, {
    headers: authorizationHeader(accessToken)
  }).then(({ data }) => data);
}

export async function submitDraftForApproval(
  workspaceId: string,
  draftId: string,
  accessToken: string,
  approvalRouteId?: string
) {
  return apiClient
    .post<SchedulerApprovalRequest>(
      `/api/v1/workspaces/${workspaceId}/posts/drafts/${draftId}/approval-requests`,
      { approvalRouteId },
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function scheduleDraft(
  workspaceId: string,
  draftId: string,
  accessToken: string,
  request: ScheduleDraftRequest
) {
  return apiClient
    .post<ScheduledJob>(
      `/api/v1/workspaces/${workspaceId}/posts/drafts/${draftId}/schedule`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function listCalendarEntries(
  workspaceId: string,
  accessToken: string,
  from: string,
  to: string
) {
  return apiClient
    .get<SchedulerCalendarEntry[]>(`/api/v1/workspaces/${workspaceId}/calendar/posts`, {
      headers: authorizationHeader(accessToken),
      params: { from, to }
    })
    .then(({ data }) => data);
}

export async function listConnectedAccounts(workspaceId: string, accessToken: string) {
  return apiClient
    .get<ConnectedSocialAccount[]>(`/api/v1/workspaces/${workspaceId}/social/accounts`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

// ── Analytics API ───────────────────────────────────────────────────────────
// Phase 8: Analytics Dashboard endpoints

export async function getAnalyticsOverview(
  workspaceId: string,
  accessToken: string,
  from: string,
  to: string
) {
  return apiClient
    .get<AnalyticsOverview>(`/api/v1/analytics/${workspaceId}/overview`, {
      headers: authorizationHeader(accessToken),
      params: { from, to }
    })
    .then(({ data }) => data);
}

export async function getAnalyticsTimeSeries(
  workspaceId: string,
  accessToken: string,
  from: string,
  to: string
) {
  return apiClient
    .get<AccountMetric[]>(`/api/v1/analytics/${workspaceId}/time-series`, {
      headers: authorizationHeader(accessToken),
      params: { from, to }
    })
    .then(({ data }) => data);
}

export async function getTopContent(
  workspaceId: string,
  accessToken: string,
  from: string,
  to: string,
  limit = 10
) {
  return apiClient
    .get<ContentPerformance[]>(`/api/v1/analytics/${workspaceId}/top-content`, {
      headers: authorizationHeader(accessToken),
      params: { from, to, limit }
    })
    .then(({ data }) => data);
}

export async function getPlatformBreakdown(
  workspaceId: string,
  accessToken: string,
  from: string,
  to: string
) {
  return apiClient
    .get<PlatformBreakdown[]>(`/api/v1/analytics/${workspaceId}/platform-breakdown`, {
      headers: authorizationHeader(accessToken),
      params: { from, to }
    })
    .then(({ data }) => data);
}

// ── Advanced Analytics API ──────────────────────────────────────────────────

import type {
  AnalyticsUTMConversion,
  AnalyticsCompetitorBenchmark,
  AnalyticsReportTemplate,
  AnalyticsReportSchedule,
  AnalyticsBIIntegration
} from "@nexora/contracts";

export async function getUTMConversions(
  workspaceId: string,
  accessToken: string,
  from: string,
  to: string
) {
  return apiClient
    .get<AnalyticsUTMConversion[]>(`/api/v1/analytics/${workspaceId}/utm-conversions`, {
      headers: authorizationHeader(accessToken),
      params: { from, to }
    })
    .then(({ data }) => data);
}

export async function getCompetitorBenchmarks(
  workspaceId: string,
  accessToken: string,
  from: string,
  to: string
) {
  return apiClient
    .get<AnalyticsCompetitorBenchmark[]>(`/api/v1/analytics/${workspaceId}/competitors/benchmarks`, {
      headers: authorizationHeader(accessToken),
      params: { from, to }
    })
    .then(({ data }) => data);
}

export async function listReportTemplates(
  workspaceId: string,
  accessToken: string
) {
  return apiClient
    .get<AnalyticsReportTemplate[]>(`/api/v1/analytics/${workspaceId}/reports/templates`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function createReportTemplate(
  workspaceId: string,
  accessToken: string,
  request: Partial<AnalyticsReportTemplate>
) {
  return apiClient
    .post<AnalyticsReportTemplate>(`/api/v1/analytics/${workspaceId}/reports/templates`, request, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function scheduleReport(
  workspaceId: string,
  templateId: string,
  accessToken: string,
  request: Partial<AnalyticsReportSchedule>
) {
  return apiClient
    .post<AnalyticsReportSchedule>(`/api/v1/analytics/${workspaceId}/reports/templates/${templateId}/schedule`, request, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function getBIIntegrations(
  workspaceId: string,
  accessToken: string
) {
  return apiClient
    .get<AnalyticsBIIntegration[]>(`/api/v1/analytics/${workspaceId}/bi-integrations`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function configureBIIntegration(
  workspaceId: string,
  provider: "looker_studio" | "tableau",
  accessToken: string
) {
  return apiClient
    .post<AnalyticsBIIntegration>(`/api/v1/analytics/${workspaceId}/bi-integrations/${provider}/configure`, {}, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

// ── Billing APIs ────────────────────────────────────────────────────────────

export async function getBillingPlans() {
  return apiClient
    .get<BillingPlan[]>("/api/v1/billing/plans")
    .then(({ data }) => data);
}

export async function getWorkspaceSubscription(workspaceId: string, accessToken: string) {
  return apiClient
    .get<WorkspaceSubscription>(`/api/v1/billing/workspaces/${workspaceId}/subscription`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function getWorkspaceInvoices(workspaceId: string, accessToken: string) {
  return apiClient
    .get<Invoice[]>(`/api/v1/billing/workspaces/${workspaceId}/invoices`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function getWorkspaceEntitlements(workspaceId: string, accessToken: string) {
  return apiClient
    .get<WorkspaceEntitlement>(`/api/v1/billing/workspaces/${workspaceId}/entitlements`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

// ── Notification APIs ───────────────────────────────────────────────────────

export async function getInAppNotifications(workspaceId: string, accessToken: string) {
  return apiClient
    .get<InAppNotification[]>(`/api/v1/notifications/workspaces/${workspaceId}/in-app`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function markNotificationRead(workspaceId: string, notificationId: string, accessToken: string) {
  return apiClient.put(
    `/api/v1/notifications/workspaces/${workspaceId}/in-app/${notificationId}/read`,
    {},
    { headers: authorizationHeader(accessToken) }
  );
}

export async function getNotificationPreferences(workspaceId: string, accessToken: string) {
  return apiClient
    .get<NotificationPreference[]>(`/api/v1/notifications/workspaces/${workspaceId}/preferences`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function updateNotificationPreference(
  workspaceId: string,
  payload: NotificationPreferenceUpdateRequest,
  accessToken: string
) {
  return apiClient.put(
    `/api/v1/notifications/workspaces/${workspaceId}/preferences`,
    payload,
    { headers: authorizationHeader(accessToken) }
  );
}

export type MediaUploadResponse = {
  bucketName: string;
  objectKey: string;
  mimeType: string;
  mediaKind: string;
  sizeBytes: number;
  sha256Checksum: string;
  sourceUrl: string;
};

export async function uploadMedia(workspaceId: string, accessToken: string, file: File) {
  const formData = new FormData();
  formData.append("file", file);

  return apiClient
    .post<MediaUploadResponse>(
      `/api/v1/workspaces/${workspaceId}/posts/media/upload`,
      formData,
      {
        headers: {
          ...authorizationHeader(accessToken),
          "Content-Type": "multipart/form-data"
        }
      }
    )
    .then(({ data }) => data);
}

// ── Bulk Import API ─────────────────────────────────────────────────────────

export async function bulkImportCSV(
  workspaceId: string,
  accessToken: string,
  file: File
) {
  const formData = new FormData();
  formData.append("file", file);
  return apiClient
    .post<BulkImportResponse>(
      `/api/v1/workspaces/${workspaceId}/posts/bulk-import`,
      formData,
      {
        headers: {
          ...authorizationHeader(accessToken),
          "Content-Type": "multipart/form-data"
        }
      }
    )
    .then(({ data }) => data);
}

// ── Link-in-Bio API ─────────────────────────────────────────────────────────

export async function createBioPage(
  workspaceId: string,
  accessToken: string,
  request: CreateBioPageRequest
) {
  return apiClient
    .post<LinkInBioPage>(`/api/v1/workspaces/${workspaceId}/bio-pages`, request, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function listBioPages(workspaceId: string, accessToken: string) {
  return apiClient
    .get<LinkInBioPage[]>(`/api/v1/workspaces/${workspaceId}/bio-pages`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function getBioPage(workspaceId: string, pageId: string, accessToken: string) {
  return apiClient
    .get<LinkInBioPage>(`/api/v1/workspaces/${workspaceId}/bio-pages/${pageId}`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function updateBioPage(
  workspaceId: string,
  pageId: string,
  accessToken: string,
  request: UpdateBioPageRequest
) {
  return apiClient
    .put<LinkInBioPage>(`/api/v1/workspaces/${workspaceId}/bio-pages/${pageId}`, request, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function addBioEntry(
  workspaceId: string,
  pageId: string,
  accessToken: string,
  request: AddBioEntryRequest
) {
  return apiClient
    .post<LinkInBioEntry>(
      `/api/v1/workspaces/${workspaceId}/bio-pages/${pageId}/entries`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function removeBioEntry(
  workspaceId: string,
  pageId: string,
  entryId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/bio-pages/${pageId}/entries/${entryId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

// ── Optimal Send Time API ───────────────────────────────────────────────────

export async function getOptimalSendTimes(
  workspaceId: string,
  accessToken: string,
  provider?: string
) {
  return apiClient
    .get<OptimalSendTimeSlot[]>(`/api/v1/analytics/${workspaceId}/optimal-send-times`, {
      headers: authorizationHeader(accessToken),
      params: provider ? { provider } : {}
    })
    .then(({ data }) => data);
}

// ── Reschedule API (Calendar Drag-and-Drop) ─────────────────────────────────

export async function reschedulePost(
  workspaceId: string,
  draftId: string,
  accessToken: string,
  scheduledFor: string,
  timezone: string
) {
  return apiClient
    .post<ScheduledJob>(
      `/api/v1/workspaces/${workspaceId}/posts/drafts/${draftId}/schedule`,
      { scheduledFor, timezone },
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

// ── Evergreen Content API ───────────────────────────────────────────────────

import type {
  EvergreenCategory,
  EvergreenPost,
  EvergreenPostVariation,
  EvergreenScheduleSlot,
  CreateEvergreenCategoryRequest,
  UpdateEvergreenCategoryRequest,
  CreateEvergreenPostRequest,
  AddEvergreenVariationRequest,
  CreateEvergreenScheduleSlotRequest,
  ReorderEvergreenQueueRequest
} from "@nexora/contracts";

export async function listEvergreenCategories(workspaceId: string, accessToken: string) {
  return apiClient
    .get<EvergreenCategory[]>(`/api/v1/workspaces/${workspaceId}/evergreen/categories`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function createEvergreenCategory(
  workspaceId: string,
  accessToken: string,
  request: CreateEvergreenCategoryRequest
) {
  return apiClient
    .post<EvergreenCategory>(`/api/v1/workspaces/${workspaceId}/evergreen/categories`, request, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function updateEvergreenCategory(
  workspaceId: string,
  categoryId: string,
  accessToken: string,
  request: UpdateEvergreenCategoryRequest
) {
  return apiClient
    .put<EvergreenCategory>(
      `/api/v1/workspaces/${workspaceId}/evergreen/categories/${categoryId}`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function deleteEvergreenCategory(
  workspaceId: string,
  categoryId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/evergreen/categories/${categoryId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function listEvergreenPosts(
  workspaceId: string,
  categoryId: string,
  accessToken: string
) {
  return apiClient
    .get<EvergreenPost[]>(
      `/api/v1/workspaces/${workspaceId}/evergreen/categories/${categoryId}/posts`,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function createEvergreenPost(
  workspaceId: string,
  categoryId: string,
  accessToken: string,
  request: CreateEvergreenPostRequest
) {
  return apiClient
    .post<EvergreenPost>(
      `/api/v1/workspaces/${workspaceId}/evergreen/categories/${categoryId}/posts`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function updateEvergreenPost(
  workspaceId: string,
  postId: string,
  accessToken: string,
  request: Partial<CreateEvergreenPostRequest> & { status?: string }
) {
  return apiClient
    .put<EvergreenPost>(
      `/api/v1/workspaces/${workspaceId}/evergreen/posts/${postId}`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function deleteEvergreenPost(
  workspaceId: string,
  postId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/evergreen/posts/${postId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function addPostVariation(
  workspaceId: string,
  postId: string,
  accessToken: string,
  request: AddEvergreenVariationRequest
) {
  return apiClient
    .post<EvergreenPostVariation>(
      `/api/v1/workspaces/${workspaceId}/evergreen/posts/${postId}/variations`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function removePostVariation(
  workspaceId: string,
  postId: string,
  variationId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/evergreen/posts/${postId}/variations/${variationId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function listScheduleSlots(workspaceId: string, accessToken: string) {
  return apiClient
    .get<EvergreenScheduleSlot[]>(`/api/v1/workspaces/${workspaceId}/evergreen/schedule-slots`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function createScheduleSlot(
  workspaceId: string,
  accessToken: string,
  request: CreateEvergreenScheduleSlotRequest
) {
  return apiClient
    .post<EvergreenScheduleSlot>(
      `/api/v1/workspaces/${workspaceId}/evergreen/schedule-slots`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function deleteScheduleSlot(
  workspaceId: string,
  slotId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/evergreen/schedule-slots/${slotId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function reorderEvergreenQueue(
  workspaceId: string,
  categoryId: string,
  accessToken: string,
  request: ReorderEvergreenQueueRequest
) {
  return apiClient
    .put(
      `/api/v1/workspaces/${workspaceId}/evergreen/categories/${categoryId}/reorder`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

// ── Smart Inbox & CRM API ───────────────────────────────────────────────────

import type {
  InboxMessage,
  InboxConversation,
  InboxReview,
  InboxTag,
  InboxInternalNote,
  InboxCollisionLock,
  InboxModerationRule,
  InboxBotFlow,
  InboxCrmContact,
  ListInboxMessagesRequest,
  ReplyToInboxMessageRequest,
  CreateInboxTagRequest,
  AddInternalNoteRequest,
  AssignInboxMessageRequest,
  CreateModerationRuleRequest,
  CreateBotFlowRequest,
  UpdateBotFlowRequest
} from "@nexora/contracts";

export async function listInboxMessages(
  workspaceId: string,
  accessToken: string,
  params?: ListInboxMessagesRequest
) {
  return apiClient
    .get<InboxMessage[]>(`/api/v1/workspaces/${workspaceId}/inbox/messages`, {
      headers: authorizationHeader(accessToken),
      params
    })
    .then(({ data }) => data);
}

export async function getInboxConversation(
  workspaceId: string,
  conversationId: string,
  accessToken: string
) {
  return apiClient
    .get<InboxConversation>(
      `/api/v1/workspaces/${workspaceId}/inbox/conversations/${conversationId}`,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function replyToInboxMessage(
  workspaceId: string,
  messageId: string,
  accessToken: string,
  request: ReplyToInboxMessageRequest
) {
  return apiClient
    .post<InboxMessage>(
      `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/reply`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function acquireCollisionLock(
  workspaceId: string,
  messageId: string,
  accessToken: string
) {
  return apiClient
    .post<InboxCollisionLock>(
      `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/lock`,
      {},
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function releaseCollisionLock(
  workspaceId: string,
  messageId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/lock`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function listInboxTags(workspaceId: string, accessToken: string) {
  return apiClient
    .get<InboxTag[]>(`/api/v1/workspaces/${workspaceId}/inbox/tags`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function createInboxTag(
  workspaceId: string,
  accessToken: string,
  request: CreateInboxTagRequest
) {
  return apiClient
    .post<InboxTag>(`/api/v1/workspaces/${workspaceId}/inbox/tags`, request, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function addTagToMessage(
  workspaceId: string,
  messageId: string,
  tagId: string,
  accessToken: string
) {
  return apiClient.post(
    `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/tags/${tagId}`,
    {},
    { headers: authorizationHeader(accessToken) }
  );
}

export async function removeTagFromMessage(
  workspaceId: string,
  messageId: string,
  tagId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/tags/${tagId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function addInternalNote(
  workspaceId: string,
  messageId: string,
  accessToken: string,
  request: AddInternalNoteRequest
) {
  return apiClient
    .post<InboxInternalNote>(
      `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/notes`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function assignInboxMessage(
  workspaceId: string,
  messageId: string,
  accessToken: string,
  request: AssignInboxMessageRequest
) {
  return apiClient.put(
    `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/assign`,
    request,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function listInboxReviews(workspaceId: string, accessToken: string) {
  return apiClient
    .get<InboxReview[]>(`/api/v1/workspaces/${workspaceId}/inbox/reviews`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function respondToReview(
  workspaceId: string,
  reviewId: string,
  accessToken: string,
  body: string
) {
  return apiClient
    .post<InboxReview>(
      `/api/v1/workspaces/${workspaceId}/inbox/reviews/${reviewId}/respond`,
      { body },
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function generateAIReply(
  workspaceId: string,
  messageId: string,
  accessToken: string
) {
  return apiClient
    .post<{ suggestedReply: string }>(
      `/api/v1/workspaces/${workspaceId}/inbox/messages/${messageId}/ai-reply`,
      {},
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function listModerationRules(workspaceId: string, accessToken: string) {
  return apiClient
    .get<InboxModerationRule[]>(
      `/api/v1/workspaces/${workspaceId}/inbox/moderation-rules`,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function createModerationRule(
  workspaceId: string,
  accessToken: string,
  request: CreateModerationRuleRequest
) {
  return apiClient
    .post<InboxModerationRule>(
      `/api/v1/workspaces/${workspaceId}/inbox/moderation-rules`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function deleteModerationRule(
  workspaceId: string,
  ruleId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/inbox/moderation-rules/${ruleId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function toggleModerationRule(
  workspaceId: string,
  ruleId: string,
  isActive: boolean,
  accessToken: string
) {
  return apiClient.put(
    `/api/v1/workspaces/${workspaceId}/inbox/moderation-rules/${ruleId}/toggle`,
    { isActive },
    { headers: authorizationHeader(accessToken) }
  );
}

export async function listBotFlows(workspaceId: string, accessToken: string) {
  return apiClient
    .get<InboxBotFlow[]>(`/api/v1/workspaces/${workspaceId}/inbox/bot-flows`, {
      headers: authorizationHeader(accessToken)
    })
    .then(({ data }) => data);
}

export async function createBotFlow(
  workspaceId: string,
  accessToken: string,
  request: CreateBotFlowRequest
) {
  return apiClient
    .post<InboxBotFlow>(
      `/api/v1/workspaces/${workspaceId}/inbox/bot-flows`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function updateBotFlow(
  workspaceId: string,
  flowId: string,
  accessToken: string,
  request: UpdateBotFlowRequest
) {
  return apiClient
    .put<InboxBotFlow>(
      `/api/v1/workspaces/${workspaceId}/inbox/bot-flows/${flowId}`,
      request,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

export async function deleteBotFlow(
  workspaceId: string,
  flowId: string,
  accessToken: string
) {
  return apiClient.delete(
    `/api/v1/workspaces/${workspaceId}/inbox/bot-flows/${flowId}`,
    { headers: authorizationHeader(accessToken) }
  );
}

export async function getInboxCrmContact(
  workspaceId: string,
  senderId: string,
  accessToken: string
) {
  return apiClient
    .get<InboxCrmContact>(
      `/api/v1/workspaces/${workspaceId}/inbox/crm/contacts/${senderId}`,
      { headers: authorizationHeader(accessToken) }
    )
    .then(({ data }) => data);
}

