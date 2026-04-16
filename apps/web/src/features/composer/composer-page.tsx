import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ConnectedSocialAccount, SaveDraftRequest, SocialProvider } from "@nexora/contracts";
import { Badge, Button, Card, CardTitle, cn } from "@nexora/ui";

import { useAuth } from "@/features/auth/auth-context";
import {
  getDraft,
  listConnectedAccounts,
  listDrafts,
  saveDraft,
  scheduleDraft,
  submitDraftForApproval
} from "@/lib/api";

const providers: SocialProvider[] = ["meta", "linkedin", "x"];
const inputClassName =
  "w-full rounded-3xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-sky-400";

type DraftFormState = {
  title: string;
  body: string;
  primaryTimezone: string;
  campaignLabel: string;
  approvalRouteId: string;
  metadataJson: string;
  mediaBucket: string;
  mediaObjectKey: string;
  mediaMimeType: string;
  mediaKind: string;
  mediaSizeBytes: string;
  mediaChecksum: string;
  mediaAltText: string;
  mediaSourceUrl: string;
  scheduledFor: string;
  variants: Record<
    SocialProvider,
    {
      caption: string;
      linkUrl: string;
      firstComment: string;
      targetAccountIds: string[];
    }
  >;
};

const emptyForm = (): DraftFormState => ({
  title: "Spring Launch Narrative",
  body: "Launching a premium social publishing workflow that keeps approvals, variants, and scheduling in sync.",
  primaryTimezone: "Asia/Calcutta",
  campaignLabel: "Spring Launch",
  approvalRouteId: "",
  metadataJson: JSON.stringify({ futureAiHints: ["tone", "best_publish_window"] }, null, 2),
  mediaBucket: "nexora-assets",
  mediaObjectKey: "campaigns/spring-launch/hero.png",
  mediaMimeType: "image/png",
  mediaKind: "image",
  mediaSizeBytes: "245760",
  mediaChecksum: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  mediaAltText: "Spring launch hero visual",
  mediaSourceUrl: "https://cdn.nexora.local/spring-launch-hero.png",
  scheduledFor: "",
  variants: {
    meta: { caption: "", linkUrl: "", firstComment: "", targetAccountIds: [] },
    linkedin: { caption: "", linkUrl: "", firstComment: "", targetAccountIds: [] },
    x: { caption: "", linkUrl: "", firstComment: "", targetAccountIds: [] }
  }
});

