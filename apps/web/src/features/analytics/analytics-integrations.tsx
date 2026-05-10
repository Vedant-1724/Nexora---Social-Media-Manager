import { useState } from "react";
import { Card, CardTitle, Badge, Button, cn } from "@nexora/ui";
import { Database, Link as LinkIcon, CheckCircle2, AlertCircle } from "lucide-react";
import { mockBIIntegrations } from "./analytics-data";

export function AnalyticsIntegrations() {
  const [integrations, setIntegrations] = useState(mockBIIntegrations);

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="max-w-3xl">
        <h3 className="text-lg font-semibold text-slate-900">Business Intelligence Integrations</h3>
        <p className="text-sm text-slate-500 mt-1">
          Pipe raw social data and metrics directly into your organization's BI tools. We provide real-time connection keys for seamless syncing.
        </p>
      </div>

      <div className="grid gap-6 max-w-4xl">
        {/* Looker Studio */}
        <Card className="!p-6 border-slate-200/60 bg-white/60 backdrop-blur">
          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white border border-slate-100 shadow-sm">
                {/* Looker Studio Logo Placeholder */}
                <div className="flex gap-1">
                  <div className="w-1.5 h-6 bg-blue-600 rounded-full" />
                  <div className="w-1.5 h-4 bg-yellow-500 rounded-full self-end" />
                  <div className="w-1.5 h-8 bg-red-500 rounded-full self-end" />
                </div>
              </div>
              <div>
                <h4 className="text-base font-semibold text-slate-900">Looker Studio</h4>
                <p className="text-sm text-slate-500 mt-0.5">Visualize your Nexora data in Google's Looker Studio.</p>
              </div>
            </div>
            {integrations.find(i => i.provider === "looker_studio")?.status === "active" ? (
              <Badge className="bg-emerald-50 text-emerald-700 border-emerald-200">
                <CheckCircle2 className="h-3 w-3 mr-1" /> Connected
              </Badge>
            ) : (
              <Button variant="secondary" className="text-xs">Connect</Button>
            )}
          </div>

          {integrations.find(i => i.provider === "looker_studio")?.status === "active" && (
            <div className="mt-6 p-4 rounded-xl bg-slate-50 border border-slate-100">
              <div className="flex justify-between items-center mb-2">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-500">Connection Details</p>
                <p className="text-xs text-slate-400">Last sync: {new Date(integrations.find(i => i.provider === "looker_studio")!.lastSyncAt!).toLocaleString()}</p>
              </div>
              <div className="flex items-center gap-2">
                <code className="flex-1 rounded border border-slate-200 bg-white px-3 py-2 text-xs text-slate-700 font-mono">
                  {integrations.find(i => i.provider === "looker_studio")!.connectionKey}
                </code>
                <Button variant="secondary" className="h-[34px] px-3"><LinkIcon className="h-3.5 w-3.5" /></Button>
              </div>
              <p className="text-[10px] text-slate-400 mt-2">Use this key in the Nexora Looker Studio Connector.</p>
            </div>
          )}
        </Card>

        {/* Tableau */}
        <Card className="!p-6 border-slate-200/60 bg-white/60 backdrop-blur">
          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white border border-slate-100 shadow-sm text-2xl font-bold text-slate-800">
                +
              </div>
              <div>
                <h4 className="text-base font-semibold text-slate-900">Tableau</h4>
                <p className="text-sm text-slate-500 mt-0.5">Pipe data to Tableau Web Data Connector (WDC).</p>
              </div>
            </div>
            {integrations.find(i => i.provider === "tableau")?.status === "active" ? (
              <Badge className="bg-emerald-50 text-emerald-700 border-emerald-200">
                <CheckCircle2 className="h-3 w-3 mr-1" /> Connected
              </Badge>
            ) : (
              <Button variant="secondary" className="text-xs">Connect</Button>
            )}
          </div>
          
          {integrations.find(i => i.provider === "tableau")?.status !== "active" && (
            <div className="mt-4 flex items-start gap-2 rounded-xl bg-amber-50/50 p-3 border border-amber-100">
              <AlertCircle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
              <p className="text-xs text-amber-800">Tableau integration requires an Enterprise plan. <a href="#" className="font-semibold underline">Upgrade to enable</a>.</p>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
