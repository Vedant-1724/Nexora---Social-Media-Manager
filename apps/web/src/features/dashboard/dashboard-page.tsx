import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardTitle, Badge, cn } from "@nexora/ui";
import { ArrowUpRight, ArrowDownRight, Calendar, Users, Radio, TrendingUp, Clock, Sparkles } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { getAnalyticsOverview } from "@/lib/api";

function formatNumber(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return n.toLocaleString();
}

const activityFeed = [
  { user: "Priya S.", action: "published a post", channel: "Instagram", time: "2m ago", avatar: "PS" },
  { user: "Arjun M.", action: "approved draft", channel: "LinkedIn", time: "15m ago", avatar: "AM" },
  { user: "Sara K.", action: "scheduled 3 posts", channel: "X (Twitter)", time: "1h ago", avatar: "SK" },
  { user: "Dev R.", action: "connected account", channel: "Facebook", time: "3h ago", avatar: "DR" },
  { user: "Meera J.", action: "exported report", channel: "Analytics", time: "5h ago", avatar: "MJ" }
];

const upcomingPosts = [
  { title: "Product Launch Announcement", time: "Today 3:00 PM", platforms: ["meta", "linkedin"], status: "scheduled" },
  { title: "Weekly Tips Thread", time: "Today 6:30 PM", platforms: ["x"], status: "approved" },
  { title: "Team Spotlight Feature", time: "Tomorrow 10:00 AM", platforms: ["meta", "linkedin", "x"], status: "pending" },
  { title: "Customer Success Story", time: "Tomorrow 2:00 PM", platforms: ["linkedin"], status: "draft" }
];

const miniSparkline = [35, 48, 42, 65, 58, 72, 68, 85, 78, 92, 88, 95];

function SparklineChart({ data, color }: { data: number[]; color: string }) {
  const max = Math.max(...data);
  const min = Math.min(...data);
  const range = max - min || 1;
  const w = 120;
  const h = 32;
  const points = data
    .map((v, i) => `${(i / (data.length - 1)) * w},${h - ((v - min) / range) * h}`)
    .join(" ");

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="w-full h-8 mt-2" preserveAspectRatio="none">
      <defs>
        <linearGradient id={`grad-${color}`} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={color} stopOpacity="0.3" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <polyline
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        points={points}
      />
      <polygon
        fill={`url(#grad-${color})`}
        points={`0,${h} ${points} ${w},${h}`}
      />
    </svg>
  );
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    scheduled: "bg-sky-100 text-sky-700 border-sky-200",
    approved: "bg-emerald-100 text-emerald-700 border-emerald-200",
    pending: "bg-amber-100 text-amber-700 border-amber-200",
    draft: "bg-slate-100 text-slate-600 border-slate-200"
  };

  return (
    <span className={cn("inline-flex items-center rounded-lg border px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider", styles[status] ?? styles.draft)}>
      {status}
    </span>
  );
}