function formatError(error: unknown) {
  if (
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message === "string"
  ) {
    return (error as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Request failed.";
  }
  return "We could not complete that request.";
}

export function ComposerPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const accessToken = session?.session?.accessToken ?? "";
  const workspaceId = session?.currentWorkspace?.workspaceId ?? "";
  const [selectedDraftId, setSelectedDraftId] = useState<string | null>(null);
  const [form, setForm] = useState<DraftFormState>(() => emptyForm());
  const [feedback, setFeedback] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"content" | "media" | "platforms" | "advanced">("content");

  const draftsQuery = useQuery({
    queryKey: ["scheduler-drafts", workspaceId],
    queryFn: () => listDrafts(workspaceId, accessToken),
    enabled: workspaceId.length > 0 && accessToken.length > 0
  });

  const accountsQuery = useQuery({
    queryKey: ["social-accounts", workspaceId],
    queryFn: () => listConnectedAccounts(workspaceId, accessToken),
    enabled: workspaceId.length > 0 && accessToken.length > 0
  });

  const draftQuery = useQuery({
    queryKey: ["scheduler-draft", workspaceId, selectedDraftId],
    queryFn: () => getDraft(workspaceId, selectedDraftId as string, accessToken),
    enabled: workspaceId.length > 0 && accessToken.length > 0 && selectedDraftId != null
  });

  const accountsByProvider = useMemo(() => {
    const grouped: Record<SocialProvider, ConnectedSocialAccount[]> = {
      meta: [],
      linkedin: [],
      x: []
    };
    (accountsQuery.data ?? []).forEach((account) => {
      grouped[account.provider].push(account);
    });
    return grouped;
  }, [accountsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: (request: SaveDraftRequest) => saveDraft(workspaceId, accessToken, request, selectedDraftId ?? undefined),
    onSuccess: async (draft) => {
      setSelectedDraftId(draft.draftId);
      setFeedback("Draft saved with media, variants, and scheduler metadata.");
      await queryClient.invalidateQueries({ queryKey: ["scheduler-drafts", workspaceId] });
      await queryClient.invalidateQueries({ queryKey: ["scheduler-draft", workspaceId, draft.draftId] });
    },
    onError: (error) => setFeedback(formatError(error))
  });

  const approvalMutation = useMutation({
    mutationFn: (draftId: string) => submitDraftForApproval(workspaceId, draftId, accessToken, form.approvalRouteId || undefined),
    onSuccess: async () => {
      setFeedback("Draft submitted to the approval route.");
      await queryClient.invalidateQueries({ queryKey: ["scheduler-drafts", workspaceId] });
      await queryClient.invalidateQueries({ queryKey: ["scheduler-draft", workspaceId, selectedDraftId] });
    },
    onError: (error) => setFeedback(formatError(error))
  });

  const scheduleMutation = useMutation({
    mutationFn: (draftId: string) =>
      scheduleDraft(workspaceId, draftId, accessToken, {
        scheduledFor: new Date(form.scheduledFor).toISOString(),
        timezone: form.primaryTimezone
      }),
    onSuccess: async () => {
      setFeedback("Scheduling updated and queued for dispatch.");
      await queryClient.invalidateQueries({ queryKey: ["scheduler-drafts", workspaceId] });
      await queryClient.invalidateQueries({ queryKey: ["scheduler-draft", workspaceId, selectedDraftId] });
      await queryClient.invalidateQueries({ queryKey: ["scheduler-calendar", workspaceId] });
    },
    onError: (error) => setFeedback(formatError(error))
  });

  const selectedDraft = draftQuery.data;

  useEffect(() => {
    if (!selectedDraft) {
      return;
    }
    hydrateFromServer();
  }, [selectedDraft]);

  function hydrateFromServer() {
    if (!selectedDraft) {
      return;
    }

    const nextForm = emptyForm();
    nextForm.title = selectedDraft.title;
    nextForm.body = selectedDraft.body;
    nextForm.primaryTimezone = selectedDraft.primaryTimezone;
    nextForm.campaignLabel = selectedDraft.campaignLabel ?? "";
    nextForm.approvalRouteId = selectedDraft.approvalRouteId ?? "";
    nextForm.metadataJson = JSON.stringify(selectedDraft.metadata ?? {}, null, 2);
    nextForm.scheduledFor = selectedDraft.scheduledJob?.scheduledFor
      ? new Date(selectedDraft.scheduledJob.scheduledFor).toISOString().slice(0, 16)
      : "";

    const firstAsset = selectedDraft.assets[0];
    if (firstAsset) {
      nextForm.mediaBucket = firstAsset.bucketName;
      nextForm.mediaObjectKey = firstAsset.objectKey;
      nextForm.mediaMimeType = firstAsset.mimeType;
      nextForm.mediaKind = firstAsset.mediaKind;
      nextForm.mediaSizeBytes = String(firstAsset.sizeBytes);
      nextForm.mediaChecksum = firstAsset.sha256Checksum;
      nextForm.mediaAltText = firstAsset.altText ?? "";
      nextForm.mediaSourceUrl = firstAsset.sourceUrl ?? "";
    }

    selectedDraft.variants.forEach((variant) => {
      nextForm.variants[variant.provider] = {
        caption: variant.caption,
        linkUrl: variant.linkUrl ?? "",
        firstComment: variant.firstComment ?? "",
        targetAccountIds: variant.targetAccountIds
      };
    });

    setForm(nextForm);
  }

  function buildRequest(): SaveDraftRequest {
    return {
      title: form.title,
      body: form.body,
      primaryTimezone: form.primaryTimezone,
      approvalRouteId: form.approvalRouteId || undefined,
      campaignLabel: form.campaignLabel || undefined,
      metadata: JSON.parse(form.metadataJson || "{}") as Record<string, unknown>,
      mediaAssets: [
        {
          bucketName: form.mediaBucket,
          objectKey: form.mediaObjectKey,
          mimeType: form.mediaMimeType,
          mediaKind: form.mediaKind as SaveDraftRequest["mediaAssets"][number]["mediaKind"],
          sizeBytes: Number(form.mediaSizeBytes),
          sha256Checksum: form.mediaChecksum,
          altText: form.mediaAltText || undefined,
          sourceUrl: form.mediaSourceUrl || undefined
        }
      ],
      variants: providers.map((provider) => ({
        provider,
        caption: form.variants[provider].caption || form.body,
        linkUrl: form.variants[provider].linkUrl || undefined,
        firstComment: form.variants[provider].firstComment || undefined,
        providerOptions: {
          source: "phase-6-composer",
          futureAiReady: true
        },
        targetAccountIds: form.variants[provider].targetAccountIds
      }))
    };
  }

  function toggleTarget(provider: SocialProvider, accountId: string) {
    setForm((current) => {
      const currentTargets = current.variants[provider].targetAccountIds;
      const nextTargets = currentTargets.includes(accountId)
        ? currentTargets.filter((value) => value !== accountId)
        : [...currentTargets, accountId];
      return {
        ...current,
        variants: {
          ...current.variants,
          [provider]: {
            ...current.variants[provider],
            targetAccountIds: nextTargets
          }
        }
      };
    });
  }

  return (
    <div className="grid gap-6">
      <Card>
        <div className="flex items-center justify-between gap-3">
          <div>
            <CardTitle>Post Composer</CardTitle>
            <p className="mt-2 text-sm text-slate-600">
              Workspace-scoped drafts, media metadata, approval routing, and publish-ready variants.
            </p>
          </div>
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-[220px_1fr]">
          <div className="space-y-3 rounded-[28px] border border-slate-200 bg-slate-50 p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-slate-800">Draft Queue</p>
              <button
                className="text-xs font-semibold uppercase tracking-[0.18em] text-sky-700"
                onClick={() => {
                  setSelectedDraftId(null);
                  setForm(emptyForm());
                  setFeedback("Composer reset for a new draft.");
                }}
                type="button"
              >
                New
              </button>
            </div>
            {(draftsQuery.data ?? []).map((draft) => (
              <button
                className={cn(
                  "w-full rounded-2xl border px-3 py-3 text-left transition",
                  selectedDraftId === draft.draftId
                    ? "border-slate-950 bg-slate-950 text-white"
                    : "border-slate-200 bg-white text-slate-700 hover:border-slate-300"
                )}
                key={draft.draftId}
                onClick={() => setSelectedDraftId(draft.draftId)}
                type="button"
              >
                <p className="font-semibold">{draft.title}</p>
                <p className="mt-1 text-xs uppercase tracking-[0.18em] opacity-70">
                  {draft.lifecycleStatus.replace("_", " ")}
                </p>
              </button>
            ))}
          </div>

          <div className="space-y-5">
            {selectedDraft && (
              <button
                className="rounded-full border border-slate-200 px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-600"
                onClick={hydrateFromServer}
                type="button"
              >
                Load Selected Draft
              </button>
            )}
            {feedback && (
              <div className="rounded-2xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-900">
                {feedback}
              </div>
            )}
            <div className="flex space-x-2 border-b border-slate-200 pb-px">
              {(["content", "media", "platforms", "advanced"] as const).map((tab) => (
                <button
                  key={tab}
                  className={cn(
                    "px-4 py-2 text-sm font-medium border-b-2 transition-colors",
                    activeTab === tab
                      ? "border-sky-500 text-sky-600"
                      : "border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300"
                  )}
                  onClick={() => setActiveTab(tab)}
                  type="button"
                >
                  {tab.charAt(0).toUpperCase() + tab.slice(1)}
                </button>
              ))}
            </div>

            {activeTab === "content" && (
              <div className="space-y-4">
                <div className="grid gap-4 md:grid-cols-2">
                  <Field label="Title">
                    <input
                      className={inputClassName}
                      onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                      value={form.title}
                    />
                  </Field>
                  <Field label="Campaign">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, campaignLabel: event.target.value }))
                      }
                      value={form.campaignLabel}
                    />
                  </Field>
                </div>
                <Field label="Body">
                  <textarea
                    className="min-h-32 w-full rounded-3xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-sky-400"
                    onChange={(event) => setForm((current) => ({ ...current, body: event.target.value }))}
                    value={form.body}
                  />
                </Field>
              </div>
            )}

            {activeTab === "media" && (
              <div className="space-y-4">
                <div className="grid gap-4 md:grid-cols-2">
                  <Field label="Media Bucket">
                    <input
                      className={inputClassName}
                      onChange={(event) => setForm((current) => ({ ...current, mediaBucket: event.target.value }))}
                      value={form.mediaBucket}
                    />
                  </Field>
                  <Field label="Media Object Key">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, mediaObjectKey: event.target.value }))
                      }
                      value={form.mediaObjectKey}
                    />
                  </Field>
                </div>
                <div className="grid gap-4 md:grid-cols-4">
                  <Field label="MIME">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, mediaMimeType: event.target.value }))
                      }
                      value={form.mediaMimeType}
                    />
                  </Field>
                  <Field label="Kind">
                    <input
                      className={inputClassName}
                      onChange={(event) => setForm((current) => ({ ...current, mediaKind: event.target.value }))}
                      value={form.mediaKind}
                    />
                  </Field>
                  <Field label="Size">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, mediaSizeBytes: event.target.value }))
                      }
                      value={form.mediaSizeBytes}
                    />
                  </Field>
                  <Field label="Checksum">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, mediaChecksum: event.target.value }))
                      }
                      value={form.mediaChecksum}
                    />
                  </Field>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <Field label="Alt Text">
                    <input
                      className={inputClassName}
                      onChange={(event) => setForm((current) => ({ ...current, mediaAltText: event.target.value }))}
                      value={form.mediaAltText}
                      placeholder="Image Description"
                    />
                  </Field>
                  <Field label="Source URL">
                    <input
                      className={inputClassName}
                      onChange={(event) => setForm((current) => ({ ...current, mediaSourceUrl: event.target.value }))}
                      value={form.mediaSourceUrl}
                      placeholder="https://..."
                    />
                  </Field>
                </div>
              </div>
            )}

            {activeTab === "platforms" && (
              <div className="grid gap-4">
                {providers.map((provider) => (
                  <div className="rounded-[28px] border border-slate-200 p-4" key={provider}>
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-600">
                        {provider}
                      </p>
                      <span className="text-xs text-slate-500">
                        {(accountsByProvider[provider] ?? []).length} connected account(s)
                      </span>
                    </div>
                    <div className="mt-4 grid gap-3 md:grid-cols-3">
                      <input
                        className={inputClassName}
                        onChange={(event) =>
                          setForm((current) => ({
                            ...current,
                            variants: {
                              ...current.variants,
                              [provider]: {
                                ...current.variants[provider],
                                caption: event.target.value
                              }
                            }
                          }))
                        }
                        placeholder="Caption"
                        value={form.variants[provider].caption}
                      />
                      <input
                        className={inputClassName}
                        onChange={(event) =>
                          setForm((current) => ({
                            ...current,
                            variants: {
                              ...current.variants,
                              [provider]: {
                                ...current.variants[provider],
                                linkUrl: event.target.value
                              }
                            }
                          }))
                        }
                        placeholder="Link URL"
                        value={form.variants[provider].linkUrl}
                      />
                      <input
                        className={inputClassName}
                        onChange={(event) =>
                          setForm((current) => ({
                            ...current,
                            variants: {
                              ...current.variants,
                              [provider]: {
                                ...current.variants[provider],
                                firstComment: event.target.value
                              }
                            }
                          }))
                        }
                        placeholder="First comment / reply"
                        value={form.variants[provider].firstComment}
                      />
                    </div>
                    <div className="mt-4 flex flex-wrap gap-2">
                      {(accountsByProvider[provider] ?? []).map((account) => (
                        <button
                          className={cn(
                            "rounded-full border px-3 py-2 text-xs font-semibold transition",
                            form.variants[provider].targetAccountIds.includes(account.connectedAccountId)
                              ? "border-slate-950 bg-slate-950 text-white"
                              : "border-slate-200 bg-slate-50 text-slate-700"
                          )}
                          key={account.connectedAccountId}
                          onClick={() => toggleTarget(provider, account.connectedAccountId)}
                          type="button"
                        >
                          {account.displayName}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {activeTab === "advanced" && (
              <div className="space-y-4">
                <div className="grid gap-4 md:grid-cols-3">
                  <Field label="Timezone">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, primaryTimezone: event.target.value }))
                      }
                      value={form.primaryTimezone}
                    />
                  </Field>
                  <Field label="Approval Route">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, approvalRouteId: event.target.value }))
                      }
                      placeholder="UUID from Phase 4/5 workspace bootstrap"
                      value={form.approvalRouteId}
                    />
                  </Field>
                  <Field label="Schedule">
                    <input
                      className={inputClassName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, scheduledFor: event.target.value }))
                      }
                      type="datetime-local"
                      value={form.scheduledFor}
                    />
                  </Field>
                </div>
                <Field label="Future AI Metadata">
                  <textarea
                    className="min-h-28 w-full rounded-3xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-sky-400"
                    onChange={(event) => setForm((current) => ({ ...current, metadataJson: event.target.value }))}
                    value={form.metadataJson}
                  />
                </Field>
              </div>
            )}

            <div className="flex flex-wrap gap-3">
              <Button
                disabled={saveMutation.isPending}
                onClick={() => void saveMutation.mutate(buildRequest())}
                type="button"
              >
                {saveMutation.isPending ? "Saving..." : "Save Draft"}
              </Button>
              <Button
                disabled={selectedDraftId == null || approvalMutation.isPending}
                onClick={() => selectedDraftId && void approvalMutation.mutate(selectedDraftId)}
                type="button"
                variant="secondary"
              >
                Submit for Approval
              </Button>
              <Button
                disabled={selectedDraftId == null || form.scheduledFor.length === 0 || scheduleMutation.isPending}
                onClick={() => selectedDraftId && void scheduleMutation.mutate(selectedDraftId)}
                type="button"
              >
                Queue Schedule
              </Button>
            </div>
          </div>
        </div>
      </Card>

      <Card className="bg-slate-950 text-white">
        <CardTitle className="text-white">Preview Stack</CardTitle>
        <p className="mt-3 text-sm leading-7 text-slate-300">
          Live draft preview with platform variants, account targets, and dispatch-ready scheduling context.
        </p>
        <div className="mt-6 grid gap-4 grid-cols-1 md:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-[28px] bg-white/10 p-5">
            <p className="text-sm text-slate-300">Character count</p>
            <p className="mt-2 font-display text-4xl">{form.body.length}</p>
          </div>
          <div className="rounded-[28px] bg-white/10 p-5 text-sm leading-7 text-slate-200">
            <p className="font-semibold text-white">{form.title}</p>
            <p className="mt-3">{form.body}</p>
            <p className="mt-4 text-slate-300">
              Schedule: {form.scheduledFor || "Not scheduled"} | Timezone: {form.primaryTimezone}
            </p>
          </div>
          {providers.map((provider) => (
            <div className="rounded-[28px] border border-white/10 bg-white/5 p-4" key={provider}>
              <div className="flex items-center justify-between">
                <p className="font-semibold capitalize">{provider}</p>
                <span className="text-xs uppercase tracking-[0.18em] text-slate-400">
                  {form.variants[provider].targetAccountIds.length} targets
                </span>
              </div>
              <p className="mt-3 text-sm text-slate-300">
                {form.variants[provider].caption || form.body || "No provider-specific caption yet."}
              </p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="grid gap-2">
      <span className="text-sm font-semibold text-slate-700">{label}</span>
      {children}
    </label>
  );
}
