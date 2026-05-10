// ── Smart Inbox Mock Data ───────────────────────────────────────────────────
import type { InboxMessage, InboxTag, InboxModerationRule, InboxBotFlow, InboxReview } from "@nexora/contracts";

export const inboxTags: InboxTag[] = [
  { tagId: "t1", workspaceId: "w1", label: "Bug Report", color: "red", createdAt: "2025-04-01T00:00:00Z" },
  { tagId: "t2", workspaceId: "w1", label: "Sales Lead", color: "emerald", createdAt: "2025-04-01T00:00:00Z" },
  { tagId: "t3", workspaceId: "w1", label: "Support", color: "sky", createdAt: "2025-04-01T00:00:00Z" },
  { tagId: "t4", workspaceId: "w1", label: "VIP", color: "amber", createdAt: "2025-04-01T00:00:00Z" },
  { tagId: "t5", workspaceId: "w1", label: "Feedback", color: "violet", createdAt: "2025-04-01T00:00:00Z" },
  { tagId: "t6", workspaceId: "w1", label: "Urgent", color: "rose", createdAt: "2025-04-01T00:00:00Z" },
];

export const mockMessages: InboxMessage[] = [
  {
    messageId: "m1", workspaceId: "w1", conversationId: "c1", type: "dm", provider: "instagram",
    sender: { senderId: "s1", displayName: "Emma Rodriguez", username: "@emmarod", avatarUrl: null, provider: "instagram", followerCount: 14200, isVerified: false },
    subject: null, body: "Hey! I love your new product line. Can I get a discount code for my followers? I have 14K on Instagram and would love to collaborate 💕", bodyHtml: null, mediaUrls: [],
    externalPostUrl: null, parentExternalPostId: null, status: "unread", sentiment: "positive",
    tags: [inboxTags[1], inboxTags[3]], assignedToUserId: null, assignedToName: null,
    collisionLock: null, internalNotes: [{ noteId: "n1", messageId: "m1", authorUserId: "u1", authorName: "Priya S.", body: "Potential micro-influencer — check engagement rate before responding.", createdAt: "2025-05-10T09:30:00Z" }],
    reviewRating: null, reviewSource: null, aiSuggestedReply: "Hi Emma! Thanks for reaching out 😊 We'd love to explore a collaboration. Could you share your media kit so we can review your audience demographics?",
    createdAt: "2025-05-11T08:15:00Z", updatedAt: "2025-05-11T08:15:00Z"
  },
  {
    messageId: "m2", workspaceId: "w1", conversationId: "c2", type: "comment", provider: "meta",
    sender: { senderId: "s2", displayName: "Jake Thompson", username: "jake.t", avatarUrl: null, provider: "meta", followerCount: 340, isVerified: false },
    subject: null, body: "This product broke after 2 days of use. Terrible quality. I want a refund immediately!", bodyHtml: null, mediaUrls: [],
    externalPostUrl: "https://facebook.com/post/123", parentExternalPostId: "ext_post_1", status: "unread", sentiment: "negative",
    tags: [inboxTags[0], inboxTags[5]], assignedToUserId: "u2", assignedToName: "Arjun M.",
    collisionLock: { messageId: "m2", lockedByUserId: "u2", lockedByName: "Arjun M.", lockedAt: "2025-05-11T08:20:00Z", expiresAt: "2025-05-11T08:25:00Z" },
    internalNotes: [], reviewRating: null, reviewSource: null,
    aiSuggestedReply: "Hi Jake, we're really sorry about your experience. We'd like to make this right — could you DM us your order number so we can process a replacement?",
    createdAt: "2025-05-11T07:45:00Z", updatedAt: "2025-05-11T08:20:00Z"
  },
  {
    messageId: "m3", workspaceId: "w1", conversationId: "c3", type: "mention", provider: "x",
    sender: { senderId: "s3", displayName: "TechReview Daily", username: "@techreviewdaily", avatarUrl: null, provider: "x", followerCount: 89000, isVerified: true },
    subject: null, body: "Just tested @nexora's new scheduling feature. Game changer for social media teams! Full review coming this week 🧵", bodyHtml: null, mediaUrls: [],
    externalPostUrl: "https://x.com/techreviewdaily/status/123", parentExternalPostId: null, status: "read", sentiment: "positive",
    tags: [inboxTags[3]], assignedToUserId: "u1", assignedToName: "Priya S.",
    collisionLock: null, internalNotes: [], reviewRating: null, reviewSource: null, aiSuggestedReply: null,
    createdAt: "2025-05-11T06:30:00Z", updatedAt: "2025-05-11T07:00:00Z"
  },
  {
    messageId: "m4", workspaceId: "w1", conversationId: "c4", type: "dm", provider: "linkedin",
    sender: { senderId: "s4", displayName: "Sarah Chen", username: "sarah-chen-cmo", avatarUrl: null, provider: "linkedin", followerCount: 5200, isVerified: false },
    subject: "Enterprise Inquiry", body: "Hi team, I'm the CMO at Vertex Inc and we're evaluating social media management platforms for our 50-person marketing team. Can we schedule a demo?", bodyHtml: null, mediaUrls: [],
    externalPostUrl: null, parentExternalPostId: null, status: "unread", sentiment: "positive",
    tags: [inboxTags[1], inboxTags[3]], assignedToUserId: null, assignedToName: null,
    collisionLock: null, internalNotes: [], reviewRating: null, reviewSource: null,
    aiSuggestedReply: "Hi Sarah! Thank you for your interest in Nexora. We'd be happy to arrange a personalized demo for your team. Let me connect you with our enterprise solutions team.",
    createdAt: "2025-05-11T05:00:00Z", updatedAt: "2025-05-11T05:00:00Z"
  },
  {
    messageId: "m5", workspaceId: "w1", conversationId: "c5", type: "comment", provider: "instagram",
    sender: { senderId: "s5", displayName: "Maria Lopez", username: "@marialopez", avatarUrl: null, provider: "instagram", followerCount: 1200, isVerified: false },
    subject: null, body: "How do I change my subscription plan? I've been trying for an hour and can't find the setting 😤", bodyHtml: null, mediaUrls: [],
    externalPostUrl: null, parentExternalPostId: "ext_post_5", status: "unread", sentiment: "negative",
    tags: [inboxTags[2]], assignedToUserId: "u3", assignedToName: "Sara K.",
    collisionLock: null, internalNotes: [], reviewRating: null, reviewSource: null,
    aiSuggestedReply: "Hi Maria! You can change your plan by going to Settings → Billing → Change Plan. Let us know if you need any help!",
    createdAt: "2025-05-11T04:20:00Z", updatedAt: "2025-05-11T04:20:00Z"
  },
  {
    messageId: "m6", workspaceId: "w1", conversationId: "c6", type: "review", provider: "google_my_business",
    sender: { senderId: "s6", displayName: "Alex Wright", username: null, avatarUrl: null, provider: "google_my_business", followerCount: null, isVerified: false },
    subject: "Great tool but needs work", body: "Nexora has been amazing for scheduling but the analytics dashboard could use more export options. Also, mobile app when?", bodyHtml: null, mediaUrls: [],
    externalPostUrl: null, parentExternalPostId: null, status: "read", sentiment: "neutral",
    tags: [inboxTags[4]], assignedToUserId: null, assignedToName: null,
    collisionLock: null, internalNotes: [], reviewRating: 4, reviewSource: "google_my_business", aiSuggestedReply: null,
    createdAt: "2025-05-10T18:00:00Z", updatedAt: "2025-05-10T18:00:00Z"
  },
  {
    messageId: "m7", workspaceId: "w1", conversationId: "c7", type: "dm", provider: "x",
    sender: { senderId: "s7", displayName: "Dev Community Hub", username: "@devcommhub", avatarUrl: null, provider: "x", followerCount: 42000, isVerified: true },
    subject: null, body: "We'd like to feature Nexora in our next 'Tools of the Week' newsletter. Could you provide some key stats and a quote from your CEO?", bodyHtml: null, mediaUrls: [],
    externalPostUrl: null, parentExternalPostId: null, status: "replied", sentiment: "positive",
    tags: [inboxTags[3]], assignedToUserId: "u1", assignedToName: "Priya S.",
    collisionLock: null, internalNotes: [], reviewRating: null, reviewSource: null, aiSuggestedReply: null,
    createdAt: "2025-05-10T14:00:00Z", updatedAt: "2025-05-10T16:00:00Z"
  },
  {
    messageId: "m8", workspaceId: "w1", conversationId: "c8", type: "review", provider: "apple_app_store",
    sender: { senderId: "s8", displayName: "ContentKing99", username: null, avatarUrl: null, provider: "apple_app_store", followerCount: null, isVerified: false },
    subject: "Crashes on startup", body: "App crashes every time I try to open the composer. iPhone 15 Pro, iOS 18. Please fix ASAP.", bodyHtml: null, mediaUrls: [],
    externalPostUrl: null, parentExternalPostId: null, status: "unread", sentiment: "negative",
    tags: [inboxTags[0], inboxTags[5]], assignedToUserId: null, assignedToName: null,
    collisionLock: null, internalNotes: [], reviewRating: 1, reviewSource: "apple_app_store", aiSuggestedReply: null,
    createdAt: "2025-05-10T12:30:00Z", updatedAt: "2025-05-10T12:30:00Z"
  },
  {
    messageId: "m9", workspaceId: "w1", conversationId: "c9", type: "mention", provider: "linkedin",
    sender: { senderId: "s9", displayName: "Digital Marketing Pro", username: "dmp-official", avatarUrl: null, provider: "linkedin", followerCount: 28000, isVerified: false },
    subject: null, body: "Our top 5 social media tools for 2025: 1. Nexora 2. Buffer 3. Hootsuite... Read more in our latest article!", bodyHtml: null, mediaUrls: [],
    externalPostUrl: "https://linkedin.com/post/456", parentExternalPostId: null, status: "read", sentiment: "positive",
    tags: [], assignedToUserId: null, assignedToName: null,
    collisionLock: null, internalNotes: [], reviewRating: null, reviewSource: null, aiSuggestedReply: null,
    createdAt: "2025-05-10T10:15:00Z", updatedAt: "2025-05-10T11:00:00Z"
  },
  {
    messageId: "m10", workspaceId: "w1", conversationId: "c10", type: "review", provider: "yelp",
    sender: { senderId: "s10", displayName: "SmallBizOwner", username: null, avatarUrl: null, provider: "yelp", followerCount: null, isVerified: false },
    subject: "Life saver for my bakery", body: "Nexora helped me manage all my social accounts from one place. My bakery's Instagram grew 300% in 3 months!", bodyHtml: null, mediaUrls: [],
    externalPostUrl: null, parentExternalPostId: null, status: "read", sentiment: "positive",
    tags: [inboxTags[4]], assignedToUserId: null, assignedToName: null,
    collisionLock: null, internalNotes: [], reviewRating: 5, reviewSource: "yelp", aiSuggestedReply: null,
    createdAt: "2025-05-09T20:00:00Z", updatedAt: "2025-05-09T20:00:00Z"
  }
];

