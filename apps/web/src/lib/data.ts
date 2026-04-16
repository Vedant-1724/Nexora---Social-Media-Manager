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
