// ── Static Data ─────────────────────────────────────────────────────────────

export const platformCards = [
  {
    name: "Meta (Facebook & Instagram)",
    detail: "Unified publishing, stories, reels, and audience analytics across both networks.",
    icon: "📱",
    gradient: "from-blue-500/10 to-purple-500/10"
  },
  {
    name: "LinkedIn",
    detail: "Company pages, article scheduling, engagement tracking, and SSO B2B flows.",
    icon: "💼",
    gradient: "from-sky-500/10 to-blue-600/10"
  },
  {
    name: "X (Twitter)",
    detail: "Thread composition, media cards, webhook-driven reply routing, and trend analytics.",
    icon: "✖️",
    gradient: "from-slate-500/10 to-slate-700/10"
  }
];

export const featureHighlights = [
  {
    title: "Visual Content Calendar",
    description: "Drag, drop, and reschedule across all connected platforms in a unified timeline view.",
    icon: "📅",
    gradient: "from-sky-500 to-cyan-500"
  },
  {
    title: "AI-Powered Scheduling",
    description: "Intelligent timing suggests optimal post windows based on your audience engagement patterns.",
    icon: "🤖",
    gradient: "from-violet-500 to-purple-500"
  },
  {
    title: "Team Approvals",
    description: "Multi-step approval workflows with role-based permissions and audit trail.",
    icon: "🛡️",
    gradient: "from-emerald-500 to-teal-500"
  },
  {
    title: "Real-time Analytics",
    description: "Cross-platform engagement metrics, audience insights, and exportable reports.",
    icon: "📊",
    gradient: "from-amber-500 to-orange-500"
  },
  {
    title: "Unified Inbox",
    description: "Comments, DMs, and mentions from all platforms in one collaborative workspace.",
    icon: "💬",
    gradient: "from-pink-500 to-rose-500"
  },
  {
    title: "Enterprise Security",
    description: "SOC 2 ready with encrypted tokens, RBAC, SSO, and complete audit logging.",
    icon: "🔒",
    gradient: "from-slate-600 to-slate-800"
  }
];

export const socialProofStats = [
  { label: "Posts Scheduled", value: "2.4M+", detail: "across all workspaces" },
  { label: "Platforms Supported", value: "6+", detail: "and growing monthly" },
  { label: "Team Members", value: "15K+", detail: "collaborating daily" },
  { label: "Uptime SLA", value: "99.9%", detail: "enterprise-grade reliability" }
];

export const pricingPlans = [
  {
    name: "Free",
    price: "₹0",
    period: "/forever",
    description: "Perfect for solo creators getting started",
    accent: "border-slate-200",
    features: [
      "1 workspace",
      "3 connected accounts",
      "30 scheduled posts/month",
      "Basic analytics",
      "Community support"
    ],
    cta: "Start Free",
    popular: false
  },
  {
    name: "Pro",
    price: "₹2,999",
    period: "/month",
    description: "For growing teams and agencies",
    accent: "border-sky-300",
    features: [
      "5 workspaces",
      "25 connected accounts",
      "Unlimited scheduled posts",
      "Advanced analytics & exports",
      "Approval workflows",
      "Priority email support",
      "API access"
    ],
    cta: "Start Pro Trial",
    popular: true
  },
  {
    name: "Enterprise",
    price: "₹8,500",
    period: "/month",
    description: "Custom solutions for large organizations",
    accent: "border-slate-800",
    features: [
      "Unlimited workspaces",
      "Unlimited accounts",
      "White-label options",
      "Custom RBAC & SSO",
      "Dedicated account manager",
      "SLA guarantee",
      "Custom integrations"
    ],
    cta: "Contact Sales",
    popular: false
  }
];

// ── Evergreen Content Demo Data ─────────────────────────────────────────────

export const evergreenCategoryColors = [
  { name: "sky", hex: "#0ea5e9", gradient: "from-sky-400 to-sky-600" },
  { name: "emerald", hex: "#10b981", gradient: "from-emerald-400 to-emerald-600" },
  { name: "amber", hex: "#f59e0b", gradient: "from-amber-400 to-amber-600" },
  { name: "rose", hex: "#f43f5e", gradient: "from-rose-400 to-rose-600" },
  { name: "violet", hex: "#8b5cf6", gradient: "from-violet-400 to-violet-600" },
  { name: "orange", hex: "#f97316", gradient: "from-orange-400 to-orange-600" },
  { name: "teal", hex: "#14b8a6", gradient: "from-teal-400 to-teal-600" },
  { name: "pink", hex: "#ec4899", gradient: "from-pink-400 to-pink-600" },
  { name: "indigo", hex: "#6366f1", gradient: "from-indigo-400 to-indigo-600" },
  { name: "slate", hex: "#64748b", gradient: "from-slate-400 to-slate-600" }
] as const;

