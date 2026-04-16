import { useState } from "react";
import { Card, CardTitle, Badge, Button, cn } from "@nexora/ui";
import { Mail, Shield, UserPlus, MoreHorizontal, Clock, CheckCircle2 } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";

const teamMembers = [
  { name: "Priya Sharma", email: "priya@nexora.io", role: "Admin", status: "active", avatar: "PS", gradient: "from-sky-400 to-sky-600", joinedAt: "Jan 2025" },
  { name: "Arjun Mehta", email: "arjun@nexora.io", role: "Content Editor", status: "active", avatar: "AM", gradient: "from-emerald-400 to-emerald-600", joinedAt: "Jan 2025" },
  { name: "Sara Khan", email: "sara@nexora.io", role: "Analyst", status: "active", avatar: "SK", gradient: "from-violet-400 to-violet-600", joinedAt: "Feb 2025" },
  { name: "Dev Rathod", email: "dev@nexora.io", role: "Approver", status: "active", avatar: "DR", gradient: "from-amber-400 to-amber-600", joinedAt: "Mar 2025" },
  { name: "Meera Joshi", email: "meera@nexora.io", role: "Viewer", status: "pending", avatar: "MJ", gradient: "from-pink-400 to-pink-600", joinedAt: "Invited" }
];

const roleDescriptions: Record<string, { description: string; permissions: string[] }> = {
  Admin: { description: "Full workspace control", permissions: ["All permissions", "Billing access", "Member management"] },
  "Content Editor": { description: "Create and publish content", permissions: ["Create drafts", "Publish posts", "Manage media"] },
  Analyst: { description: "View reports and data", permissions: ["View analytics", "Export reports", "View posts"] },
  Approver: { description: "Review and approve content", permissions: ["Approve drafts", "Reject posts", "View content"] },
  Viewer: { description: "Read-only access", permissions: ["View dashboard", "View posts", "View calendar"] }
};

const recentActivity = [
  { user: "Priya S.", action: "invited Meera Joshi", time: "2h ago" },
  { user: "Arjun M.", action: "updated role to Content Editor", time: "1d ago" },
  { user: "Sara K.", action: "joined the workspace", time: "3d ago" },
  { user: "Admin", action: "created workspace", time: "2mo ago" }
];

export function TeamPage() {
  const { session } = useAuth();
  const [showInvite, setShowInvite] = useState(false);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ── Header ──────────────────────────────────────────────── */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="font-display text-2xl text-slate-950">Team</h2>
          <p className="mt-1 text-sm text-slate-500">Manage members, roles, and permissions for your workspace.</p>
        </div>
        <Button
          onClick={() => setShowInvite(!showInvite)}
          className="shadow-lg shadow-sky-600/20 transition-all hover:scale-[1.03]"
        >
          <UserPlus className="mr-2 h-4 w-4" />
          Invite Member
        </Button>
      </div>

      {/* ── Invite Panel ────────────────────────────────────────── */}
      {showInvite && (
        <Card className="animate-fade-in-down glow-border border-sky-200/50">
          <CardTitle className="text-lg mb-4">Invite a New Member</CardTitle>
          <div className="flex flex-col gap-3 sm:flex-row">
            <input
              className="flex-1 rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-3 text-sm backdrop-blur transition-all focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20"
              placeholder="email@example.com"
              type="email"
            />
            <select className="rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-3 text-sm backdrop-blur transition-all focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20">
              <option>Content Editor</option>
              <option>Analyst</option>
              <option>Approver</option>
              <option>Viewer</option>
            </select>
            <Button className="shadow-lg shadow-sky-600/20">
              <Mail className="mr-2 h-4 w-4" />
              Send Invite
            </Button>
          </div>
        </Card>
      )}

      <div className="grid gap-4 lg:grid-cols-[1.5fr_1fr]">
        {/* ── Member Cards ──────────────────────────────────────── */}
        <div className="space-y-3 stagger">
          {teamMembers.map((member) => (
            <Card key={member.email} className="group hover-lift glow-border !p-4">
              <div className="flex items-center gap-4">
                <div className={cn(
                  "flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br text-sm font-bold text-white shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3",
                  member.gradient
                )}>
                  {member.avatar}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-slate-900">{member.name}</p>
                    {member.status === "active" ? (
                      <div className="flex items-center gap-1">
                        <div className="h-1.5 w-1.5 rounded-full bg-emerald-400 shadow-[0_0_4px_rgba(34,197,94,0.5)]" />
                        <span className="text-[10px] font-semibold text-emerald-600">Active</span>
                      </div>
                    ) : (
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3 text-amber-500" />
                        <span className="text-[10px] font-semibold text-amber-600">Pending</span>
                      </div>
                    )}
                  </div>
                  <p className="mt-0.5 text-xs text-slate-400">{member.email}</p>
                </div>
                <div className="flex items-center gap-3">
                  <div className="text-right hidden sm:block">
                    <span className="inline-flex items-center gap-1 rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                      <Shield className="h-3 w-3" />
                      {member.role}
                    </span>
                    <p className="mt-1 text-[10px] text-slate-400">{member.joinedAt}</p>
                  </div>
                  <button className="rounded-xl p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600">
                    <MoreHorizontal className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </Card>
          ))}
        </div>

        {/* ── Sidebar ───────────────────────────────────────────── */}
        <div className="space-y-4">
          {/* Role Definitions */}
          <Card className="hover-lift">
            <CardTitle className="text-lg mb-4">Roles & Permissions</CardTitle>
            <div className="space-y-4">
              {Object.entries(roleDescriptions).map(([role, info]) => (
                <div key={role} className="rounded-2xl border border-slate-100 p-3.5 transition-colors hover:border-sky-200/60 hover:bg-sky-50/20">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-semibold text-slate-800">{role}</p>
                    <span className="text-[10px] text-slate-400">{info.description}</span>
                  </div>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {info.permissions.map((perm) => (
                      <span key={perm} className="inline-flex items-center gap-1 rounded-lg bg-slate-50 px-2 py-0.5 text-[10px] font-medium text-slate-500">
                        <CheckCircle2 className="h-2.5 w-2.5 text-emerald-500" />
                        {perm}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* Recent Activity */}
          <Card className="hover-lift">
            <CardTitle className="text-lg mb-4">Recent Activity</CardTitle>
            <div className="space-y-3 stagger">
              {recentActivity.map((item) => (
                <div key={`${item.user}-${item.time}`} className="flex items-start gap-3">
                  <div className="mt-0.5 h-2 w-2 rounded-full bg-sky-400 shadow-[0_0_4px_rgba(14,165,233,0.4)]" />
                  <div>
                    <p className="text-sm text-slate-700">
                      <span className="font-semibold text-slate-900">{item.user}</span>{" "}
                      {item.action}
                    </p>
                    <p className="mt-0.5 text-xs text-slate-400">{item.time}</p>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
