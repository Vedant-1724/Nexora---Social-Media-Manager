import { Link, NavLink, Outlet } from "react-router-dom";
import {
  BarChart3,
  Bell,
  Calendar,
  ChevronDown,
  Inbox,
  LayoutDashboard,
  LogOut,
  PenSquare,
  Search,
  Settings,
  Users,
  Link2
} from "lucide-react";
import { Badge, Card, cn, Button } from "@nexora/ui";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { getInAppNotifications, markNotificationRead } from "@/lib/api";

import { useAuth } from "@/features/auth/auth-context";

const navigationItems = [
  { label: "Dashboard", href: "/app/dashboard", icon: LayoutDashboard },
  { label: "Inbox", href: "/app/inbox", icon: Inbox },
  { label: "Calendar", href: "/app/calendar", icon: Calendar },
  { label: "Composer", href: "/app/composer", icon: PenSquare },
  { label: "Bio Pages", href: "/app/bio-pages", icon: Link2 },
  { label: "Analytics", href: "/app/analytics", icon: BarChart3 },
  { label: "Team", href: "/app/team", icon: Users },
  { label: "Settings", href: "/app/settings", icon: Settings }
];

export function AppShell() {
  const { logout, session, switchWorkspace } = useAuth();
  const workspaces = session?.workspaces ?? [];
  const currentWorkspace = session?.currentWorkspace;
  const workspaceId = currentWorkspace?.workspaceId ?? "";
  const accessToken = session?.session?.accessToken ?? "";

  const [notificationsOpen, setNotificationsOpen] = useState(false);

  const { data: notifications, refetch } = useQuery({
    queryKey: ["in-app-notifications", workspaceId],
    queryFn: () => getInAppNotifications(workspaceId, accessToken),
    enabled: !!workspaceId && !!accessToken,
    refetchInterval: 30000 // Poll every 30s
  });

  const unreadCount = notifications?.filter(n => !n.read).length || 0;

  const handleMarkRead = async (id: string) => {
    if (workspaceId && accessToken) {
      try {
        await markNotificationRead(workspaceId, id, accessToken);
        void refetch();
      } catch (err) {
        console.error("Failed to mark read");
      }
    }
  };

  return (
    <div className="min-h-screen bg-transparent px-3 py-3 lg:px-5 lg:py-4">
      <div className="mx-auto grid max-w-[1440px] gap-4 lg:grid-cols-[260px_1fr]">
        {/* ── Sidebar ────────────────────────────────────────────────── */}
        <aside className="glass-dark rounded-[28px] p-5 shadow-[0_0_40px_rgba(168,85,247,0.15)] animate-fade-in">
          <Link className="flex items-center gap-3" to="/">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-purple-500 to-sky-500 shadow-lg shadow-purple-500/30">
              <span className="text-lg font-bold text-white">N</span>
            </div>
            <span className="font-display text-2xl font-semibold text-white">Nexora</span>
          </Link>

          <nav className="mt-8 space-y-1 stagger">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  className={({ isActive }) =>
                    cn(
                      "group flex items-center gap-3 rounded-2xl px-4 py-3 text-sm font-semibold transition-all duration-300",
                      isActive
                        ? "glass-light text-white shadow-[0_0_20px_rgba(255,255,255,0.1)] border border-white/10"
                        : "text-slate-400 hover:bg-white/5 hover:text-white"
                    )
                  }
                  key={item.href}
                  to={item.href}
                >
                  <Icon className="h-[18px] w-[18px] transition-transform duration-300 group-hover:scale-110" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>

          {/* ── Workspace Card ──────────────────────────────────────── */}
          <Card className="mt-6 glass-light border-white/10 text-white p-4">
            <div className="flex items-center gap-2">
              <div className="h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(34,197,94,0.6)]" />
              <Badge className="border-white/10 bg-white/5 text-[10px] text-slate-300">Pro</Badge>
            </div>
            <h3 className="mt-3 font-display text-lg leading-tight">
              {currentWorkspace?.workspaceName ?? "Loading..."}
            </h3>
            <p className="mt-1.5 text-xs text-slate-400">
              {currentWorkspace?.roleName ?? "—"} access
            </p>
            {workspaces.length > 1 && (
              <div className="mt-4 space-y-1.5">
                {workspaces.map((workspace) => (
                  <button
                    className={cn(
                      "w-full rounded-xl px-3 py-2.5 text-left text-xs font-medium transition-all duration-200",
                      workspace.workspaceId === currentWorkspace?.workspaceId
                        ? "bg-white/12 text-white"
                        : "text-slate-400 hover:bg-white/6 hover:text-slate-200"
                    )}
                    key={workspace.workspaceId}
                    onClick={() => void switchWorkspace(workspace.workspaceId)}
                    type="button"
                  >
                    {workspace.workspaceName}
                  </button>
                ))}
              </div>
            )}
          </Card>
        </aside>

        {/* ── Main Content ──────────────────────────────────────────── */}
        <section className="glass rounded-[28px] p-4 shadow-[0_0_40px_rgba(14,165,233,0.1)] md:p-6 animate-fade-in" style={{ animationDelay: "0.1s" }}>
          <header className="flex flex-col gap-4 border-b border-white/10 pb-5 md:flex-row md:items-center md:justify-between">
            <div className="animate-fade-in-up">
              <p className="text-[11px] font-bold uppercase tracking-[0.3em] text-sky-400">
                Operations Console
              </p>
              <h1 className="mt-2 font-display text-3xl text-white lg:text-4xl">
                Command Center
              </h1>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center gap-2 rounded-2xl glass-light border border-white/10 px-4 py-2.5 text-sm text-slate-300 transition-all hover:border-white/20 hover:bg-white/10 cursor-pointer">
                <Search className="h-4 w-4" />
                <span className="hidden sm:inline">Search...</span>
              </div>
              <div className="relative">
                <button 
                  onClick={() => setNotificationsOpen(!notificationsOpen)}
                  className={cn(
                    "relative rounded-2xl glass-light border border-white/10 p-2.5 text-slate-300 transition-all hover:border-white/20 hover:bg-white/10 hover:text-white",
                    unreadCount > 0 && "pulse-dot"
                  )}
                >
                  <Bell className="h-4 w-4" />
                </button>

                {notificationsOpen && (
                  <div className="absolute right-0 mt-3 w-80 rounded-3xl glass-dark border border-white/10 p-4 shadow-2xl z-50">
                    <div className="flex items-center justify-between mb-3 border-b border-white/10 pb-2">
                      <h3 className="font-semibold text-white">Notifications</h3>
                      {unreadCount > 0 && <Badge className="glass-light text-sky-300">{unreadCount} New</Badge>}
                    </div>
                    
                    <div className="max-h-80 overflow-auto space-y-2">
                      {notifications?.length ? notifications.map(notif => (
                        <div key={notif.id} className={cn("p-3 rounded-2xl border transition-colors glass-light", notif.read ? "border-transparent opacity-60" : "border-sky-500/30 bg-sky-500/10")}>
                          <p className="text-sm font-semibold text-white">{notif.title}</p>
                          <p className="text-xs text-slate-300 mt-1 leading-relaxed">{notif.message}</p>
                          <div className="flex items-center justify-between mt-2 pt-2 border-t border-white/5">
                            <span className="text-[10px] text-slate-500 capitalize">In App • {new Date(notif.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                            {!notif.read && (
                              <button onClick={() => handleMarkRead(notif.id)} className="text-[10px] font-semibold text-sky-600 hover:text-sky-700">
                                Mark read
                              </button>
                            )}
                          </div>
                        </div>
                      )) : (
                        <p className="text-sm text-center text-slate-500 py-4">No notifications yet</p>
                      )}
                    </div>
                  </div>
                )}
              </div>
              <button className="flex items-center gap-2 rounded-2xl glass-dark border border-white/10 px-4 py-2.5 text-sm font-semibold text-white transition-all hover:bg-white/5 hover:scale-[1.02]">
                <div className="flex h-6 w-6 items-center justify-center rounded-lg bg-gradient-to-br from-purple-400 to-sky-600 text-[10px] font-bold">
                  {session?.user?.displayName?.charAt(0) ?? "N"}
                </div>
                <span className="hidden sm:inline">{session?.user?.displayName ?? "User"}</span>
                <ChevronDown className="h-3.5 w-3.5 opacity-60" />
              </button>
              <button
                className="group flex items-center gap-1.5 rounded-2xl glass-light border border-white/10 px-3.5 py-2.5 text-sm font-medium text-slate-300 transition-all hover:border-red-500/40 hover:bg-red-500/10 hover:text-red-400"
                onClick={() => void logout()}
                type="button"
              >
                <LogOut className="h-3.5 w-3.5 transition-transform group-hover:-translate-x-0.5" />
                <span className="hidden sm:inline">Exit</span>
              </button>
            </div>
          </header>
          <div className="pt-6">
            <Outlet />
          </div>
        </section>
      </div>
    </div>
  );
}

// ── Marketing Shell ─────────────────────────────────────────────────────────

export function MarketingShell() {
  return (
    <div className="min-h-screen bg-transparent">
      <header className="sticky top-0 z-50 glass border-b border-white/10 px-6 py-4">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          <Link className="flex items-center gap-3 transition-transform hover:scale-[1.02]" to="/">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-purple-500 to-sky-500 shadow-[0_0_15px_rgba(168,85,247,0.4)]">
              <span className="text-sm font-bold text-white">N</span>
            </div>
            <span className="font-display text-2xl font-semibold text-white">Nexora</span>
          </Link>
          <div className="flex items-center gap-3">
            <Link
              className="rounded-xl px-4 py-2.5 text-sm font-semibold text-slate-300 transition-colors hover:bg-white/10 hover:text-white"
              to="/pricing"
            >
              Pricing
            </Link>
            <Link
              className="rounded-xl glass-dark border border-white/20 px-5 py-2.5 text-sm font-semibold text-white transition-all hover:shadow-[0_0_20px_rgba(255,255,255,0.1)] hover:scale-[1.02]"
              to="/auth/login"
            >
              Sign In
            </Link>
          </div>
        </div>
      </header>
      <main className="px-6 py-12">
        <div className="mx-auto max-w-6xl">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