export const demoEvergreenCategories = [
  {
    categoryId: "cat-1",
    workspaceId: "ws-1",
    name: "Blog Promotions",
    color: "sky" as const,
    icon: "📝",
    description: "Share and reshare your latest blog articles to drive traffic.",
    isEvergreen: true,
    postCount: 12,
    activePostCount: 10,
    lastPublishedAt: "2026-05-10T14:30:00Z",
    createdAt: "2026-01-15T10:00:00Z",
    updatedAt: "2026-05-10T14:30:00Z"
  },
  {
    categoryId: "cat-2",
    workspaceId: "ws-1",
    name: "Inspirational Quotes",
    color: "emerald" as const,
    icon: "💡",
    description: "Motivational and industry-relevant quotes for engagement.",
    isEvergreen: true,
    postCount: 24,
    activePostCount: 22,
    lastPublishedAt: "2026-05-10T09:00:00Z",
    createdAt: "2026-01-20T10:00:00Z",
    updatedAt: "2026-05-10T09:00:00Z"
  },
  {
    categoryId: "cat-3",
    workspaceId: "ws-1",
    name: "Industry News",
    color: "amber" as const,
    icon: "📰",
    description: "Curated news and trending topics in your industry.",
    isEvergreen: false,
    postCount: 8,
    activePostCount: 6,
    lastPublishedAt: "2026-05-09T16:00:00Z",
    createdAt: "2026-02-01T10:00:00Z",
    updatedAt: "2026-05-09T16:00:00Z"
  },
  {
    categoryId: "cat-4",
    workspaceId: "ws-1",
    name: "Product Tips",
    color: "violet" as const,
    icon: "🎯",
    description: "Quick tips and tricks to help users get the most from your product.",
    isEvergreen: true,
    postCount: 18,
    activePostCount: 16,
    lastPublishedAt: "2026-05-10T12:00:00Z",
    createdAt: "2026-02-10T10:00:00Z",
    updatedAt: "2026-05-10T12:00:00Z"
  },
  {
    categoryId: "cat-5",
    workspaceId: "ws-1",
    name: "User Stories",
    color: "rose" as const,
    icon: "❤️",
    description: "Customer testimonials and success stories.",
    isEvergreen: true,
    postCount: 6,
    activePostCount: 6,
    lastPublishedAt: "2026-05-08T11:00:00Z",
    createdAt: "2026-03-01T10:00:00Z",
    updatedAt: "2026-05-08T11:00:00Z"
  },
  {
    categoryId: "cat-6",
    workspaceId: "ws-1",
    name: "Behind the Scenes",
    color: "teal" as const,
    icon: "🎬",
    description: "Team culture, office life, and company updates.",
    isEvergreen: true,
    postCount: 9,
    activePostCount: 8,
    lastPublishedAt: "2026-05-07T15:00:00Z",
    createdAt: "2026-03-15T10:00:00Z",
    updatedAt: "2026-05-07T15:00:00Z"
  }
];

