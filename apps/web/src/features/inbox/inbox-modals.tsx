import { useState } from "react";
import { Card, CardTitle, Badge, Button, cn } from "@nexora/ui";
import { X, Plus, Shield, Zap, Bot, Play, Pause, Trash2, ArrowRight } from "lucide-react";
import { mockModerationRules, mockBotFlows } from "./inbox-data";

/* ═══════════════════════════════════════════════════════════════════════════
   Moderation Rules Modal
   ═══════════════════════════════════════════════════════════════════════════ */

export function ModerationRulesModal({ onClose }: { onClose: () => void }) {
  const [rules, setRules] = useState(mockModerationRules);
  const [showCreate, setShowCreate] = useState(false);

  const triggerIcons: Record<string, string> = { keyword: "🔤", sentiment: "😠", spam_score: "🛡️", profanity: "🚫" };
  const actionColors: Record<string, string> = { hide: "bg-slate-100 text-slate-700", flag: "bg-amber-100 text-amber-700", auto_reply: "bg-sky-100 text-sky-700", delete: "bg-red-100 text-red-700" };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm animate-fade-in" onClick={onClose}>
      <div className="w-full max-w-2xl max-h-[85vh] overflow-auto rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-2xl backdrop-blur-xl" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-500 to-orange-500 text-white shadow-lg shadow-rose-500/20"><Shield className="h-5 w-5" /></div>
            <div><CardTitle>Moderation Rules</CardTitle><p className="text-xs text-slate-500 mt-0.5">Auto-moderate comments and messages</p></div>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="secondary" onClick={() => setShowCreate(!showCreate)} className="text-xs"><Plus className="h-3 w-3 mr-1" />New Rule</Button>
            <button onClick={onClose} className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600"><X className="h-4 w-4" /></button>
          </div>
        </div>

        {showCreate && (
          <Card className="mb-4 !p-4 glow-border border-sky-200/50 animate-fade-in-down">
            <p className="text-sm font-semibold text-slate-800 mb-3">Create New Rule</p>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1 block">Name</label>
                <input className="w-full rounded-xl border border-slate-200/60 bg-white/60 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20" placeholder="Rule name" />
              </div>
              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1 block">Trigger</label>
                <select className="w-full rounded-xl border border-slate-200/60 bg-white/60 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none">
                  <option>Keyword Match</option><option>Negative Sentiment</option><option>High Spam Score</option><option>Profanity</option>
                </select>
              </div>
              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1 block">Trigger Value</label>
                <input className="w-full rounded-xl border border-slate-200/60 bg-white/60 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-500/20" placeholder="e.g. spam, scam" />
              </div>
              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1 block">Action</label>
                <select className="w-full rounded-xl border border-slate-200/60 bg-white/60 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none">
                  <option>Hide Comment</option><option>Flag for Review</option><option>Auto-Reply</option><option>Delete</option>
                </select>
              </div>
            </div>
            <div className="flex justify-end mt-3"><Button className="text-xs shadow-lg shadow-sky-600/20">Create Rule</Button></div>
          </Card>
        )}

        <div className="space-y-3 stagger">
          {rules.map(rule => (
            <div key={rule.ruleId} className="group flex items-center gap-4 rounded-2xl border border-slate-100 bg-slate-50/50 px-4 py-3.5 transition-all hover:border-sky-200 hover:bg-sky-50/20">
              <span className="text-xl">{triggerIcons[rule.trigger] ?? "⚙️"}</span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-semibold text-slate-800">{rule.name}</p>
                  <span className={cn("rounded-lg px-2 py-0.5 text-[10px] font-bold uppercase", actionColors[rule.action])}>{rule.action.replace("_", " ")}</span>
                </div>
                <p className="text-xs text-slate-400 mt-0.5 truncate">{rule.description}</p>
                <p className="text-[10px] text-slate-400 mt-1">{rule.matchCount} matches</p>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => setRules(prev => prev.map(r => r.ruleId === rule.ruleId ? { ...r, isActive: !r.isActive } : r))} className={cn("rounded-xl p-2 transition-colors", rule.isActive ? "text-emerald-600 hover:bg-emerald-50" : "text-slate-400 hover:bg-slate-100")}>
                  {rule.isActive ? <Play className="h-3.5 w-3.5" /> : <Pause className="h-3.5 w-3.5" />}
                </button>
                <button className="rounded-xl p-2 text-slate-400 hover:bg-red-50 hover:text-red-500"><Trash2 className="h-3.5 w-3.5" /></button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════════════════
   Bot Builder Modal
   ═══════════════════════════════════════════════════════════════════════════ */

const NODE_STYLES: Record<string, { bg: string; border: string; icon: string }> = {
  trigger: { bg: "bg-sky-50", border: "border-sky-300", icon: "⚡" },
  condition: { bg: "bg-amber-50", border: "border-amber-300", icon: "🔀" },
  action: { bg: "bg-violet-50", border: "border-violet-300", icon: "🎯" },
  delay: { bg: "bg-slate-50", border: "border-slate-300", icon: "⏱️" },
  reply: { bg: "bg-emerald-50", border: "border-emerald-300", icon: "💬" },
};

export function BotBuilderModal({ onClose }: { onClose: () => void }) {
  const [flows, setFlows] = useState(mockBotFlows);
  const [selectedFlow, setSelectedFlow] = useState<string | null>(null);

  const activeFlow = flows.find(f => f.flowId === selectedFlow);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm animate-fade-in" onClick={onClose}>
      <div className="w-full max-w-4xl max-h-[85vh] overflow-hidden rounded-[28px] border border-white/70 bg-white/95 shadow-2xl backdrop-blur-xl flex flex-col" onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg shadow-violet-500/20"><Bot className="h-5 w-5" /></div>
            <div><CardTitle>Automation Bot Builder</CardTitle><p className="text-xs text-slate-500 mt-0.5">Build automated response flows</p></div>
          </div>
          <button onClick={onClose} className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600"><X className="h-4 w-4" /></button>
        </div>

        <div className="flex flex-1 overflow-hidden">
          {/* Flow List */}
          <div className="w-72 border-r border-slate-100 p-4 overflow-auto inbox-scroll">
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs font-bold uppercase tracking-wider text-slate-400">Flows</p>
              <button className="rounded-lg p-1.5 text-sky-600 hover:bg-sky-50"><Plus className="h-3.5 w-3.5" /></button>
            </div>
            <div className="space-y-2">
              {flows.map(flow => (
                <button key={flow.flowId} onClick={() => setSelectedFlow(flow.flowId)}
                  className={cn("w-full text-left rounded-2xl border p-3.5 transition-all", selectedFlow === flow.flowId ? "border-sky-300 bg-sky-50/50 shadow-sm" : "border-slate-100 hover:border-sky-200 hover:bg-slate-50/80")}>
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-semibold text-slate-800 truncate">{flow.name}</p>
                    <div className={cn("h-2 w-2 rounded-full", flow.isActive ? "bg-emerald-400 shadow-[0_0_4px_rgba(34,197,94,0.5)]" : "bg-slate-300")} />
                  </div>
                  <p className="text-[10px] text-slate-400 mt-1 truncate">{flow.description}</p>
                  <div className="flex items-center gap-2 mt-2">
                    <span className="text-[10px] font-semibold text-slate-500">{flow.nodes.length} nodes</span>
                    <span className="text-slate-300">·</span>
                    <span className="text-[10px] text-slate-400">{flow.triggerCount} triggers</span>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Flow Canvas */}
          <div className="flex-1 p-6 overflow-auto inbox-scroll">
            {activeFlow ? (
              <div>
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h3 className="font-display text-lg font-semibold text-slate-950">{activeFlow.name}</h3>
                    <p className="text-xs text-slate-500 mt-0.5">{activeFlow.description}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge className={activeFlow.isActive ? "bg-emerald-50 text-emerald-700 border-emerald-200" : "bg-slate-100 text-slate-600 border-slate-200"}>
                      {activeFlow.isActive ? "Active" : "Paused"}
                    </Badge>
                    <Button variant="secondary" className="text-xs" onClick={() => setFlows(prev => prev.map(f => f.flowId === activeFlow.flowId ? { ...f, isActive: !f.isActive } : f))}>
                      {activeFlow.isActive ? <><Pause className="h-3 w-3 mr-1" />Pause</> : <><Play className="h-3 w-3 mr-1" />Activate</>}
                    </Button>
                  </div>
                </div>

                {/* Visual Flow */}
                <div className="flex flex-col items-center gap-1">
                  {activeFlow.nodes.map((node, idx) => {
                    const style = NODE_STYLES[node.type] ?? NODE_STYLES.action;
                    return (
                      <div key={node.nodeId} className="flex flex-col items-center">
                        <div className={cn("bot-flow-node w-72 text-center", style.bg, style.border)}>
                          <div className="flex items-center justify-center gap-2 mb-1">
                            <span className="text-lg">{style.icon}</span>
                            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">{node.type}</span>
                          </div>
                          <p className="text-sm font-semibold text-slate-800">{node.label}</p>
                          {node.config.message && <p className="text-xs text-slate-500 mt-1 line-clamp-2">"{String(node.config.message)}"</p>}
                        </div>
                        {idx < activeFlow.nodes.length - 1 && (
                          <div className="flex flex-col items-center my-1">
                            <div className="w-0.5 h-6 bg-gradient-to-b from-sky-300 to-sky-100" />
                            <ArrowRight className="h-3 w-3 text-sky-400 rotate-90" />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Stats */}
                <div className="mt-8 grid grid-cols-2 gap-3">
                  <Card className="!p-4 !rounded-2xl">
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Total Triggers</p>
                    <p className="font-display text-2xl font-semibold text-slate-950 mt-1">{activeFlow.triggerCount.toLocaleString()}</p>
                  </Card>
                  <Card className="!p-4 !rounded-2xl">
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Last Triggered</p>
                    <p className="text-sm font-semibold text-slate-800 mt-1">{activeFlow.lastTriggeredAt ? new Date(activeFlow.lastTriggeredAt).toLocaleDateString() : "Never"}</p>
                  </Card>
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-center">
                <Bot className="h-12 w-12 text-slate-300 mb-3" />
                <p className="text-sm font-semibold text-slate-500">Select a flow to view</p>
                <p className="text-xs text-slate-400 mt-1">Or create a new automation flow</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