export function DashboardPage() {
  const { session } = useAuth();
  const accessToken = session?.session?.accessToken ?? "";
  const workspaceId = session?.currentWorkspace?.workspaceId ?? "";

  const range = useMemo(() => {
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 7 + 1);
    return {
      from: from.toISOString().slice(0, 10),
      to: to.toISOString().slice(0, 10)
    };
  }, []);

  const { data: overview } = useQuery({
    queryKey: ["analytics-overview", workspaceId, range.from, range.to],
    queryFn: () => getAnalyticsOverview(workspaceId, accessToken, range.from, range.to),
    enabled: !!workspaceId && !!accessToken,
    placeholderData: {
      impressions: 1240000, reach: 84200, engagements: 58200,
      comments: 3100, clicks: 18400, followerDelta: 2847,
      engagementRate: 4.7, impressionsChange: 18.3,
      engagementsChange: 12.1, reachChange: 15.6, followerDeltaChange: 340
    },
    retry: 1
  });

  const dashboardStats = useMemo(() => {
    const ov = overview;
    if (!ov) return [];

    return [
      {
        label: "Scheduled Posts",
        value: "128",
        change: "+14%",
        trend: "up" as const,
        icon: Calendar,
        gradient: "from-sky-500 to-cyan-500",
        shadowColor: "shadow-sky-500/20"
      },
      {
        label: "Weekly Engagements",
        value: formatNumber(ov.engagements),
        change: `${ov.engagementsChange >= 0 ? "+" : ""}${ov.engagementsChange}%`,
        trend: ov.engagementsChange >= 0 ? "up" as const : "down" as const,
        icon: Clock,
        gradient: "from-amber-500 to-orange-500",
        shadowColor: "shadow-amber-500/20"
      },
      {
        label: "New Followers",
        value: formatNumber(ov.followerDelta),
        change: `${ov.followerDeltaChange >= 0 ? "+" : ""}${ov.followerDeltaChange}`,
        trend: ov.followerDeltaChange >= 0 ? "up" as const : "down" as const,
        icon: Users,
        gradient: "from-emerald-500 to-teal-500",
        shadowColor: "shadow-emerald-500/20"
      },
      {
        label: "Weekly Reach",
        value: formatNumber(ov.reach),
        change: `${ov.reachChange >= 0 ? "+" : ""}${ov.reachChange}%`,
        trend: ov.reachChange >= 0 ? "up" as const : "down" as const,
        icon: TrendingUp,
        gradient: "from-violet-500 to-purple-500",
        shadowColor: "shadow-violet-500/20"
      }
    ];
  }, [overview]);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ── Welcome ─────────────────────────────────────────────── */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="font-display text-2xl text-slate-950">
            Welcome back, {session?.user?.displayName?.split(" ")[0] ?? "there"} 👋
          </h2>
          <p className="mt-1 text-sm text-slate-500">Here's what's happening across your workspace.</p>
        </div>
        <Badge className="hidden sm:inline-flex">
          <Sparkles className="mr-1.5 h-3 w-3" />
          Live Data
        </Badge>
      </div>

      {/* ── Stat Cards ──────────────────────────────────────────── */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 stagger">
        {dashboardStats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card key={stat.label} className="group hover-lift glow-border relative overflow-hidden">
              <div className="flex items-start justify-between">
                <div className={`flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br ${stat.gradient} text-white shadow-lg ${stat.shadowColor} transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3`}>
                  <Icon className="h-5 w-5" />
                </div>
                <div className={cn(
                  "flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-bold",
                  stat.trend === "up"
                    ? "bg-emerald-50 text-emerald-600"
                    : "bg-red-50 text-red-500"
                )}>
                  {stat.trend === "up" ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
                  {stat.change}
                </div>
              </div>
              <p className="mt-4 font-display text-3xl font-semibold text-slate-950 stat-number">{stat.value}</p>
              <p className="mt-1 text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{stat.label}</p>
              <SparklineChart data={miniSparkline} color={stat.trend === "up" ? "#0ea5e9" : "#ef4444"} />
            </Card>
          );
        })}
      </div>

      {/* ── Content Grid ────────────────────────────────────────── */}
      <div className="grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        {/* ── Upcoming Posts ────────────────────────────────────── */}
        <Card className="hover-lift">
          <div className="flex items-center justify-between mb-5">
            <CardTitle className="text-lg">Upcoming Posts</CardTitle>
            <Badge>Today + Tomorrow</Badge>
          </div>
          <div className="space-y-3 stagger">
            {upcomingPosts.map((post) => (
              <div
                key={post.title}
                className="group flex items-center justify-between rounded-2xl border border-slate-100 bg-slate-50/50 px-4 py-3.5 transition-all hover:border-sky-200 hover:bg-sky-50/30 hover:shadow-sm cursor-pointer"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-slate-800 group-hover:text-slate-950">{post.title}</p>
                  <div className="mt-1.5 flex items-center gap-2">
                    <span className="text-xs text-slate-400">{post.time}</span>
                    <span className="text-slate-300">·</span>
                    <div className="flex gap-1">
                      {post.platforms.map((p) => (
                        <span key={p} className="rounded bg-slate-200/70 px-1.5 py-0.5 text-[10px] font-medium text-slate-500">{p}</span>
                      ))}
                    </div>
                  </div>
                </div>
                <StatusBadge status={post.status} />
              </div>
            ))}
          </div>
        </Card>

        {/* ── Activity Feed ─────────────────────────────────────── */}
        <Card className="hover-lift">
          <div className="flex items-center justify-between mb-5">
            <CardTitle className="text-lg">Activity</CardTitle>
            <div className="flex items-center gap-1.5">
              <div className="h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_6px_rgba(34,197,94,0.5)]" />
              <span className="text-xs font-semibold text-emerald-600">Live</span>
            </div>
          </div>
          <div className="space-y-3 stagger">
            {activityFeed.map((item) => (
              <div
                key={`${item.user}-${item.time}`}
                className="flex items-center gap-3 rounded-2xl px-3 py-2.5 transition-colors hover:bg-slate-50/80"
              >
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-sky-100 to-sky-200 text-[11px] font-bold text-sky-700">
                  {item.avatar}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm text-slate-700">
                    <span className="font-semibold text-slate-900">{item.user}</span>{" "}
                    {item.action}
                  </p>
                  <p className="mt-0.5 text-xs text-slate-400">{item.channel} · {item.time}</p>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
