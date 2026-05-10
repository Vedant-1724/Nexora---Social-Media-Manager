import type { 
  AnalyticsOverview, 
  AccountMetric, 
  ContentPerformance, 
  PlatformBreakdown,
  AnalyticsUTMConversion,
  AnalyticsCompetitor,
  AnalyticsCompetitorBenchmark,
  AnalyticsReportTemplate,
  AnalyticsBIIntegration
} from "@nexora/contracts";

export const timeRanges = [
  { key: "7", label: "7D", days: 7 },
  { key: "30", label: "30D", days: 30 },
  { key: "90", label: "90D", days: 90 }
] as const;

export function dateRange(days: number) {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - days + 1);
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10)
  };
}

export const mockOverview: AnalyticsOverview = {
  impressions: 1240000,
  paidImpressions: 440000,
  organicImpressions: 800000,
  reach: 920000,
  paidReach: 320000,
  organicReach: 600000,
  engagements: 58200,
  comments: 3100,
  clicks: 18400,
  followerDelta: 2847,
  engagementRate: 4.7,
  impressionsChange: 18.3,
  engagementsChange: 12.1,
  reachChange: 15.6,
  clicksChange: 9.8,
  followerDeltaChange: 340
};

export const mockTimeSeries: AccountMetric[] = [
  { date: "Mon", provider: "meta", impressions: 14500, reach: 11200, engagements: 980, comments: 52, clicks: 310, followerDelta: 18 },
  { date: "Tue", provider: "meta", impressions: 16800, reach: 13100, engagements: 1120, comments: 64, clicks: 380, followerDelta: 24 },
  { date: "Wed", provider: "meta", impressions: 19200, reach: 15400, engagements: 1340, comments: 78, clicks: 420, followerDelta: 32 },
  { date: "Thu", provider: "meta", impressions: 15600, reach: 12000, engagements: 1050, comments: 58, clicks: 350, followerDelta: 20 },
  { date: "Fri", provider: "meta", impressions: 21000, reach: 16800, engagements: 1480, comments: 88, clicks: 480, followerDelta: 38 },
  { date: "Sat", provider: "meta", impressions: 23400, reach: 18200, engagements: 1620, comments: 94, clicks: 520, followerDelta: 42 },
  { date: "Sun", provider: "meta", impressions: 18000, reach: 14100, engagements: 1200, comments: 68, clicks: 400, followerDelta: 28 }
];

export const mockTopContent: ContentPerformance[] = [
  { draftId: "1", provider: "meta", providerPostId: "fb-001", impressions: 48200, likes: 2180, comments: 142, shares: 89, clicks: 1240, saves: 67, videoViews: 0, engagementRate: 6.2 },
  { draftId: "2", provider: "meta", providerPostId: "ig-001", impressions: 35800, likes: 3100, comments: 98, shares: 45, clicks: 820, saves: 234, videoViews: 28400, engagementRate: 8.1 },
  { draftId: "3", provider: "x", providerPostId: "tw-001", impressions: 22100, likes: 890, comments: 67, shares: 156, clicks: 440, saves: 0, videoViews: 0, engagementRate: 5.4 },
  { draftId: "4", provider: "linkedin", providerPostId: "li-001", impressions: 18700, likes: 620, comments: 84, shares: 42, clicks: 380, saves: 28, videoViews: 0, engagementRate: 4.8 },
  { draftId: "5", provider: "linkedin", providerPostId: "li-002", impressions: 15300, likes: 480, comments: 56, shares: 31, clicks: 290, saves: 19, videoViews: 0, engagementRate: 3.9 }
];

export const mockPlatformBreakdown: PlatformBreakdown[] = [
  { provider: "meta", impressions: 468000, engagements: 24800, percentage: 38 },
  { provider: "linkedin", impressions: 347000, engagements: 16200, percentage: 28 },
  { provider: "x", impressions: 248000, engagements: 11400, percentage: 20 },
  { provider: "instagram", impressions: 177000, engagements: 19200, percentage: 14 }
];

export const mockUTMConversions: AnalyticsUTMConversion[] = [
  { campaign: "spring_sale_2026", source: "instagram", medium: "social", clicks: 3200, conversions: 184, revenue: 12450.00 },
  { campaign: "spring_sale_2026", source: "facebook", medium: "social_paid", clicks: 5400, conversions: 210, revenue: 15200.00 },
  { campaign: "newsletter_signup", source: "x", medium: "social", clicks: 1200, conversions: 89, revenue: 0 },
  { campaign: "webinar_q2", source: "linkedin", medium: "social", clicks: 840, conversions: 124, revenue: 0 },
  { campaign: "product_launch", source: "instagram", medium: "social", clicks: 4100, conversions: 310, revenue: 24800.00 }
];

