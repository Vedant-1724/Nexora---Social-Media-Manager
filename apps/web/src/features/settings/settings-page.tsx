import { useState, useEffect } from "react";
import { Card, CardTitle, Badge, Button, cn } from "@nexora/ui";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { 
  getWorkspaceSubscription, 
  getWorkspaceInvoices,
  getNotificationPreferences,
  updateNotificationPreference
} from "@/lib/api";
import {
  Bell,
  CreditCard,
  Globe,
  Key,
  Link2,
  Monitor,
  Moon,
  Palette,
  Shield,
  Sun,
  User,
  Zap
} from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";

const settingsTabs = [
  { id: "profile", label: "Profile", icon: User },
  { id: "connections", label: "Connections", icon: Link2 },
  { id: "billing", label: "Billing", icon: CreditCard },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "security", label: "Security", icon: Shield },
  { id: "appearance", label: "Appearance", icon: Palette }
] as const;

type TabId = typeof settingsTabs[number]["id"];

const connectedAccounts = [
  { provider: "Meta (Facebook)", status: "connected", icon: "📱", gradient: "from-blue-500 to-indigo-600", expires: "Jan 2027" },
  { provider: "Instagram", status: "connected", icon: "📸", gradient: "from-pink-500 to-purple-600", expires: "Mar 2027" },
  { provider: "LinkedIn", status: "connected", icon: "💼", gradient: "from-sky-500 to-blue-700", expires: "Feb 2027" },
  { provider: "X (Twitter)", status: "expired", icon: "✖️", gradient: "from-slate-500 to-slate-700", expires: "Expired" }
];

const notificationSettings = [
  { label: "Post published", description: "When a scheduled post is published successfully", emailEnabled: true, pushEnabled: true },
  { label: "Approval required", description: "When a post needs your approval", emailEnabled: true, pushEnabled: true },
  { label: "Post failed", description: "When publishing fails on any platform", emailEnabled: true, pushEnabled: false },
  { label: "New team member", description: "When someone joins your workspace", emailEnabled: false, pushEnabled: true },
  { label: "Usage alerts", description: "When approaching plan limits", emailEnabled: true, pushEnabled: false }
];

function Toggle({ enabled, onToggle }: { enabled: boolean; onToggle: () => void }) {
  return (
    <button
      className={cn(
        "relative inline-flex h-6 w-11 items-center rounded-full transition-all duration-300",
        enabled ? "bg-sky-500 shadow-lg shadow-sky-500/30" : "bg-slate-200"
      )}
      onClick={onToggle}
      type="button"
    >
      <span
        className={cn(
          "inline-block h-4 w-4 rounded-full bg-white shadow-md transition-transform duration-300",
          enabled ? "translate-x-6" : "translate-x-1"
        )}
      />
    </button>
  );
}