export const demoEvergreenPosts = [
  {
    postId: "post-1",
    categoryId: "cat-1",
    workspaceId: "ws-1",
    body: "🚀 New on the blog: \"10 Ways to Boost Your Social Media ROI\" — packed with actionable strategies for 2026. Link in bio!",
    status: "active" as const,
    isEvergreen: true,
    publishCount: 5,
    lastPublishedAt: "2026-05-10T14:30:00Z",
    queuePosition: 1,
    variations: [
      { variationId: "var-1a", postId: "post-1", caption: "🚀 New on the blog: \"10 Ways to Boost Your Social Media ROI\" — packed with actionable strategies for 2026. Link in bio!", mediaUrls: [], usageCount: 3, lastUsedAt: "2026-05-10T14:30:00Z", createdAt: "2026-01-15T10:00:00Z" },
      { variationId: "var-1b", postId: "post-1", caption: "Want to 10x your social ROI? Our latest blog post breaks down the top strategies working right now 📈 #SocialMediaTips", mediaUrls: [], usageCount: 2, lastUsedAt: "2026-05-03T14:30:00Z", createdAt: "2026-01-15T10:00:00Z" }
    ],
    targetProviders: ["x", "linkedin"] as const,
    createdAt: "2026-01-15T10:00:00Z",
    updatedAt: "2026-05-10T14:30:00Z"
  },
  {
    postId: "post-2",
    categoryId: "cat-1",
    workspaceId: "ws-1",
    body: "📊 Data-driven marketing is the future. Read our comprehensive guide to analytics-first content strategy.",
    status: "active" as const,
    isEvergreen: true,
    publishCount: 3,
    lastPublishedAt: "2026-05-08T10:00:00Z",
    queuePosition: 2,
    variations: [
      { variationId: "var-2a", postId: "post-2", caption: "📊 Data-driven marketing is the future. Read our comprehensive guide to analytics-first content strategy.", mediaUrls: [], usageCount: 2, lastUsedAt: "2026-05-08T10:00:00Z", createdAt: "2026-02-01T10:00:00Z" },
      { variationId: "var-2b", postId: "post-2", caption: "Still guessing what content works? Our analytics guide will change how you think about social strategy 🎯", mediaUrls: [], usageCount: 1, lastUsedAt: "2026-04-28T10:00:00Z", createdAt: "2026-02-01T10:00:00Z" },
      { variationId: "var-2c", postId: "post-2", caption: "The secret to consistent growth? Data. Dive into our latest guide on analytics-first marketing ↗️", mediaUrls: [], usageCount: 0, lastUsedAt: null, createdAt: "2026-03-10T10:00:00Z" }
    ],
    targetProviders: ["meta", "linkedin"] as const,
    createdAt: "2026-02-01T10:00:00Z",
    updatedAt: "2026-05-08T10:00:00Z"
  },
  {
    postId: "post-3",
    categoryId: "cat-2",
    workspaceId: "ws-1",
    body: "\"The best time to plant a tree was 20 years ago. The second best time is now.\" — Chinese Proverb 🌱",
    status: "active" as const,
    isEvergreen: true,
    publishCount: 8,
    lastPublishedAt: "2026-05-10T09:00:00Z",
    queuePosition: 1,
    variations: [
      { variationId: "var-3a", postId: "post-3", caption: "\"The best time to plant a tree was 20 years ago. The second best time is now.\" — Chinese Proverb 🌱", mediaUrls: [], usageCount: 4, lastUsedAt: "2026-05-10T09:00:00Z", createdAt: "2026-01-20T10:00:00Z" },
      { variationId: "var-3b", postId: "post-3", caption: "The best time to start was yesterday. The next best time? Right now. 🌱 #Motivation #GrowthMindset", mediaUrls: [], usageCount: 4, lastUsedAt: "2026-05-05T09:00:00Z", createdAt: "2026-01-20T10:00:00Z" }
    ],
    targetProviders: ["x", "meta"] as const,
    createdAt: "2026-01-20T10:00:00Z",
    updatedAt: "2026-05-10T09:00:00Z"
  },
  {
    postId: "post-4",
    categoryId: "cat-2",
    workspaceId: "ws-1",
    body: "\"Success is not final, failure is not fatal: it is the courage to continue that counts.\" — Winston Churchill ✨",
    status: "active" as const,
    isEvergreen: true,
    publishCount: 6,
    lastPublishedAt: "2026-05-09T09:00:00Z",
    queuePosition: 2,
    variations: [
      { variationId: "var-4a", postId: "post-4", caption: "\"Success is not final, failure is not fatal: it is the courage to continue that counts.\" — Winston Churchill ✨", mediaUrls: [], usageCount: 3, lastUsedAt: "2026-05-09T09:00:00Z", createdAt: "2026-01-22T10:00:00Z" },
      { variationId: "var-4b", postId: "post-4", caption: "Every setback is a setup for a comeback. Keep going. ✨ #NeverGiveUp", mediaUrls: [], usageCount: 3, lastUsedAt: "2026-05-04T09:00:00Z", createdAt: "2026-01-22T10:00:00Z" }
    ],
    targetProviders: ["x", "meta", "linkedin"] as const,
    createdAt: "2026-01-22T10:00:00Z",
    updatedAt: "2026-05-09T09:00:00Z"
  },
  {
    postId: "post-5",
    categoryId: "cat-4",
    workspaceId: "ws-1",
    body: "💡 Pro tip: Use Nexora's bulk scheduling to plan an entire week of content in under 15 minutes!",
    status: "active" as const,
    isEvergreen: true,
    publishCount: 4,
    lastPublishedAt: "2026-05-10T12:00:00Z",
    queuePosition: 1,
    variations: [
      { variationId: "var-5a", postId: "post-5", caption: "💡 Pro tip: Use Nexora's bulk scheduling to plan an entire week of content in under 15 minutes!", mediaUrls: [], usageCount: 2, lastUsedAt: "2026-05-10T12:00:00Z", createdAt: "2026-02-10T10:00:00Z" },
      { variationId: "var-5b", postId: "post-5", caption: "Save hours every week! Bulk schedule your posts and focus on what matters — creating great content 🎯", mediaUrls: [], usageCount: 2, lastUsedAt: "2026-05-05T12:00:00Z", createdAt: "2026-02-10T10:00:00Z" }
    ],
    targetProviders: ["x", "linkedin"] as const,
    createdAt: "2026-02-10T10:00:00Z",
    updatedAt: "2026-05-10T12:00:00Z"
  },
  {
    postId: "post-6",
    categoryId: "cat-1",
    workspaceId: "ws-1",
    body: "📱 Our guide to mastering Instagram Reels for business is live! Stop scrolling, start creating.",
    status: "paused" as const,
    isEvergreen: true,
    publishCount: 2,
    lastPublishedAt: "2026-04-15T14:00:00Z",
    queuePosition: 3,
    variations: [
      { variationId: "var-6a", postId: "post-6", caption: "📱 Our guide to mastering Instagram Reels for business is live! Stop scrolling, start creating.", mediaUrls: [], usageCount: 1, lastUsedAt: "2026-04-15T14:00:00Z", createdAt: "2026-03-01T10:00:00Z" },
      { variationId: "var-6b", postId: "post-6", caption: "Reels are the #1 way to grow on Instagram right now. Here's our complete business guide 🎬", mediaUrls: [], usageCount: 1, lastUsedAt: "2026-04-01T14:00:00Z", createdAt: "2026-03-01T10:00:00Z" }
    ],
    targetProviders: ["meta"] as const,
    createdAt: "2026-03-01T10:00:00Z",
    updatedAt: "2026-04-15T14:00:00Z"
  }
];

