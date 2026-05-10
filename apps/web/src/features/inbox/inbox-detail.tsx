import { useState } from "react";
import { Card, CardTitle, Badge, Button, cn } from "@nexora/ui";
import { Send, Sparkles, StickyNote, UserPlus, Star, ChevronDown, X, Lock } from "lucide-react";
import { TAG_COLORS, PROVIDER_META, teamMembers } from "./inbox-data";
import type { InboxMessage } from "@nexora/contracts";

function StarRating({ rating }: { rating: number }) {
  return (
    <div className="review-stars">
      {[1,2,3,4,5].map(i => (
        <Star key={i} className={cn("h-4 w-4", i <= rating ? "star-filled fill-current" : "star-empty fill-current")} />
      ))}
    </div>
  );
}

export function ConversationDetail({ message, onClose }: { message: InboxMessage | null; onClose: () => void }) {
  const [replyText, setReplyText] = useState("");
  const [showNotes, setShowNotes] = useState(false);
  const [showAI, setShowAI] = useState(false);
  const [noteText, setNoteText] = useState("");
  const [showAssign, setShowAssign] = useState(false);

  if (!message) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center px-6">
        <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-slate-100 mb-4"><Send className="h-7 w-7 text-slate-300" /></div>
        <p className="text-sm font-semibold text-slate-500">Select a conversation</p>
        <p className="text-xs text-slate-400 mt-1">Choose a message to view details and reply</p>
      </div>
    );
  }

  const prov = PROVIDER_META[message.provider] ?? { label: message.provider, icon: "📨", color: "from-slate-500 to-slate-600" };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
        <div className="flex items-center gap-3 min-w-0">
          <div className={cn("flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br text-sm font-bold text-white shadow-lg", prov.color)}>
            {message.sender.displayName.split(" ").map(w => w[0]).join("").slice(0, 2)}
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <p className="text-sm font-semibold text-slate-900 truncate">{message.sender.displayName}</p>
              {message.sender.isVerified && <span className="text-sky-500 text-xs">✓</span>}
            </div>
            <p className="text-xs text-slate-400">{message.sender.username ?? prov.label} · {message.sender.followerCount?.toLocaleString() ?? "—"} followers</p>
          </div>
        </div>
        <button onClick={onClose} className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 lg:hidden"><X className="h-4 w-4" /></button>
      </div>

      {/* Collision Lock Banner */}
      {message.collisionLock && (
        <div className="mx-5 mt-3">
          <div className="collision-lock-pulse"><Lock className="h-3 w-3" />{message.collisionLock.lockedByName} is currently replying…</div>
        </div>
      )}

      {/* Message Content */}
      <div className="flex-1 overflow-auto px-5 py-4 inbox-scroll space-y-4">
        {/* Review Rating */}
        {message.reviewRating != null && (
          <div className="flex items-center gap-3 mb-2">
            <StarRating rating={message.reviewRating} />
            <Badge className="text-[10px]">{message.reviewSource?.replace(/_/g, " ").toUpperCase()}</Badge>
          </div>
        )}

        {/* Message Body */}
        <Card className="!p-4 !rounded-2xl">
          <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-wrap">{message.body}</p>
          <div className="flex items-center gap-2 mt-3 pt-3 border-t border-slate-100/50">
            <span className="text-lg">{prov.icon}</span>
            <span className="text-[10px] text-slate-400">{prov.label} · {message.type.toUpperCase()}</span>
            <span className="text-slate-300">·</span>
            <span className="text-[10px] text-slate-400">{new Date(message.createdAt).toLocaleString()}</span>
            <span className="ml-auto"><span className="sentiment-dot" data-sentiment={message.sentiment} /></span>
          </div>
        </Card>

        {/* Tags */}
        <div className="flex flex-wrap gap-1.5">
          {message.tags.map(tag => {
            const tc = TAG_COLORS[tag.color] ?? TAG_COLORS.slate;
            return <span key={tag.tagId} className={cn("tag-chip", tc.bg, tc.text, tc.border)}>{tag.label}</span>;
          })}
          <button className="tag-chip bg-slate-50 text-slate-400 border-dashed border-slate-200 hover:border-sky-300 hover:text-sky-600">+ Tag</button>
        </div>

        {/* Assignment */}
        <div className="flex items-center justify-between rounded-2xl border border-slate-100 bg-slate-50/50 px-4 py-3">
          <div className="flex items-center gap-2">
            <UserPlus className="h-4 w-4 text-slate-400" />
            <span className="text-xs font-semibold text-slate-600">
              {message.assignedToName ? `Assigned to ${message.assignedToName}` : "Unassigned"}
            </span>
          </div>
          <div className="relative">
            <button onClick={() => setShowAssign(!showAssign)} className="text-xs font-semibold text-sky-600 hover:text-sky-700 flex items-center gap-1">
              {message.assignedToName ? "Reassign" : "Assign"}<ChevronDown className="h-3 w-3" />
            </button>
            {showAssign && (
              <div className="absolute right-0 top-full mt-2 w-48 rounded-2xl border border-slate-200/60 bg-white/95 p-2 shadow-xl z-10">
                {teamMembers.map(m => (
                  <button key={m.userId} onClick={() => setShowAssign(false)} className="w-full flex items-center gap-2 rounded-xl px-3 py-2 text-sm hover:bg-sky-50/50 transition-colors">
                    <div className={cn("flex h-6 w-6 items-center justify-center rounded-lg bg-gradient-to-br text-[9px] font-bold text-white", m.gradient)}>{m.avatar}</div>
                    <span className="text-xs font-medium text-slate-700">{m.name}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* CRM Card */}
        <Card className="!p-4 !rounded-2xl glass-dark bg-slate-950 text-white border-white/5">
          <p className="text-[10px] font-bold uppercase tracking-wider text-sky-400 mb-2">Contact CRM</p>
          <div className="grid grid-cols-2 gap-3">
            <div><p className="text-[10px] text-slate-500">Interactions</p><p className="text-sm font-semibold text-white">24</p></div>
            <div><p className="text-[10px] text-slate-500">Sentiment</p><p className="text-sm font-semibold text-emerald-400">78%</p></div>
            <div><p className="text-[10px] text-slate-500">First Seen</p><p className="text-sm font-semibold text-white">Jan 2025</p></div>
            <div><p className="text-[10px] text-slate-500">Lifetime Value</p><p className="text-sm font-semibold text-amber-400">$1,240</p></div>
          </div>
        </Card>

        {/* AI Suggestion */}
        {message.aiSuggestedReply && (
          <div>
            <button onClick={() => setShowAI(!showAI)} className="flex items-center gap-2 text-xs font-semibold text-violet-600 hover:text-violet-700 mb-2">
              <Sparkles className="h-3.5 w-3.5" />{showAI ? "Hide AI Suggestion" : "View AI Suggestion"}
            </button>
            {showAI && (
              <Card className="!p-4 !rounded-2xl ai-suggestion-glow">
                <div className="flex items-center gap-2 mb-2">
                  <Sparkles className="h-3.5 w-3.5 text-violet-500" />
                  <span className="text-[10px] font-bold uppercase tracking-wider text-violet-500">AI-Suggested Reply</span>
                </div>
                <p className="text-sm text-slate-700 leading-relaxed">{message.aiSuggestedReply}</p>
                <Button variant="secondary" className="mt-3 text-xs" onClick={() => { setReplyText(message.aiSuggestedReply!); setShowAI(false); }}>Use This Reply</Button>
              </Card>
            )}
          </div>
        )}

        {/* Internal Notes */}
        <div>
          <button onClick={() => setShowNotes(!showNotes)} className="flex items-center gap-2 text-xs font-semibold text-amber-600 hover:text-amber-700 mb-2">
            <StickyNote className="h-3.5 w-3.5" />Internal Notes ({message.internalNotes.length})
          </button>
          {showNotes && (
            <div className="space-y-2">
              {message.internalNotes.map(note => (
                <div key={note.noteId} className="rounded-xl border border-amber-200/50 bg-amber-50/30 p-3">
                  <p className="text-xs text-slate-700">{note.body}</p>
                  <p className="text-[10px] text-slate-400 mt-1">{note.authorName} · {new Date(note.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</p>
                </div>
              ))}
              <div className="flex gap-2">
                <input value={noteText} onChange={e => setNoteText(e.target.value)} className="flex-1 rounded-xl border border-slate-200/60 bg-white/60 px-3 py-2 text-xs focus:border-amber-300 focus:outline-none" placeholder="Add internal note..." />
                <Button variant="secondary" className="text-xs" onClick={() => setNoteText("")}>Add</Button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Reply Composer */}
      <div className="border-t border-slate-100 p-4">
        <div className="flex gap-2">
          <textarea value={replyText} onChange={e => setReplyText(e.target.value)} rows={2}
            className="flex-1 rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-3 text-sm resize-none focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20 backdrop-blur"
            placeholder={message.collisionLock ? "Another agent is replying..." : "Write a reply..."} disabled={!!message.collisionLock} />
          <Button className="self-end shadow-lg shadow-sky-600/20" disabled={!replyText.trim() || !!message.collisionLock}>
            <Send className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
