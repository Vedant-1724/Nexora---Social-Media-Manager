import axios from "axios";
import type {
  AccountMetric,
  AnalyticsOverview,
  BillingPlan,
  ConnectedSocialAccount,
  ContentPerformance,
  DraftDetails,
  DraftSummary,
  InAppNotification,
  Invoice,
  NotificationPreference,
  NotificationPreferenceUpdateRequest,
  PlatformBreakdown,
  SaveDraftRequest,
  ScheduleDraftRequest,
  SchedulerApprovalRequest,
  SchedulerCalendarEntry,
  ScheduledJob,
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