export const mockModerationRules: InboxModerationRule[] = [
  { ruleId: "mr1", workspaceId: "w1", name: "Block Profanity", description: "Automatically hide comments containing profanity", trigger: "profanity", triggerValue: "auto", action: "hide", actionValue: null, isActive: true, matchCount: 142, createdAt: "2025-03-01T00:00:00Z", updatedAt: "2025-05-10T00:00:00Z" },
  { ruleId: "mr2", workspaceId: "w1", name: "Flag Negative Sentiment", description: "Flag messages with strong negative sentiment for review", trigger: "sentiment", triggerValue: "negative", action: "flag", actionValue: null, isActive: true, matchCount: 67, createdAt: "2025-03-15T00:00:00Z", updatedAt: "2025-05-08T00:00:00Z" },
  { ruleId: "mr3", workspaceId: "w1", name: "Spam Filter", description: "Hide messages with high spam score", trigger: "spam_score", triggerValue: "0.85", action: "hide", actionValue: null, isActive: true, matchCount: 328, createdAt: "2025-02-01T00:00:00Z", updatedAt: "2025-05-10T00:00:00Z" },
  { ruleId: "mr4", workspaceId: "w1", name: "Auto-Reply FAQ", description: "Send auto-reply for common keyword questions", trigger: "keyword", triggerValue: "pricing,plans,cost,how much", action: "auto_reply", actionValue: "Thanks for your interest! Check out our pricing at nexora.io/pricing or DM us for a custom quote.", isActive: false, matchCount: 23, createdAt: "2025-04-01T00:00:00Z", updatedAt: "2025-04-15T00:00:00Z" },
];

