import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardTitle, Badge, cn } from "@nexora/ui";
import type { AnalyticsOverview, AccountMetric, ContentPerformance, PlatformBreakdown } from "@nexora/contracts";
import {
  ArrowUpRight,
  ArrowDownRight,
  Eye,
  Heart,
  Users,
  Share2,
  Download,
  MousePointerClick
} from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import {
  getAnalyticsOverview,
  getAnalyticsTimeSeries,
  getTopContent,
  getPlatformBreakdown
} from "@/lib/api";

// ── Time Range ──────────────────────────────────────────────────────────────

const timeRanges = [
  { key: "7", label: "7D", days: 7 },
  { key: "30", label: "30D", days: 30 },
  { key: "90", label: "90D", days: 90 }
] as const;

function dateRange(days: number) {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - days + 1);
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10)
  };
}

// ── Fallback Mock Data ──────────────────────────────────────────────────────

const mockOverview: AnalyticsOverview = {
  impressions: 1240000, reach: 920000, engagements: 58200,
  comments: 3100, clicks: 18400, followerDelta: 2847,
  engagementRate: 4.7, impressionsChange: 18.3,
  engagementsChange: 12.1, reachChange: 15.6, followerDeltaChange: 340
};

const mockTimeSeries: AccountMetric[] = [
  { date: "Mon", provider: "meta", impressions: 14500, reach: 11200, engagements: 980, comments: 52, clicks: 310, followerDelta: 18 },
  { date: "Tue", provider: "meta", impressions: 16800, reach: 13100, engagements: 1120, comments: 64, clicks: 380, followerDelta: 24 },
  { date: "Wed", provider: "meta", impressions: 19200, reach: 15400, engagements: 1340, comments: 78, clicks: 420, followerDelta: 32 },
  { date: "Thu", provider: "meta", impressions: 15600, reach: 12000, engagements: 1050, comments: 58, clicks: 350, followerDelta: 20 },
  { date: "Fri", provider: "meta", impressions: 21000, reach: 16800, engagements: 1480, comments: 88, clicks: 480, followerDelta: 38 },
  { date: "Sat", provider: "meta", impressions: 23400, reach: 18200, engagements: 1620, comments: 94, clicks: 520, followerDelta: 42 },
  { date: "Sun", provider: "meta", impressions: 18000, reach: 14100, engagements: 1200, comments: 68, clicks: 400, followerDelta: 28 }
];

const mockTopContent: ContentPerformance[] = [
  { draftId: "1", provider: "meta", providerPostId: "fb-001", impressions: 48200, likes: 2180, comments: 142, shares: 89, clicks: 1240, saves: 67, videoViews: 0, engagementRate: 6.2 },
  { draftId: "2", provider: "meta", providerPostId: "ig-001", impressions: 35800, likes: 3100, comments: 98, shares: 45, clicks: 820, saves: 234, videoViews: 28400, engagementRate: 8.1 },
  { draftId: "3", provider: "x", providerPostId: "tw-001", impressions: 22100, likes: 890, comments: 67, shares: 156, clicks: 440, saves: 0, videoViews: 0, engagementRate: 5.4 },
  { draftId: "4", provider: "linkedin", providerPostId: "li-001", impressions: 18700, likes: 620, comments: 84, shares: 42, clicks: 380, saves: 28, videoViews: 0, engagementRate: 4.8 },
  { draftId: "5", provider: "linkedin", providerPostId: "li-002", impressions: 15300, likes: 480, comments: 56, shares: 31, clicks: 290, saves: 19, videoViews: 0, engagementRate: 3.9 }
];

const mockPlatformBreakdown: PlatformBreakdown[] = [
  { provider: "meta", impressions: 468000, engagements: 24800, percentage: 38 },
  { provider: "linkedin", impressions: 347000, engagements: 16200, percentage: 28 },
  { provider: "x", impressions: 248000, engagements: 11400, percentage: 20 }
];

// ── Components ──────────────────────────────────────────────────────────────

