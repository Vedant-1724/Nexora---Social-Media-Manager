import { Link, NavLink, Outlet } from "react-router-dom";
import {
  BarChart3,
  Bell,
  Calendar,
  ChevronDown,
  LayoutDashboard,
  LogOut,
  PenSquare,
  Search,
  Settings,
  Users
} from "lucide-react";
import { Badge, Card, cn, Button } from "@nexora/ui";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { getInAppNotifications, markNotificationRead } from "@/lib/api";

import { useAuth } from "@/features/auth/auth-context";

const navigationItems = [
  { label: "Dashboard", href: "/app/dashboard", icon: LayoutDashboard },
  { label: "Calendar", href: "/app/calendar", icon: Calendar },
  { label: "Composer", href: "/app/composer", icon: PenSquare },
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
        <aside className="glass rounded-[28px] p-5 shadow-glow-sm animate-fade-in">
          <Link className="flex items-center gap-3" to="/">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-sky-700 shadow-lg shadow-sky-500/30">
              <span className="text-lg font-bold text-white">N</span>
            </div>
            <span className="font-display text-2xl font-semibold text-slate-950">Nexora</span>
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
                        ? "bg-slate-950 text-white shadow-lg shadow-slate-950/20"
                        : "text-slate-500 hover:bg-slate-100/80 hover:text-slate-900"
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
          <Card className="mt-6 glass-dark bg-slate-950 text-white border-white/5">
            <div className="flex items-center gap-2">
              <div className="h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(34,197,94,0.6)]" />
              <Badge className="border-white/15 bg-white/8 text-[10px] text-white/80">Pro</Badge>
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
        <section className="glass rounded-[28px] p-4 shadow-glow-sm md:p-6 animate-fade-in" style={{ animationDelay: "0.1s" }}>
          <header className="flex flex-col gap-4 border-b border-slate-200/60 pb-5 md:flex-row md:items-center md:justify-between">
            <div className="animate-fade-in-up">
              <p className="text-[11px] font-bold uppercase tracking-[0.3em] text-sky-600">
                Operations Console
              </p>
              <h1 className="mt-2 font-display text-3xl text-slate-950 lg:text-4xl">
                Command Center
              </h1>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center gap-2 rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-2.5 text-sm text-slate-400 backdrop-blur transition-all hover:border-sky-200 hover:shadow-sm cursor-pointer">
                <Search className="h-4 w-4" />
                <span className="hidden sm:inline">Search...</span>
              </div>
              <div className="relative">
                <button 
                  onClick={() => setNotificationsOpen(!notificationsOpen)}
                  className={cn(
                    "relative rounded-2xl border border-slate-200/60 bg-white/60 p-2.5 text-slate-500 backdrop-blur transition-all hover:border-sky-200 hover:text-slate-700 hover:shadow-sm",
                    unreadCount > 0 && "pulse-dot"
                  )}
                >
                  <Bell className="h-4 w-4" />
                </button>

                {notificationsOpen && (
                  <div className="absolute right-0 mt-3 w-80 rounded-3xl border border-slate-200/60 bg-white/95 p-4 shadow-xl shadow-slate-200/50 backdrop-blur-xl z-50">
                    <div className="flex items-center justify-between mb-3 border-b border-slate-100 pb-2">
                      <h3 className="font-semibold text-slate-900">Notifications</h3>
                      {unreadCount > 0 && <Badge className="bg-sky-100 text-sky-700">{unreadCount} New</Badge>}
                    </div>
                    
                    <div className="max-h-80 overflow-auto space-y-2">
                      {notifications?.length ? notifications.map(notif => (
                        <div key={notif.id} className={cn("p-3 rounded-2xl border transition-colors", notif.read ? "border-transparent bg-slate-50/50" : "border-sky-100 bg-sky-50/30")}>
                          <p className="text-sm font-semibold text-slate-900">{notif.title}</p>
                          <p className="text-xs text-slate-500 mt-1 leading-relaxed">{notif.message}</p>
                          <div className="flex items-center justify-between mt-2 pt-2 border-t border-slate-100/50">
                            <span className="text-[10px] text-slate-400 capitalize">In App • {new Date(notif.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
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
              <button className="flex items-center gap-2 rounded-2xl bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-slate-950/15 transition-all hover:shadow-slate-950/25 hover:scale-[1.02]">
                <div className="flex h-6 w-6 items-center justify-center rounded-lg bg-gradient-to-br from-sky-400 to-sky-600 text-[10px] font-bold">
                  {session?.user?.displayName?.charAt(0) ?? "N"}
                </div>
                <span className="hidden sm:inline">{session?.user?.displayName ?? "User"}</span>
                <ChevronDown className="h-3.5 w-3.5 opacity-60" />
              </button>
              <button
                className="group flex items-center gap-1.5 rounded-2xl border border-slate-200/60 bg-white/60 px-3.5 py-2.5 text-sm font-medium text-slate-500 backdrop-blur transition-all hover:border-red-200 hover:bg-red-50/60 hover:text-red-600"
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
      <header className="sticky top-0 z-50 glass border-b border-white/40 px-6 py-4">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          <Link className="flex items-center gap-3 transition-transform hover:scale-[1.02]" to="/">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-sky-500 to-sky-700 shadow-lg shadow-sky-500/25">
              <span className="text-sm font-bold text-white">N</span>
            </div>
            <span className="font-display text-2xl font-semibold text-slate-950">Nexora</span>
          </Link>
          <div className="flex items-center gap-3">
            <Link
              className="rounded-xl px-4 py-2.5 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-100/60 hover:text-slate-900"
              to="/pricing"
            >
              Pricing
            </Link>
            <Link
              className="rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-slate-950/15 transition-all hover:shadow-slate-950/25 hover:scale-[1.02]"
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
