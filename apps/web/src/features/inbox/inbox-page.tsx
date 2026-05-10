import { useState, useMemo } from "react";
import { Card, CardTitle, Badge, Button, cn } from "@nexora/ui";
import {
  Search, Filter, Mail, MessageSquare, AtSign, Star, Inbox,
  Shield, Bot, Archive, Tag, CheckCheck, Sparkles, Users
} from "lucide-react";
import { mockMessages, inboxTags, mockReviews, TAG_COLORS, PROVIDER_META, TYPE_ICONS, teamMembers } from "./inbox-data";
import { ConversationDetail } from "./inbox-detail";
import { ModerationRulesModal, BotBuilderModal } from "./inbox-modals";
import type { InboxMessage, InboxMessageType, InboxMessageStatus, InboxSentiment } from "@nexora/contracts";

type TabKey = "all" | "dm" | "comment" | "mention" | "review" | "assigned";

const TABS: { key: TabKey; label: string; icon: React.ReactNode }[] = [
  { key: "all", label: "All", icon: <Inbox className="h-3.5 w-3.5" /> },
  { key: "dm", label: "DMs", icon: <Mail className="h-3.5 w-3.5" /> },
  { key: "comment", label: "Comments", icon: <MessageSquare className="h-3.5 w-3.5" /> },
  { key: "mention", label: "Mentions", icon: <AtSign className="h-3.5 w-3.5" /> },
  { key: "review", label: "Reviews", icon: <Star className="h-3.5 w-3.5" /> },
  { key: "assigned", label: "Assigned to Me", icon: <Users className="h-3.5 w-3.5" /> },
];

const STATUS_OPTIONS: { value: InboxMessageStatus | "all"; label: string }[] = [
  { value: "all", label: "All Status" },
  { value: "unread", label: "Unread" },
  { value: "read", label: "Read" },
  { value: "replied", label: "Replied" },
  { value: "archived", label: "Archived" },
];

const SENTIMENT_OPTIONS: { value: InboxSentiment | "all"; label: string }[] = [
  { value: "all", label: "All Sentiment" },
  { value: "positive", label: "😊 Positive" },
  { value: "neutral", label: "😐 Neutral" },
  { value: "negative", label: "😠 Negative" },
];

const NETWORK_OPTIONS = [
  { value: "all", label: "All Networks" },
  { value: "instagram", label: "📸 Instagram" },
  { value: "meta", label: "👤 Facebook" },
  { value: "x", label: "𝕏 X (Twitter)" },
  { value: "linkedin", label: "💼 LinkedIn" },
  { value: "google_my_business", label: "🏢 Google" },
  { value: "apple_app_store", label: "🍎 App Store" },
  { value: "yelp", label: "⭐ Yelp" },
];