const metricConfig = [
  { key: "impressions" as const, label: "Total Impressions", changeKey: "impressionsChange" as const, icon: Eye, color: "sky" },
  { key: "engagements" as const, label: "Engagements", changeKey: "engagementsChange" as const, icon: Heart, color: "rose" },
  { key: "followerDelta" as const, label: "New Followers", changeKey: "followerDeltaChange" as const, icon: Users, color: "emerald" },
  { key: "clicks" as const, label: "Link Clicks", changeKey: "impressionsChange" as const, icon: MousePointerClick, color: "violet" }
];

const colorMap: Record<string, { iconBg: string; shadow: string }> = {
  sky: { iconBg: "from-sky-500 to-cyan-500", shadow: "shadow-sky-500/20" },
  rose: { iconBg: "from-rose-500 to-pink-500", shadow: "shadow-rose-500/20" },
  emerald: { iconBg: "from-emerald-500 to-teal-500", shadow: "shadow-emerald-500/20" },
  violet: { iconBg: "from-violet-500 to-purple-500", shadow: "shadow-violet-500/20" }
};

const platformGradient: Record<string, string> = {
  meta: "bg-gradient-to-r from-blue-500 to-indigo-500",
  linkedin: "bg-gradient-to-r from-sky-500 to-blue-600",
  x: "bg-gradient-to-r from-slate-600 to-slate-800"
};

const postTitleMap: Record<string, string> = {
  "fb-001": "Product Launch Announcement",
  "ig-001": "Behind the Scenes Reel",
  "tw-001": "Weekly Tips Thread #42",
  "li-001": "Customer Success Story",
  "li-002": "Industry Insights Report",
  "fb-post-001": "Product Launch Announcement",
  "ig-reel-001": "Behind the Scenes Reel",
  "tweet-thread-001": "Weekly Tips Thread #42",
  "li-article-001": "Customer Success Story",
  "li-post-002": "Industry Insights Report",
  "ig-story-001": "Instagram Story Campaign",
  "tweet-poll-001": "Community Poll",
  "li-carousel-001": "Carousel: 5 Growth Tips"
};

function formatNumber(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return n.toLocaleString();
}