export const mockCompetitors: AnalyticsCompetitor[] = [
  { competitorId: "comp-1", workspaceId: "ws-1", name: "RivalBrands Inc", handle: "@rivalbrands", provider: "instagram", avatarUrl: null, trackedSince: "2025-01-01T00:00:00Z" },
  { competitorId: "comp-2", workspaceId: "ws-1", name: "Alpha Marketing", handle: "@alphamarketing", provider: "x", avatarUrl: null, trackedSince: "2025-03-15T00:00:00Z" },
  { competitorId: "comp-3", workspaceId: "ws-1", name: "Global Reach", handle: "@globalreach", provider: "linkedin", avatarUrl: null, trackedSince: "2025-06-10T00:00:00Z" }
];

export const mockCompetitorBenchmarks: AnalyticsCompetitorBenchmark[] = [
  { competitorId: "comp-1", date: "2026-05-10", followers: 45000, followerGrowth: 1.2, engagements: 12400, postsPublished: 14, shareOfVoicePercentage: 28 },
  { competitorId: "comp-2", date: "2026-05-10", followers: 82000, followerGrowth: 0.8, engagements: 18200, postsPublished: 8, shareOfVoicePercentage: 42 },
  { competitorId: "comp-3", date: "2026-05-10", followers: 15000, followerGrowth: 3.5, engagements: 5100, postsPublished: 22, shareOfVoicePercentage: 12 },
  { competitorId: "self", date: "2026-05-10", followers: 32000, followerGrowth: 2.1, engagements: 9800, postsPublished: 12, shareOfVoicePercentage: 18 } // "self" representing the user's workspace
];

export const mockReportTemplates: AnalyticsReportTemplate[] = [
  {
    templateId: "rt-1",
    workspaceId: "ws-1",
    name: "Monthly Client Performance Report",
    description: "Standard monthly breakdown of organic and paid performance across all channels.",
    agencyLogoUrl: null,
    modules: [
      { moduleId: "mod-1", type: "overview", title: "Executive Summary", position: 0, config: {} },
      { moduleId: "mod-2", type: "platform_breakdown", title: "Channel Performance", position: 1, config: {} },
      { moduleId: "mod-3", type: "top_posts", title: "Top Performing Content", position: 2, config: {} },
      { moduleId: "mod-4", type: "utm_conversions", title: "Campaign Conversions", position: 3, config: {} }
    ],
    createdAt: "2026-01-15T00:00:00Z",
    updatedAt: "2026-04-20T00:00:00Z"
  },
  {
    templateId: "rt-2",
    workspaceId: "ws-1",
    name: "Competitor Analysis Report",
    description: "Quarterly benchmark against top 3 competitors.",
    agencyLogoUrl: null,
    modules: [
      { moduleId: "mod-5", type: "competitors", title: "Share of Voice", position: 0, config: {} }
    ],
    createdAt: "2026-02-10T00:00:00Z",
    updatedAt: "2026-02-10T00:00:00Z"
  }
];

export const mockBIIntegrations: AnalyticsBIIntegration[] = [
  {
    integrationId: "bi-1",
    workspaceId: "ws-1",
    provider: "looker_studio",
    status: "active",
    connectionKey: "nx_ls_9f8d7c6b5a412345",
    dataPipedCount: 142500,
    lastSyncAt: "2026-05-10T08:00:00Z",
    createdAt: "2025-11-01T00:00:00Z"
  },
  {
    integrationId: "bi-2",
    workspaceId: "ws-1",
    provider: "tableau",
    status: "inactive",
    connectionKey: null,
    dataPipedCount: 0,
    lastSyncAt: null,
    createdAt: "2026-04-01T00:00:00Z"
  }
];

export const colorMap: Record<string, { iconBg: string; shadow: string }> = {
  sky: { iconBg: "from-sky-500 to-cyan-500", shadow: "shadow-sky-500/20" },
  rose: { iconBg: "from-rose-500 to-pink-500", shadow: "shadow-rose-500/20" },
  emerald: { iconBg: "from-emerald-500 to-teal-500", shadow: "shadow-emerald-500/20" },
  violet: { iconBg: "from-violet-500 to-purple-500", shadow: "shadow-violet-500/20" },
  amber: { iconBg: "from-amber-500 to-yellow-500", shadow: "shadow-amber-500/20" },
  indigo: { iconBg: "from-indigo-500 to-blue-600", shadow: "shadow-indigo-500/20" }
};

export const platformGradient: Record<string, string> = {
  meta: "bg-gradient-to-r from-blue-500 to-indigo-500",
  linkedin: "bg-gradient-to-r from-sky-500 to-blue-600",
  x: "bg-gradient-to-r from-slate-600 to-slate-800",
  instagram: "bg-gradient-to-r from-pink-500 to-purple-500"
};

export const postTitleMap: Record<string, string> = {
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

export function formatNumber(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return n.toLocaleString();
}