export const mockBotFlows: InboxBotFlow[] = [
  {
    flowId: "bf1", workspaceId: "w1", name: "Welcome New Followers", description: "Sends a welcome DM to new followers", isActive: true,
    nodes: [
      { nodeId: "n1", type: "trigger", label: "New Follower", config: { event: "new_follower" }, position: { x: 200, y: 40 }, nextNodeIds: ["n2"] },
      { nodeId: "n2", type: "delay", label: "Wait 5 min", config: { minutes: 5 }, position: { x: 200, y: 140 }, nextNodeIds: ["n3"] },
      { nodeId: "n3", type: "reply", label: "Send Welcome", config: { message: "Hey! Thanks for following us 🎉 Let us know if you have any questions!" }, position: { x: 200, y: 240 }, nextNodeIds: [] },
    ],
    triggerCount: 1240, lastTriggeredAt: "2025-05-11T07:00:00Z", createdAt: "2025-03-01T00:00:00Z", updatedAt: "2025-05-10T00:00:00Z"
  },
  {
    flowId: "bf2", workspaceId: "w1", name: "Support Ticket Router", description: "Routes support messages to the right team member", isActive: true,
    nodes: [
      { nodeId: "n1", type: "trigger", label: "Keyword: help, support, issue", config: { keywords: ["help", "support", "issue"] }, position: { x: 200, y: 40 }, nextNodeIds: ["n2"] },
      { nodeId: "n2", type: "condition", label: "Check Sentiment", config: { field: "sentiment", operator: "equals", value: "negative" }, position: { x: 200, y: 140 }, nextNodeIds: ["n3", "n4"] },
      { nodeId: "n3", type: "action", label: "Assign to Senior Agent", config: { assignTo: "u2" }, position: { x: 100, y: 240 }, nextNodeIds: [] },
      { nodeId: "n4", type: "reply", label: "Auto-Reply", config: { message: "Thanks for reaching out! Our team will get back to you shortly." }, position: { x: 300, y: 240 }, nextNodeIds: [] },
    ],
    triggerCount: 456, lastTriggeredAt: "2025-05-11T06:30:00Z", createdAt: "2025-04-01T00:00:00Z", updatedAt: "2025-05-09T00:00:00Z"
  },
  {
    flowId: "bf3", workspaceId: "w1", name: "After-Hours Responder", description: "Auto-replies outside business hours", isActive: false,
    nodes: [
      { nodeId: "n1", type: "trigger", label: "Any DM Received", config: { event: "dm_received" }, position: { x: 200, y: 40 }, nextNodeIds: ["n2"] },
      { nodeId: "n2", type: "condition", label: "Outside Hours?", config: { field: "time", operator: "outside", value: "09:00-17:00" }, position: { x: 200, y: 140 }, nextNodeIds: ["n3"] },
      { nodeId: "n3", type: "reply", label: "Out of Office", config: { message: "Thanks for your message! Our team is currently offline. We'll respond first thing in the morning." }, position: { x: 200, y: 240 }, nextNodeIds: [] },
    ],
    triggerCount: 89, lastTriggeredAt: "2025-05-10T22:00:00Z", createdAt: "2025-04-15T00:00:00Z", updatedAt: "2025-05-05T00:00:00Z"
  }
];