function AnimatedBar({ value, maxValue, color, delay }: { value: number; maxValue: number; color: string; delay: number }) {
  const height = Math.max((value / maxValue) * 100, 4);
  return (
    <div className="flex flex-col items-center gap-1.5 flex-1">
      <div className="relative h-28 w-full rounded-xl bg-slate-100/60 overflow-hidden">
        <div
          className={cn("absolute bottom-0 w-full rounded-xl transition-all duration-1000 ease-out", color)}
          style={{ height: `${height}%`, transitionDelay: `${delay}ms`, animation: `fadeInUp 0.8s ease-out ${delay}ms forwards`, opacity: 0 }}
        />
      </div>
    </div>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function AnalyticsPage() {
  const { session } = useAuth();
  const accessToken = session?.session?.accessToken ?? "";
  const workspaceId = session?.currentWorkspace?.workspaceId ?? "";
  const [selectedRange, setSelectedRange] = useState<string>("7");

  const range = useMemo(() => {
    const r = timeRanges.find((t) => t.key === selectedRange) ?? timeRanges[0];
    return dateRange(r.days);
  }, [selectedRange]);

  // ── Queries (fallback to mock data when backend unavailable) ────────────
  const { data: overview } = useQuery({
    queryKey: ["analytics-overview", workspaceId, range.from, range.to],
    queryFn: () => getAnalyticsOverview(workspaceId, accessToken, range.from, range.to),
    enabled: !!workspaceId && !!accessToken,
    placeholderData: mockOverview,
    retry: 1
  });

  const { data: timeSeries } = useQuery({
    queryKey: ["analytics-timeseries", workspaceId, range.from, range.to],
    queryFn: () => getAnalyticsTimeSeries(workspaceId, accessToken, range.from, range.to),
    enabled: !!workspaceId && !!accessToken,
    placeholderData: mockTimeSeries,
    retry: 1
  });

  const { data: topContent } = useQuery({
    queryKey: ["analytics-topcontent", workspaceId, range.from, range.to],
    queryFn: () => getTopContent(workspaceId, accessToken, range.from, range.to, 5),
    enabled: !!workspaceId && !!accessToken,
    placeholderData: mockTopContent,
    retry: 1
  });

  const { data: breakdown } = useQuery({
    queryKey: ["analytics-breakdown", workspaceId, range.from, range.to],
    queryFn: () => getPlatformBreakdown(workspaceId, accessToken, range.from, range.to),
    enabled: !!workspaceId && !!accessToken,
    placeholderData: mockPlatformBreakdown,
    retry: 1
  });

  const ov = overview ?? mockOverview;
  const ts = timeSeries ?? mockTimeSeries;
  const tc = topContent ?? mockTopContent;
  const bd = breakdown ?? mockPlatformBreakdown;

  // Aggregate time series per day for chart
  const chartData = useMemo(() => {
    const dayMap = new Map<string, { impressions: number; engagements: number }>();
    for (const m of ts) {
      const existing = dayMap.get(m.date) ?? { impressions: 0, engagements: 0 };
      dayMap.set(m.date, {
        impressions: existing.impressions + m.impressions,
        engagements: existing.engagements + m.engagements
      });
    }
    return Array.from(dayMap.entries())
      .map(([date, vals]) => ({ day: date.length > 3 ? date.slice(5) : date, ...vals }))
      .slice(-7);
  }, [ts]);

  const chartMax = Math.max(...chartData.map((d) => Math.max(d.impressions, d.engagements)), 1);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ── Header ──────────────────────────────────────────────── */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="font-display text-2xl text-slate-950">Analytics</h2>
          <p className="mt-1 text-sm text-slate-500">Performance insights across all connected platforms.</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex rounded-2xl border border-slate-200/60 bg-white/60 p-1 backdrop-blur">
            {timeRanges.map((r) => (
              <button
                key={r.key}
                className={cn(
                  "rounded-xl px-4 py-2 text-xs font-bold tracking-wider transition-all duration-200",
                  selectedRange === r.key
                    ? "bg-slate-950 text-white shadow-lg shadow-slate-950/15"
                    : "text-slate-500 hover:text-slate-700"
                )}
                onClick={() => setSelectedRange(r.key)}
                type="button"
              >
                {r.label}
              </button>
            ))}
          </div>
          <button className="flex items-center gap-1.5 rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-2.5 text-xs font-semibold text-slate-600 backdrop-blur transition-all hover:border-sky-200 hover:shadow-sm">
            <Download className="h-3.5 w-3.5" />
            Export
          </button>
        </div>
      </div>

      {/* ── Metric Cards ────────────────────────────────────────── */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 stagger">
        {metricConfig.map((metric) => {
          const Icon = metric.icon;
          const c = colorMap[metric.color];
          const value = ov[metric.key];
          const change = metric.changeKey === "followerDeltaChange"
            ? ov.followerDeltaChange
            : ov[metric.changeKey];
          const isUp = typeof change === "number" ? change >= 0 : true;

          return (
            <Card key={metric.key} className="group hover-lift glow-border">
              <div className="flex items-start justify-between">
                <div className={cn("flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br text-white shadow-lg transition-transform duration-300 group-hover:scale-110", c.iconBg, c.shadow)}>
                  <Icon className="h-5 w-5" />
                </div>
                <div className={cn("flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-bold",
                  isUp ? "bg-emerald-50 text-emerald-600" : "bg-red-50 text-red-500"
                )}>
                  {isUp ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
                  {typeof change === "number" ? `${change > 0 ? "+" : ""}${change}${metric.changeKey.includes("Change") && metric.key !== "followerDelta" ? "%" : ""}` : "—"}
                </div>
              </div>
              <p className="mt-4 font-display text-3xl font-semibold text-slate-950">{formatNumber(value)}</p>
              <p className="mt-1 text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{metric.label}</p>
            </Card>
          );
        })}
      </div>

      {/* ── Charts Section ──────────────────────────────────────── */}
      <div className="grid gap-4 lg:grid-cols-[1.5fr_1fr]">
        {/* Bar Chart */}
        <Card className="hover-lift">
          <div className="flex items-center justify-between mb-6">
            <CardTitle className="text-lg">Performance Trend</CardTitle>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-sky-500" /> Impressions</span>
              <span className="flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-emerald-500" /> Engagements</span>
            </div>
          </div>
          <div className="flex items-end gap-2">
            {chartData.map((d, i) => (
              <div key={d.day} className="flex-1 space-y-1">
                <div className="flex gap-1 h-28">
                  <AnimatedBar value={d.impressions} maxValue={chartMax} color="bg-gradient-to-t from-sky-500 to-sky-300" delay={i * 80} />
                  <AnimatedBar value={d.engagements} maxValue={chartMax} color="bg-gradient-to-t from-emerald-500 to-emerald-300" delay={i * 80 + 40} />
                </div>
                <p className="text-center text-[10px] font-bold uppercase tracking-wider text-slate-400">{d.day}</p>
              </div>
            ))}
          </div>
        </Card>

        {/* Platform Breakdown */}
        <Card className="hover-lift">
          <CardTitle className="text-lg mb-6">Platform Breakdown</CardTitle>
          <div className="space-y-5 stagger">
            {bd.map((p) => (
              <div key={p.provider}>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-semibold text-slate-700 capitalize">{p.provider}</span>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-slate-400">{formatNumber(p.impressions)} imp</span>
                    <span className="text-sm font-bold text-slate-950">{p.percentage}%</span>
                  </div>
                </div>
                <div className="h-2.5 rounded-full bg-slate-100 overflow-hidden">
                  <div
                    className={cn("h-full rounded-full transition-all duration-1000 ease-out", platformGradient[p.provider] ?? "bg-slate-400")}
                    style={{ width: `${p.percentage}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
          <div className="mt-5 pt-4 border-t border-slate-100">
            <div className="flex justify-between text-sm">
              <span className="font-semibold text-slate-700">Avg Engagement Rate</span>
              <span className="font-bold text-slate-950">{ov.engagementRate}%</span>
            </div>
          </div>
        </Card>
      </div>

      {/* ── Top Posts Table ──────────────────────────────────────── */}
      <Card className="hover-lift overflow-hidden">
        <div className="flex items-center justify-between mb-5">
          <CardTitle className="text-lg">Top Performing Content</CardTitle>
          <Badge>This Period</Badge>
        </div>
        <div className="overflow-x-auto -mx-6">
          <table className="w-full min-w-[640px]">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="px-6 py-3 text-left text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Post</th>
                <th className="px-4 py-3 text-left text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Platform</th>
                <th className="px-4 py-3 text-right text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Impressions</th>
                <th className="px-4 py-3 text-right text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Likes</th>
                <th className="px-6 py-3 text-right text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Engagement</th>
              </tr>
            </thead>
            <tbody className="stagger">
              {tc.map((post, i) => (
                <tr key={post.providerPostId} className="border-b border-slate-50 transition-colors hover:bg-sky-50/30 cursor-pointer">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-slate-100 text-xs font-bold text-slate-500">{i + 1}</span>
                      <span className="text-sm font-semibold text-slate-800">
                        {postTitleMap[post.providerPostId] ?? `Post ${post.providerPostId}`}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <span className="rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 capitalize">{post.provider}</span>
                  </td>
                  <td className="px-4 py-4 text-right text-sm font-semibold text-slate-800">{formatNumber(post.impressions)}</td>
                  <td className="px-4 py-4 text-right text-sm font-semibold text-slate-800">{formatNumber(post.likes)}</td>
                  <td className="px-6 py-4 text-right">
                    <span className="rounded-lg bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-600">{post.engagementRate}%</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