export const demoEvergreenScheduleSlots = [
  { slotId: "slot-1", workspaceId: "ws-1", categoryId: "cat-1", categoryName: "Blog Promotions", categoryColor: "sky" as const, dayOfWeek: 1, timeUtc: "09:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "10 Ways to Boost Your Social Media ROI", createdAt: "2026-01-15T10:00:00Z" },
  { slotId: "slot-2", workspaceId: "ws-1", categoryId: "cat-2", categoryName: "Inspirational Quotes", categoryColor: "emerald" as const, dayOfWeek: 1, timeUtc: "14:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "The best time to plant a tree...", createdAt: "2026-01-20T10:00:00Z" },
  { slotId: "slot-3", workspaceId: "ws-1", categoryId: "cat-4", categoryName: "Product Tips", categoryColor: "violet" as const, dayOfWeek: 2, timeUtc: "10:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "Use bulk scheduling to save time", createdAt: "2026-02-10T10:00:00Z" },
  { slotId: "slot-4", workspaceId: "ws-1", categoryId: "cat-1", categoryName: "Blog Promotions", categoryColor: "sky" as const, dayOfWeek: 3, timeUtc: "09:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "Data-driven marketing guide", createdAt: "2026-01-15T10:00:00Z" },
  { slotId: "slot-5", workspaceId: "ws-1", categoryId: "cat-5", categoryName: "User Stories", categoryColor: "rose" as const, dayOfWeek: 3, timeUtc: "16:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "Customer success story", createdAt: "2026-03-01T10:00:00Z" },
  { slotId: "slot-6", workspaceId: "ws-1", categoryId: "cat-2", categoryName: "Inspirational Quotes", categoryColor: "emerald" as const, dayOfWeek: 4, timeUtc: "09:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "Success is not final...", createdAt: "2026-01-20T10:00:00Z" },
  { slotId: "slot-7", workspaceId: "ws-1", categoryId: "cat-6", categoryName: "Behind the Scenes", categoryColor: "teal" as const, dayOfWeek: 4, timeUtc: "12:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "Meet our engineering team", createdAt: "2026-03-15T10:00:00Z" },
  { slotId: "slot-8", workspaceId: "ws-1", categoryId: "cat-3", categoryName: "Industry News", categoryColor: "amber" as const, dayOfWeek: 5, timeUtc: "10:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "AI trends in social media", createdAt: "2026-02-01T10:00:00Z" },
  { slotId: "slot-9", workspaceId: "ws-1", categoryId: "cat-4", categoryName: "Product Tips", categoryColor: "violet" as const, dayOfWeek: 5, timeUtc: "15:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "Automate your posting schedule", createdAt: "2026-02-10T10:00:00Z" },
  { slotId: "slot-10", workspaceId: "ws-1", categoryId: "cat-1", categoryName: "Blog Promotions", categoryColor: "sky" as const, dayOfWeek: 6, timeUtc: "11:00", timezone: "Asia/Kolkata", isActive: true, nextPostTitle: "Instagram Reels guide", createdAt: "2026-01-15T10:00:00Z" }
];