export const mockReviews: InboxReview[] = [
  { reviewId: "r1", workspaceId: "w1", source: "google_my_business", reviewerName: "Alex Wright", reviewerAvatarUrl: null, rating: 4, title: "Great tool but needs work", body: "Nexora has been amazing for scheduling but the analytics dashboard could use more export options.", response: null, respondedAt: null, sentiment: "neutral", createdAt: "2025-05-10T18:00:00Z" },
  { reviewId: "r2", workspaceId: "w1", source: "apple_app_store", reviewerName: "ContentKing99", reviewerAvatarUrl: null, rating: 1, title: "Crashes on startup", body: "App crashes every time I try to open the composer. iPhone 15 Pro, iOS 18.", response: null, respondedAt: null, sentiment: "negative", createdAt: "2025-05-10T12:30:00Z" },
  { reviewId: "r3", workspaceId: "w1", source: "yelp", reviewerName: "SmallBizOwner", reviewerAvatarUrl: null, rating: 5, title: "Life saver for my bakery", body: "Nexora helped me manage all my social accounts from one place. My bakery's Instagram grew 300%!", response: "Thank you so much! We're thrilled to hear about your success 🎉", respondedAt: "2025-05-10T10:00:00Z", sentiment: "positive", createdAt: "2025-05-09T20:00:00Z" },
  { reviewId: "r4", workspaceId: "w1", source: "google_my_business", reviewerName: "MarketingMaven", reviewerAvatarUrl: null, rating: 5, title: "Best in class", body: "Switched from Hootsuite and never looked back. The AI features are incredible.", response: null, respondedAt: null, sentiment: "positive", createdAt: "2025-05-08T14:00:00Z" },
];

