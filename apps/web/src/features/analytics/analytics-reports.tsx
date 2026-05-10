import { useState } from "react";
import { Card, CardTitle, Badge, Button, cn } from "@nexora/ui";
import { FileText, Plus, Image as ImageIcon, Send, Clock, GripHorizontal, Settings } from "lucide-react";
import { mockReportTemplates } from "./analytics-data";

export function AnalyticsReports() {
  const [templates, setTemplates] = useState(mockReportTemplates);
  const [builderOpen, setBuilderOpen] = useState(false);

  if (builderOpen) {
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div>
            <h3 className="text-lg font-semibold text-white">Report Builder</h3>
            <p className="text-sm text-slate-300">Drag and drop modules to construct a custom report.</p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="secondary" onClick={() => setBuilderOpen(false)}>Cancel</Button>
            <Button>Save Template</Button>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-[300px_1fr_300px]">
          {/* Modules Library */}
          <Card className="!p-4 h-max glass border-white/10">
            <CardTitle className="text-sm mb-4 text-white">Available Modules</CardTitle>
            <div className="space-y-2">
              {["Executive Summary", "Platform Breakdown", "Top Performing Content", "Audience Demographics", "Competitor Share of Voice", "UTM Campaign Conversions"].map(mod => (
                <div key={mod} className="flex items-center gap-3 rounded-xl border border-white/20 border-dashed glass-light p-3 cursor-grab hover:border-sky-500/50 hover:bg-sky-500/10 transition-colors">
                  <GripHorizontal className="h-4 w-4 text-slate-400" />
                  <span className="text-xs font-semibold text-slate-200">{mod}</span>
                </div>
              ))}
            </div>
          </Card>

          {/* Canvas */}
          <div className="rounded-3xl border-2 border-dashed border-white/20 glass p-8 min-h-[600px] flex flex-col gap-4">
            {/* Header Module */}
            <div className="group relative rounded-2xl border border-white/10 glass-light p-6 hover:border-sky-500/50 transition-colors">
              <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                <Button variant="secondary" className="h-8 px-2 text-xs"><Settings className="h-3 w-3 mr-1" />Edit</Button>
              </div>
              <div className="flex items-center justify-between mb-4">
                <h1 className="text-2xl font-bold text-white">Monthly Performance Report</h1>
                <div className="h-12 w-32 rounded bg-white/5 flex items-center justify-center border border-white/20 border-dashed cursor-pointer hover:bg-white/10 transition-colors">
                  <ImageIcon className="h-4 w-4 text-slate-400 mr-1" /><span className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Add Agency Logo</span>
                </div>
              </div>
              <p className="text-sm text-slate-400">Prepared for: Client Name</p>
            </div>

            {/* Dropped Modules */}
            <div className="rounded-2xl border border-white/10 glass-light p-6">
              <div className="flex items-center gap-2 mb-4">
                <GripHorizontal className="h-4 w-4 text-slate-400 cursor-grab" />
                <h4 className="font-semibold text-white">Executive Summary</h4>
              </div>
              <div className="grid grid-cols-4 gap-4">
                {[1, 2, 3, 4].map(i => <div key={i} className="h-20 rounded-xl bg-white/10 animate-pulse" />)}
              </div>
            </div>

            <div className="rounded-2xl border border-white/10 glass-light p-6">
              <div className="flex items-center gap-2 mb-4">
                <GripHorizontal className="h-4 w-4 text-slate-400 cursor-grab" />
                <h4 className="font-semibold text-white">Platform Breakdown</h4>
              </div>
              <div className="h-40 rounded-xl bg-white/10 animate-pulse" />
            </div>

            <div className="flex-1 flex items-center justify-center rounded-2xl border-2 border-dashed border-sky-500/30 bg-sky-500/10 text-sky-400 text-sm font-semibold p-8">
              Drop modules here
            </div>
          </div>

          {/* Settings */}
          <Card className="!p-4 h-max glass border-white/10">
            <CardTitle className="text-sm mb-4 text-white">Automation Settings</CardTitle>
            <div className="space-y-4">
              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1 block">Frequency</label>
                <select className="w-full rounded-xl border border-white/10 glass-light px-3 py-2 text-xs focus:border-sky-500/50 focus:outline-none text-white">
                  <option className="bg-slate-900">Monthly (1st of Month)</option>
                  <option className="bg-slate-900">Weekly (Monday)</option>
                  <option className="bg-slate-900">Manual Only</option>
                </select>
              </div>
              <div>
                <label className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1 block">Recipients (Email)</label>
                <textarea className="w-full rounded-xl border border-white/10 glass-light px-3 py-2 text-xs focus:border-sky-500/50 focus:outline-none h-20 text-white placeholder:text-slate-500" placeholder="client@example.com, ceo@example.com" />
              </div>
              <Button className="w-full text-xs bg-emerald-500 hover:bg-emerald-600 text-white shadow-emerald-500/20 shadow-lg"><Clock className="h-3.5 w-3.5 mr-1" />Enable Schedule</Button>
            </div>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-white">Automated Reports</h3>
          <p className="text-sm text-slate-300">Design white-labeled PDF reports and schedule them for your clients.</p>
        </div>
        <Button onClick={() => setBuilderOpen(true)} className="text-xs shadow-lg shadow-sky-600/20">
          <Plus className="h-3.5 w-3.5 mr-1" />New Report Template
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {templates.map(tmpl => (
          <Card key={tmpl.templateId} className="group hover-lift !p-5 cursor-pointer glass border-white/10">
            <div className="flex items-start justify-between mb-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl glass-light border border-sky-500/30 text-sky-400">
                <FileText className="h-5 w-5" />
              </div>
              <Badge className="glass-light text-emerald-400 border-emerald-500/30 text-[10px]">Active Schedule</Badge>
            </div>
            <h4 className="font-semibold text-white line-clamp-1">{tmpl.name}</h4>
            <p className="text-xs text-slate-300 mt-1 line-clamp-2 min-h-[32px]">{tmpl.description}</p>
            
            <div className="mt-4 pt-4 border-t border-white/10 flex items-center justify-between">
              <span className="text-[10px] text-slate-400 font-semibold">{tmpl.modules.length} Modules</span>
              <button className="flex items-center gap-1 text-xs font-semibold text-sky-400 opacity-0 group-hover:opacity-100 transition-opacity">
                <Send className="h-3 w-3" />Send Now
              </button>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