export function InboxPage() {
  const [activeTab, setActiveTab] = useState<TabKey>("all");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [sentimentFilter, setSentimentFilter] = useState<string>("all");
  const [networkFilter, setNetworkFilter] = useState<string>("all");
  const [tagFilter, setTagFilter] = useState<string>("all");
  const [showFilters, setShowFilters] = useState(false);
  const [showModeration, setShowModeration] = useState(false);
  const [showBotBuilder, setShowBotBuilder] = useState(false);

  const filtered = useMemo(() => {
    let msgs = [...mockMessages];
    if (activeTab !== "all" && activeTab !== "assigned") msgs = msgs.filter(m => m.type === activeTab);
    if (activeTab === "assigned") msgs = msgs.filter(m => m.assignedToUserId != null);
    if (statusFilter !== "all") msgs = msgs.filter(m => m.status === statusFilter);
    if (sentimentFilter !== "all") msgs = msgs.filter(m => m.sentiment === sentimentFilter);
    if (networkFilter !== "all") msgs = msgs.filter(m => m.provider === networkFilter);
    if (tagFilter !== "all") msgs = msgs.filter(m => m.tags.some(t => t.tagId === tagFilter));
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      msgs = msgs.filter(m => m.body.toLowerCase().includes(q) || m.sender.displayName.toLowerCase().includes(q));
    }
    return msgs;
  }, [activeTab, statusFilter, sentimentFilter, networkFilter, tagFilter, searchQuery]);

  const selected = mockMessages.find(m => m.messageId === selectedId) ?? null;
  const unreadCount = mockMessages.filter(m => m.status === "unread").length;

  return (
    <div className="space-y-4 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="font-display text-2xl text-white">Smart Inbox</h2>
            {unreadCount > 0 && (
              <span className="flex items-center gap-1.5 rounded-full bg-sky-500/20 px-3 py-1 text-xs font-bold text-sky-400 border border-sky-500/30">
                <span className="unread-pulse" style={{ width: 6, height: 6 }} />{unreadCount} unread
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-300">Manage conversations, reviews, and customer interactions across all channels.</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" className="text-xs" onClick={() => setShowModeration(true)}>
            <Shield className="h-3.5 w-3.5 mr-1.5" />Moderation
          </Button>
          <Button variant="secondary" className="text-xs" onClick={() => setShowBotBuilder(true)}>
            <Bot className="h-3.5 w-3.5 mr-1.5" />Bot Builder
          </Button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-white/10 overflow-x-auto pb-0">
        {TABS.map(tab => {
          const count = tab.key === "all" ? mockMessages.length
            : tab.key === "assigned" ? mockMessages.filter(m => m.assignedToUserId).length
            : mockMessages.filter(m => m.type === tab.key).length;
          return (
            <button key={tab.key} onClick={() => setActiveTab(tab.key)}
              className={cn("inbox-tab flex items-center gap-1.5 px-4 py-2.5 text-xs font-semibold whitespace-nowrap transition-colors",
                activeTab === tab.key ? "text-sky-400" : "text-slate-400 hover:text-slate-200"
              )} data-active={activeTab === tab.key}>
              {tab.icon}{tab.label}
              <span className={cn("rounded-md px-1.5 py-0.5 text-[10px] font-bold", activeTab === tab.key ? "bg-sky-500/20 text-sky-400" : "bg-white/10 text-slate-300")}>{count}</span>
            </button>
          );
        })}
      </div>

      {/* Main 3-Column Layout */}
      <div className="grid gap-4 lg:grid-cols-[280px_1fr_380px]" style={{ minHeight: "calc(100vh - 340px)" }}>
        {/* Left: Filter Sidebar */}
        <div className="space-y-3">
          {/* Search */}
          <div className="flex items-center gap-2 rounded-2xl border border-white/10 glass-light px-3 py-2.5 backdrop-blur transition-all hover:border-sky-500/50 focus-within:border-sky-500/50 focus-within:ring-2 focus-within:ring-sky-500/20">
            <Search className="h-4 w-4 text-slate-400" />
            <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
              className="flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500" placeholder="Search messages..." />
          </div>

          {/* Quick Stats */}
          <Card className="!p-4 !rounded-2xl glass border-white/10">
            <div className="grid grid-cols-2 gap-3">
              <div className="text-center"><p className="font-display text-xl font-semibold text-white">{unreadCount}</p><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Unread</p></div>
              <div className="text-center"><p className="font-display text-xl font-semibold text-white">{mockMessages.filter(m => m.assignedToUserId).length}</p><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Assigned</p></div>
              <div className="text-center"><p className="font-display text-xl font-semibold text-emerald-400">{mockMessages.filter(m => m.sentiment === "positive").length}</p><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Positive</p></div>
              <div className="text-center"><p className="font-display text-xl font-semibold text-red-400">{mockMessages.filter(m => m.sentiment === "negative").length}</p><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Negative</p></div>
            </div>
          </Card>

          {/* Filters */}
          <Card className="!p-4 !rounded-2xl glass border-white/10">
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs font-bold uppercase tracking-wider text-slate-400">Filters</p>
              <Filter className="h-3.5 w-3.5 text-slate-400" />
            </div>
            <div className="space-y-2.5">
              <select value={networkFilter} onChange={e => setNetworkFilter(e.target.value)}
                className="w-full rounded-xl border border-white/10 bg-slate-900 text-white px-3 py-2 text-xs focus:border-sky-500/50 focus:outline-none">
                {NETWORK_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
                className="w-full rounded-xl border border-white/10 bg-slate-900 text-white px-3 py-2 text-xs focus:border-sky-500/50 focus:outline-none">
                {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <select value={sentimentFilter} onChange={e => setSentimentFilter(e.target.value)}
                className="w-full rounded-xl border border-white/10 bg-slate-900 text-white px-3 py-2 text-xs focus:border-sky-500/50 focus:outline-none">
                {SENTIMENT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <select value={tagFilter} onChange={e => setTagFilter(e.target.value)}
                className="w-full rounded-xl border border-white/10 bg-slate-900 text-white px-3 py-2 text-xs focus:border-sky-500/50 focus:outline-none">
                <option value="all">All Tags</option>
                {inboxTags.map(t => <option key={t.tagId} value={t.tagId}>{t.label}</option>)}
              </select>
            </div>
            {(statusFilter !== "all" || sentimentFilter !== "all" || networkFilter !== "all" || tagFilter !== "all") && (
              <button onClick={() => { setStatusFilter("all"); setSentimentFilter("all"); setNetworkFilter("all"); setTagFilter("all"); }}
                className="mt-3 text-[10px] font-semibold text-sky-400 hover:text-sky-300">Clear All Filters</button>
            )}
          </Card>

          {/* Batch Actions */}
          <Card className="!p-3 !rounded-2xl glass border-white/10">
            <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-2">Batch Actions</p>
            <div className="grid grid-cols-2 gap-1.5">
              <button className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-[10px] font-semibold text-slate-300 hover:bg-sky-500/10 hover:text-sky-300 transition-colors">
                <CheckCheck className="h-3 w-3" />Mark Read
              </button>
              <button className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-[10px] font-semibold text-slate-300 hover:bg-white/10 hover:text-white transition-colors">
                <Archive className="h-3 w-3" />Archive
              </button>
              <button className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-[10px] font-semibold text-slate-300 hover:bg-violet-500/10 hover:text-violet-300 transition-colors">
                <Tag className="h-3 w-3" />Tag
              </button>
              <button className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-[10px] font-semibold text-slate-300 hover:bg-emerald-500/10 hover:text-emerald-300 transition-colors">
                <Users className="h-3 w-3" />Assign
              </button>
            </div>
          </Card>
        </div>

        {/* Center: Message List */}
        <Card className="!p-0 !rounded-2xl overflow-hidden glass border-white/10">
          <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
            <p className="text-xs font-semibold text-slate-300">{filtered.length} conversation{filtered.length !== 1 ? "s" : ""}</p>
            <Badge className="text-[10px] glass-light text-white border-white/20"><Sparkles className="h-2.5 w-2.5 mr-1" />Live</Badge>
          </div>
          <div className="overflow-auto inbox-scroll" style={{ maxHeight: "calc(100vh - 400px)" }}>
            {filtered.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 text-center">
                <Inbox className="h-10 w-10 text-white/20 mb-3" />
                <p className="text-sm font-semibold text-slate-300">No messages found</p>
                <p className="text-xs text-slate-400 mt-1">Try adjusting your filters</p>
              </div>
            ) : (
              <div className="stagger">
                {filtered.map(msg => {
                  const prov = PROVIDER_META[msg.provider] ?? { label: msg.provider, icon: "📨", color: "from-slate-500 to-slate-600" };
                  const isActive = selectedId === msg.messageId;
                  const timeAgo = getTimeAgo(msg.createdAt);
                  return (
                    <div key={msg.messageId} onClick={() => setSelectedId(msg.messageId)}
                      className={cn("inbox-message-row flex items-start gap-3 px-4 py-3.5 border-b border-white/5", isActive && "!bg-sky-500/10")}
                      data-active={isActive}>
                      {/* Avatar */}
                      <div className="relative shrink-0">
                        <div className={cn("flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br text-[11px] font-bold text-white shadow-md", prov.color)}>
                          {msg.sender.displayName.split(" ").map(w => w[0]).join("").slice(0, 2)}
                        </div>
                        {msg.status === "unread" && <div className="absolute -top-0.5 -right-0.5 unread-pulse" />}
                      </div>

                      {/* Content */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-1.5 min-w-0">
                            <p className={cn("text-sm truncate", msg.status === "unread" ? "font-bold text-white" : "font-semibold text-slate-300")}>
                              {msg.sender.displayName}
                            </p>
                            {msg.sender.isVerified && <span className="text-sky-500 text-[10px] shrink-0">✓</span>}
                            {msg.collisionLock && <span className="text-amber-500 text-[10px] shrink-0" title="Agent replying">🔒</span>}
                          </div>
                          <span className="text-[10px] text-slate-400 shrink-0">{timeAgo}</span>
                        </div>

                        <p className={cn("text-xs mt-0.5 line-clamp-2", msg.status === "unread" ? "text-slate-200" : "text-slate-400")}>
                          {msg.body}
                        </p>

                        <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                          <span className="text-[10px] flex items-center gap-1">
                            <span>{TYPE_ICONS[msg.type]}</span>
                            <span className="text-slate-400">{prov.label}</span>
                          </span>
                          <span className="sentiment-dot" data-sentiment={msg.sentiment} style={{ width: 6, height: 6 }} />
                          {msg.reviewRating != null && (
                            <span className="text-[10px] text-amber-500">{"★".repeat(msg.reviewRating)}{"☆".repeat(5 - msg.reviewRating)}</span>
                          )}
                          {msg.tags.slice(0, 2).map(tag => {
                            const tc = TAG_COLORS[tag.color] ?? TAG_COLORS.slate;
                            return <span key={tag.tagId} className={cn("tag-chip", tc.bg, tc.text, tc.border)} style={{ fontSize: 8, padding: "1px 5px" }}>{tag.label}</span>;
                          })}
                          {msg.tags.length > 2 && <span className="text-[10px] text-slate-400">+{msg.tags.length - 2}</span>}
                          {msg.assignedToName && <span className="text-[10px] text-violet-500 font-semibold">→ {msg.assignedToName}</span>}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </Card>

        {/* Right: Conversation Detail */}
        <Card className="!p-0 !rounded-2xl overflow-hidden glass border-white/10">
          <ConversationDetail message={selected} onClose={() => setSelectedId(null)} />
        </Card>
      </div>

      {/* Modals */}
      {showModeration && <ModerationRulesModal onClose={() => setShowModeration(false)} />}
      {showBotBuilder && <BotBuilderModal onClose={() => setShowBotBuilder(false)} />}
    </div>
  );
}

function getTimeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h`;
  const days = Math.floor(hrs / 24);
  return `${days}d`;
}