export const teamMembers = [
  { userId: "u1", name: "Priya S.", avatar: "PS", gradient: "from-sky-400 to-sky-600" },
  { userId: "u2", name: "Arjun M.", avatar: "AM", gradient: "from-emerald-400 to-emerald-600" },
  { userId: "u3", name: "Sara K.", avatar: "SK", gradient: "from-violet-400 to-violet-600" },
  { userId: "u4", name: "Dev R.", avatar: "DR", gradient: "from-amber-400 to-amber-600" },
];

export const TAG_COLORS: Record<string, { bg: string; text: string; border: string }> = {
  sky: { bg: "bg-sky-50", text: "text-sky-700", border: "border-sky-200" },
  emerald: { bg: "bg-emerald-50", text: "text-emerald-700", border: "border-emerald-200" },
  amber: { bg: "bg-amber-50", text: "text-amber-700", border: "border-amber-200" },
  rose: { bg: "bg-rose-50", text: "text-rose-700", border: "border-rose-200" },
  violet: { bg: "bg-violet-50", text: "text-violet-700", border: "border-violet-200" },
  orange: { bg: "bg-orange-50", text: "text-orange-700", border: "border-orange-200" },
  teal: { bg: "bg-teal-50", text: "text-teal-700", border: "border-teal-200" },
  pink: { bg: "bg-pink-50", text: "text-pink-700", border: "border-pink-200" },
  indigo: { bg: "bg-indigo-50", text: "text-indigo-700", border: "border-indigo-200" },
  slate: { bg: "bg-slate-50", text: "text-slate-700", border: "border-slate-200" },
  red: { bg: "bg-red-50", text: "text-red-700", border: "border-red-200" },
  cyan: { bg: "bg-cyan-50", text: "text-cyan-700", border: "border-cyan-200" },
};

export const PROVIDER_META: Record<string, { label: string; icon: string; color: string }> = {
  instagram: { label: "Instagram", icon: "📸", color: "from-pink-500 to-purple-500" },
  meta: { label: "Facebook", icon: "👤", color: "from-blue-500 to-blue-600" },
  x: { label: "X", icon: "𝕏", color: "from-slate-800 to-slate-900" },
  linkedin: { label: "LinkedIn", icon: "💼", color: "from-blue-600 to-blue-700" },
  tiktok: { label: "TikTok", icon: "🎵", color: "from-slate-900 to-pink-500" },
  google_my_business: { label: "Google", icon: "🏢", color: "from-blue-500 to-green-500" },
  apple_app_store: { label: "App Store", icon: "🍎", color: "from-slate-600 to-slate-800" },
  yelp: { label: "Yelp", icon: "⭐", color: "from-red-500 to-red-600" },
};

export const TYPE_ICONS: Record<string, string> = {
  dm: "✉️",
  comment: "💬",
  mention: "📢",
  review: "⭐",
};