export function SettingsPage() {
  const { session } = useAuth();
  const accessToken = session?.session?.accessToken ?? "";
  const workspaceId = session?.currentWorkspace?.workspaceId ?? "";

  const [activeTab, setActiveTab] = useState<TabId>("profile");
  const [notifications, setNotifications] = useState(notificationSettings);

  const { data: subscription } = useQuery({
    queryKey: ["billing-subscription", workspaceId],
    queryFn: () => getWorkspaceSubscription(workspaceId, accessToken),
    enabled: !!workspaceId && !!accessToken && activeTab === "billing",
    retry: 1
  });

  const { data: preferenceData, refetch: refetchPrefs } = useQuery({
    queryKey: ["notification-preferences", workspaceId],
    queryFn: () => getNotificationPreferences(workspaceId, accessToken),
    enabled: !!workspaceId && !!accessToken && activeTab === "notifications"
  });

  const { data: invoices } = useQuery({
    queryKey: ["billing-invoices", workspaceId],
    queryFn: () => getWorkspaceInvoices(workspaceId, accessToken),
    enabled: !!workspaceId && !!accessToken && activeTab === "billing",
    retry: 1
  });

  useEffect(() => {
    if (preferenceData) {
      setNotifications(prev => prev.map(n => {
        // Map UI labels to backend event codes
        const codeMap: Record<string, string> = {
          "Post published": "post.published",
          "Approval required": "approvals.required",
          "Post failed": "post.failed",
          "New team member": "team.invited",
          "Usage alerts": "billing.usage_alert"
        };
        const eventCode = codeMap[n.label];
        if (!eventCode) return n;

        const emailPref = preferenceData.find(p => p.eventCode === eventCode && p.channel === 'email');
        const inAppPref = preferenceData.find(p => p.eventCode === eventCode && p.channel === 'in_app');

        return {
          ...n,
          emailEnabled: emailPref ? emailPref.enabled : n.emailEnabled,
          pushEnabled: inAppPref ? inAppPref.enabled : n.pushEnabled,
        };
      }));
    }
  }, [preferenceData]);

  const updatePrefMutation = useMutation({
    mutationFn: (payload: { eventCode: string, channel: string, enabled: boolean }) => 
      updateNotificationPreference(workspaceId, payload, accessToken)
  });

  const toggleNotification = (index: number, field: "emailEnabled" | "pushEnabled") => {
    const item = notifications[index];
    const newValue = !item[field];
    
    // Optimistic update
    setNotifications((prev) =>
      prev.map((n, i) => (i === index ? { ...n, [field]: newValue } : n))
    );

    // Map to backend event code
    const codeMap: Record<string, string> = {
      "Post published": "post.published",
      "Approval required": "approvals.required",
      "Post failed": "post.failed",
      "New team member": "team.invited",
      "Usage alerts": "billing.usage_alert"
    };

    const eventCode = codeMap[item.label];
    if (eventCode && workspaceId && accessToken) {
      updatePrefMutation.mutate({
        eventCode,
        channel: field === "emailEnabled" ? "email" : "in_app",
        enabled: newValue
      });
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h2 className="font-display text-2xl text-slate-950">Settings</h2>
        <p className="mt-1 text-sm text-slate-500">Manage your workspace preferences and integrations.</p>
      </div>

      <div className="grid gap-4 lg:grid-cols-[220px_1fr]">
        {/* ── Tabs ───────────────────────────────────────────────── */}
        <nav className="space-y-1 stagger">
          {settingsTabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                className={cn(
                  "group flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-sm font-semibold transition-all duration-300",
                  activeTab === tab.id
                    ? "bg-slate-950 text-white shadow-lg shadow-slate-950/15"
                    : "text-slate-500 hover:bg-slate-100/80 hover:text-slate-700"
                )}
                onClick={() => setActiveTab(tab.id)}
                type="button"
              >
                <Icon className="h-[18px] w-[18px] transition-transform duration-300 group-hover:scale-110" />
                {tab.label}
              </button>
            );
          })}
        </nav>

        {/* ── Tab Content ───────────────────────────────────────── */}
        <div className="animate-fade-in" key={activeTab}>
          {/* Profile */}
          {activeTab === "profile" && (
            <Card className="hover-lift">
              <CardTitle className="text-lg mb-6">Profile Settings</CardTitle>
              <div className="flex flex-col gap-6 sm:flex-row sm:items-start">
                <div className="flex flex-col items-center gap-3">
                  <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-br from-sky-400 to-sky-600 text-2xl font-bold text-white shadow-xl shadow-sky-500/25 transition-transform hover:scale-105">
                    {session?.user?.displayName?.charAt(0) ?? "N"}
                  </div>
                  <button className="text-xs font-semibold text-sky-600 hover:text-sky-700 transition-colors">Change</button>
                </div>
                <div className="flex-1 space-y-4">
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-[0.2em] text-slate-400 mb-1.5">Display Name</label>
                    <input
                      className="w-full rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-3 text-sm backdrop-blur transition-all focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20"
                      defaultValue={session?.user?.displayName ?? ""}
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-[0.2em] text-slate-400 mb-1.5">Email Address</label>
                    <input
                      className="w-full rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-3 text-sm backdrop-blur transition-all focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20"
                      defaultValue={session?.user?.email ?? ""}
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold uppercase tracking-[0.2em] text-slate-400 mb-1.5">Timezone</label>
                      <select className="w-full rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-3 text-sm backdrop-blur transition-all focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20">
                        <option>Asia/Kolkata (IST)</option>
                        <option>America/New_York (EST)</option>
                        <option>Europe/London (GMT)</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs font-bold uppercase tracking-[0.2em] text-slate-400 mb-1.5">Language</label>
                      <select className="w-full rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-3 text-sm backdrop-blur transition-all focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20">
                        <option>English</option>
                        <option>Hindi</option>
                      </select>
                    </div>
                  </div>
                  <div className="pt-2">
                    <Button className="shadow-lg shadow-sky-600/20">Save Changes</Button>
                  </div>
                </div>
              </div>
            </Card>
          )}

          {/* Connections */}
          {activeTab === "connections" && (
            <div className="space-y-4">
              <Card className="hover-lift">
                <CardTitle className="text-lg mb-2">Connected Platforms</CardTitle>
                <p className="text-sm text-slate-500 mb-6">Manage your social media account connections.</p>
                <div className="space-y-3 stagger">
                  {connectedAccounts.map((account) => (
                    <div
                      key={account.provider}
                      className="group flex items-center justify-between rounded-2xl border border-slate-100 p-4 transition-all hover:border-sky-200/60 hover:shadow-sm"
                    >
                      <div className="flex items-center gap-4">
                        <div className={cn(
                          "flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br text-lg shadow-lg transition-transform duration-300 group-hover:scale-110",
                          account.gradient
                        )}>
                          {account.icon}
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-slate-800">{account.provider}</p>
                          <p className="mt-0.5 text-xs text-slate-400">
                            {account.status === "connected" ? `Expires ${account.expires}` : "Token expired"}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        {account.status === "connected" ? (
                          <Badge className="border-emerald-200 bg-emerald-50 text-emerald-700 text-[10px]">Connected</Badge>
                        ) : (
                          <Badge className="border-red-200 bg-red-50 text-red-600 text-[10px]">Expired</Badge>
                        )}
                        <Button
                          variant={account.status === "expired" ? "primary" : "ghost"}
                          className="text-xs"
                        >
                          {account.status === "expired" ? "Reconnect" : "Manage"}
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
              <Card className="hover-lift glass-dark bg-slate-950 text-white border-white/5">
                <div className="flex items-center gap-3">
                  <Zap className="h-5 w-5 text-amber-400" />
                  <div>
                    <p className="text-sm font-semibold">Connect more platforms</p>
                    <p className="text-xs text-slate-400">TikTok, YouTube, and Pinterest coming soon.</p>
                  </div>
                </div>
              </Card>
            </div>
          )}

          {/* Billing */}
          {activeTab === "billing" && (
            <div className="space-y-4">
              <Card className="hover-lift">
                <CardTitle className="text-lg mb-6">Current Subscription</CardTitle>
                {subscription ? (
                  <div className="rounded-2xl border border-slate-100 p-5 bg-gradient-to-br from-slate-50 to-white">
                    <div className="flex items-start justify-between">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="text-xl font-display font-semibold text-slate-900">{subscription.planName}</h3>
                          <Badge className="bg-sky-100 text-sky-700 capitalize">{subscription.status}</Badge>
                        </div>
                        <p className="mt-1 text-sm text-slate-500">Renews on {new Date(subscription.currentPeriodEnd).toLocaleDateString()}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-semibold text-slate-900">{subscription.seatCount} Seats</p>
                      </div>
                    </div>
                    <div className="mt-6 pt-5 border-t border-slate-100 flex gap-3">
                      <Button variant="primary">Manage Subscription</Button>
                      <Button variant="secondary">Change Plan</Button>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">Loading subscription details...</p>
                )}
              </Card>

              <Card className="hover-lift">
                <CardTitle className="text-lg mb-6">Billing History</CardTitle>
                <div className="space-y-3">
                  {invoices?.length ? invoices.map((inv) => (
                    <div key={inv.id} className="flex items-center justify-between rounded-2xl border border-slate-100 p-4 transition-all hover:border-sky-200/60 hover:shadow-sm">
                      <div className="flex items-center gap-4">
                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                          <CreditCard className="h-5 w-5" />
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-slate-800">${(inv.amountPaidMinor / 100).toFixed(2)} {inv.currency}</p>
                          <p className="text-xs text-slate-400">{new Date(inv.issuedAt).toLocaleDateString()} · {inv.invoiceNumber}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <Badge className="bg-emerald-50 border-emerald-200 text-emerald-600 capitalize">
                          {inv.status}
                        </Badge>
                        <Button variant="ghost" className="text-xs" onClick={() => inv.hostedInvoiceUrl && window.open(inv.hostedInvoiceUrl, "_blank")}>
                          View
                        </Button>
                      </div>
                    </div>
                  )) : (
                    <p className="text-sm text-slate-500">No invoices found.</p>
                  )}
                </div>
              </Card>
            </div>
          )}

          {/* Notifications */}
          {activeTab === "notifications" && (
            <Card className="hover-lift">
              <CardTitle className="text-lg mb-2">Notification Preferences</CardTitle>
              <p className="text-sm text-slate-500 mb-6">Choose how you want to be notified.</p>
              <div className="mb-4 grid grid-cols-[1fr_60px_60px] gap-2 px-4">
                <span />
                <span className="text-center text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Email</span>
                <span className="text-center text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Push</span>
              </div>
              <div className="space-y-2 stagger">
                {notifications.map((item, i) => (
                  <div
                    key={item.label}
                    className="grid grid-cols-[1fr_60px_60px] items-center gap-2 rounded-2xl border border-slate-100 px-4 py-3.5 transition-colors hover:border-sky-200/40 hover:bg-sky-50/20"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slate-800">{item.label}</p>
                      <p className="mt-0.5 text-xs text-slate-400">{item.description}</p>
                    </div>
                    <div className="flex justify-center">
                      <Toggle enabled={item.emailEnabled} onToggle={() => toggleNotification(i, "emailEnabled")} />
                    </div>
                    <div className="flex justify-center">
                      <Toggle enabled={item.pushEnabled} onToggle={() => toggleNotification(i, "pushEnabled")} />
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* Security */}
          {activeTab === "security" && (
            <div className="space-y-4">
              <Card className="hover-lift">
                <CardTitle className="text-lg mb-6">Security Settings</CardTitle>
                <div className="space-y-6">
                  <div className="flex items-center justify-between rounded-2xl border border-slate-100 p-4">
                    <div className="flex items-center gap-3">
                      <Key className="h-5 w-5 text-slate-500" />
                      <div>
                        <p className="text-sm font-semibold text-slate-800">Password</p>
                        <p className="text-xs text-slate-400">Last changed 30 days ago</p>
                      </div>
                    </div>
                    <Button variant="secondary" className="text-xs">Change Password</Button>
                  </div>
                  <div className="flex items-center justify-between rounded-2xl border border-slate-100 p-4">
                    <div className="flex items-center gap-3">
                      <Shield className="h-5 w-5 text-slate-500" />
                      <div>
                        <p className="text-sm font-semibold text-slate-800">Two-Factor Authentication</p>
                        <p className="text-xs text-slate-400">Add an extra layer of security</p>
                      </div>
                    </div>
                    <Badge className="border-amber-200 bg-amber-50 text-amber-700 text-[10px]">Not Enabled</Badge>
                  </div>
                  <div className="flex items-center justify-between rounded-2xl border border-slate-100 p-4">
                    <div className="flex items-center gap-3">
                      <Monitor className="h-5 w-5 text-slate-500" />
                      <div>
                        <p className="text-sm font-semibold text-slate-800">Active Sessions</p>
                        <p className="text-xs text-slate-400">1 active session on this device</p>
                      </div>
                    </div>
                    <Button variant="ghost" className="text-xs text-red-500 hover:text-red-600 hover:bg-red-50">Revoke All</Button>
                  </div>
                </div>
              </Card>
            </div>
          )}

          {/* Appearance */}
          {activeTab === "appearance" && (
            <Card className="hover-lift">
              <CardTitle className="text-lg mb-6">Appearance</CardTitle>
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.2em] text-slate-400 mb-3">Theme</p>
                <div className="grid grid-cols-3 gap-3">
                  {[
                    { label: "Light", icon: Sun, active: true },
                    { label: "Dark", icon: Moon, active: false },
                    { label: "System", icon: Globe, active: false }
                  ].map((theme) => {
                    const ThemeIcon = theme.icon;
                    return (
                      <button
                        key={theme.label}
                        className={cn(
                          "flex flex-col items-center gap-2 rounded-2xl border p-5 text-sm font-semibold transition-all duration-200",
                          theme.active
                            ? "border-sky-300 bg-sky-50/60 text-sky-700 shadow-lg shadow-sky-500/10"
                            : "border-slate-100 text-slate-500 hover:border-slate-200 hover:bg-slate-50/50"
                        )}
                        type="button"
                      >
                        <ThemeIcon className="h-6 w-6" />
                        {theme.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
