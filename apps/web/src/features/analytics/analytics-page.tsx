import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardTitle, Badge, cn, Button } from "@nexora/ui";
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

import {
  timeRanges,
  dateRange,
  mockOverview,
  mockTimeSeries,
  mockTopContent,
  mockPlatformBreakdown,
  mockUTMConversions,
  colorMap,
  platformGradient,
  postTitleMap,
  formatNumber
} from "./analytics-data";

import { AnalyticsCompetitors } from "./analytics-competitors";
import { AnalyticsReports } from "./analytics-reports";
import { AnalyticsIntegrations } from "./analytics-integrations";

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

export function AnalyticsPage() {
  const { session } = useAuth();
  const accessToken = session?.session?.accessToken ?? "";
  const workspaceId = session?.currentWorkspace?.workspaceId ?? "";
  
  const [selectedRange, setSelectedRange] = useState<string>("7");
  const [activeTab, setActiveTab] = useState<"overview" | "competitors" | "reports" | "integrations">("overview");

  const range = useMemo(() => {
    const r = timeRanges.find((t) => t.key === selectedRange) ?? timeRanges[0];
    return dateRange(r.days);
  }, [selectedRange]);

  // Queries (fallback to mock data when backend unavailable)
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

  const metricConfig = [
    { key: "impressions" as const, label: "Total Impressions", changeKey: "impressionsChange" as const, icon: Eye, color: "sky", hasSplit: true, paidKey: "paidImpressions", organicKey: "organicImpressions" },
    { key: "engagements" as const, label: "Engagements", changeKey: "engagementsChange" as const, icon: Heart, color: "rose", hasSplit: false },
    { key: "reach" as const, label: "Total Reach", changeKey: "reachChange" as const, icon: Users, color: "emerald", hasSplit: true, paidKey: "paidReach", organicKey: "organicReach" },
    { key: "clicks" as const, label: "Link Clicks", changeKey: "clicksChange" as const, icon: MousePointerClick, color: "violet", hasSplit: false }
  ];

  return (
    <div className="space-y-6 animate-fade-in p-6">
      {/* ── Header ──────────────────────────────────────────────── */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="font-display text-2xl text-slate-950">Analytics & Reporting</h2>
          <p className="mt-1 text-sm text-slate-500">Advanced insights, competitor tracking, and reporting.</p>
        </div>
        <div className="flex items-center gap-2">
          {activeTab === "overview" && (
            <div className="flex rounded-2xl border border-slate-200/60 bg-white/60 p-1 backdrop-blur hidden sm:flex">
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
          )}
          <button className="flex items-center gap-1.5 rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-2.5 text-xs font-semibold text-slate-600 backdrop-blur transition-all hover:border-sky-200 hover:shadow-sm">
            <Download className="h-3.5 w-3.5" />
            Export
          </button>
        </div>
      </div>

      {/* ── Sub Navigation ─────────────────────────────────────── */}
      <div className="flex space-x-1 rounded-xl bg-slate-100 p-1">
        {[
          { id: "overview", label: "Overview" },
          { id: "competitors", label: "Competitors" },
          { id: "reports", label: "Automated Reports" },
          { id: "integrations", label: "BI Integrations" }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={cn(
              "flex-1 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200",
              activeTab === tab.id
                ? "bg-white text-slate-900 shadow-sm"
                : "text-slate-500 hover:bg-white/50 hover:text-slate-700"
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* ── Content ─────────────────────────────────────────────── */}
      <div className="mt-6">
        {activeTab === "overview" && (
          <div className="space-y-6 animate-fade-in">
            {/* Metric Cards */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 stagger">
              {metricConfig.map((metric) => {
                const Icon = metric.icon;
                const c = colorMap[metric.color];
                const value = ov[metric.key as keyof AnalyticsOverview] as number;
                const change = ov[metric.changeKey] as number;
                const isUp = typeof change === "number" ? change >= 0 : true;

                return (
                  <Card key={metric.key} className="group hover-lift glow-border relative overflow-hidden">
                    <div className="flex items-start justify-between">
                      <div className={cn("flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br text-white shadow-lg transition-transform duration-300 group-hover:scale-110", c.iconBg, c.shadow)}>
                        <Icon className="h-5 w-5" />
                      </div>
                      <div className={cn("flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-bold", isUp ? "bg-emerald-50 text-emerald-600" : "bg-rose-50 text-rose-600")}>
                        {isUp ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
                        {Math.abs(change)}%
                      </div>
                    </div>
                    <div className="mt-4">
                      <h3 className="text-sm font-semibold text-slate-500">{metric.label}</h3>
                      <p className="mt-1 font-display text-3xl font-bold tracking-tight text-slate-900">
                        {formatNumber(value)}
                      </p>
                    </div>
                    {metric.hasSplit && (
                      <div className="mt-4 flex gap-4 border-t border-slate-100 pt-3">
                        <div className="flex flex-col">
                          <span className="text-[10px] uppercase tracking-wider text-slate-400 font-bold">Paid</span>
                          <span className="text-sm font-semibold text-slate-700">{formatNumber(ov[metric.paidKey as keyof AnalyticsOverview] as number)}</span>
                        </div>
                        <div className="flex flex-col">
                          <span className="text-[10px] uppercase tracking-wider text-slate-400 font-bold">Organic</span>
                          <span className="text-sm font-semibold text-slate-700">{formatNumber(ov[metric.organicKey as keyof AnalyticsOverview] as number)}</span>
                        </div>
                      </div>
                    )}
                  </Card>
                );
              })}
            </div>

            {/* Charts & Tables */}
            <div className="grid gap-6 lg:grid-cols-3">
              <Card className="lg:col-span-2 !p-6 flex flex-col">
                <div className="mb-8 flex items-center justify-between">
                  <CardTitle>Performance Trend</CardTitle>
                  <div className="flex gap-4 text-xs font-bold text-slate-500">
                    <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-sky-400" /> Impressions</span>
                    <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-rose-400" /> Engagements</span>
                  </div>
                </div>
                <div className="flex flex-1 items-end justify-between gap-2 sm:gap-4 mt-auto">
                  {chartData.map((d, i) => (
                    <div key={d.day} className="flex flex-1 flex-col gap-2">
                      <div className="flex flex-col justify-end gap-1 flex-1 h-32">
                        <AnimatedBar value={d.impressions} maxValue={chartMax} color="bg-gradient-to-t from-sky-400 to-cyan-300 shadow-[0_0_15px_rgba(56,189,248,0.3)]" delay={i * 100} />
                        <AnimatedBar value={d.engagements} maxValue={chartMax} color="bg-gradient-to-t from-rose-400 to-pink-300 shadow-[0_0_15px_rgba(2fb,113,133,0.3)]" delay={i * 100 + 300} />
                      </div>
                      <span className="text-center text-[10px] font-bold uppercase tracking-wider text-slate-400">{d.day}</span>
                    </div>
                  ))}
                </div>
              </Card>

              <Card className="!p-6 flex flex-col">
                <CardTitle className="mb-6">Platform Breakdown</CardTitle>
                <div className="flex flex-1 flex-col justify-center gap-5">
                  {bd.map((b, i) => (
                    <div key={b.provider} className="group cursor-default" style={{ animation: `fadeInRight 0.6s ease-out ${i * 100}ms forwards`, opacity: 0 }}>
                      <div className="mb-2 flex justify-between text-sm font-bold text-slate-700">
                        <span className="capitalize">{b.provider}</span>
                        <span>{b.percentage}%</span>
                      </div>
                      <div className="relative h-2 w-full overflow-hidden rounded-full bg-slate-100">
                        <div
                          className={cn("absolute left-0 top-0 h-full rounded-full transition-all duration-1000", platformGradient[b.provider] || "bg-slate-400")}
                          style={{ width: `${b.percentage}%`, transitionDelay: `${i * 150}ms` }}
                        />
                      </div>
                      <div className="mt-1 flex justify-between text-[10px] font-semibold text-slate-400 opacity-0 transition-opacity group-hover:opacity-100">
                        <span>{formatNumber(b.impressions)} imp</span>
                        <span>{formatNumber(b.engagements)} eng</span>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </div>

            {/* UTM Conversions & Top Posts */}
            <div className="grid gap-6 lg:grid-cols-2">
              <Card className="!p-0 overflow-hidden">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center">
                  <CardTitle>Top Performing Content</CardTitle>
                  <Button variant="secondary" className="text-xs">View All</Button>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="bg-slate-50/50 text-left text-[10px] font-bold uppercase tracking-wider text-slate-400">
                        <th className="px-6 py-4">Post</th>
                        <th className="px-6 py-4 text-right">Impressions</th>
                        <th className="px-6 py-4 text-right">Eng Rate</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {tc.map((p) => (
                        <tr key={p.providerPostId} className="transition-colors hover:bg-slate-50/50">
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-3">
                              <div className={cn("flex h-8 w-8 items-center justify-center rounded-lg text-white", platformGradient[p.provider] || "bg-slate-400")}>
                                <Share2 className="h-4 w-4" />
                              </div>
                              <span className="text-sm font-semibold text-slate-700 line-clamp-1">{postTitleMap[p.providerPostId] || "Content Post"}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-right text-sm font-semibold text-slate-600">{formatNumber(p.impressions)}</td>
                          <td className="px-6 py-4 text-right">
                            <Badge className="bg-sky-50 text-sky-700">{p.engagementRate}%</Badge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Card>

              <Card className="!p-0 overflow-hidden">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center">
                  <CardTitle>UTM Campaign Conversions</CardTitle>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="bg-slate-50/50 text-left text-[10px] font-bold uppercase tracking-wider text-slate-400">
                        <th className="px-6 py-4">Campaign</th>
                        <th className="px-6 py-4 text-right">Clicks</th>
                        <th className="px-6 py-4 text-right">Revenue</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {mockUTMConversions.map((utm, i) => (
                        <tr key={i} className="transition-colors hover:bg-slate-50/50">
                          <td className="px-6 py-4">
                            <div>
                              <p className="text-sm font-semibold text-slate-700">{utm.campaign}</p>
                              <p className="text-[10px] text-slate-400 uppercase">{utm.source} / {utm.medium}</p>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-right text-sm font-semibold text-slate-600">{formatNumber(utm.clicks)}</td>
                          <td className="px-6 py-4 text-right">
                            {utm.revenue > 0 ? (
                              <Badge className="bg-emerald-50 text-emerald-700 border-emerald-200">${utm.revenue.toLocaleString()}</Badge>
                            ) : (
                              <span className="text-slate-400 text-sm font-semibold">-</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Card>
            </div>
          </div>
        )}
        
        {activeTab === "competitors" && <AnalyticsCompetitors />}
        {activeTab === "reports" && <AnalyticsReports />}
        {activeTab === "integrations" && <AnalyticsIntegrations />}
      </div>
    </div>
  );
}
