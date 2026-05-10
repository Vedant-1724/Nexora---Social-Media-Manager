import { Card, CardTitle, Badge, cn } from "@nexora/ui";
import { TrendingUp, TrendingDown, Users, MessageSquare } from "lucide-react";
import { mockCompetitors, mockCompetitorBenchmarks, formatNumber } from "./analytics-data";

export function AnalyticsCompetitors() {
  const selfBenchmark = mockCompetitorBenchmarks.find(b => b.competitorId === "self");
  const others = mockCompetitorBenchmarks.filter(b => b.competitorId !== "self");
  
  // Sort by share of voice
  const allBenchmarks = [...mockCompetitorBenchmarks].sort((a, b) => b.shareOfVoicePercentage - a.shareOfVoicePercentage);

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-slate-900">Competitor Benchmarking</h3>
          <p className="text-sm text-slate-500">Compare your growth and share of voice against industry rivals.</p>
        </div>
        <button className="rounded-xl bg-slate-950 px-4 py-2 text-xs font-semibold text-white shadow-lg shadow-slate-950/20 hover:bg-slate-800 transition-all">
          + Add Competitor
        </button>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        {/* Share of Voice Chart */}
        <Card className="lg:col-span-1 !p-6">
          <CardTitle className="mb-6">Share of Voice</CardTitle>
          <div className="flex flex-col gap-4">
            {allBenchmarks.map(bench => {
              const comp = mockCompetitors.find(c => c.competitorId === bench.competitorId) ?? { name: "Your Brand", provider: "meta" };
              const isSelf = bench.competitorId === "self";
              return (
                <div key={bench.competitorId} className="space-y-2">
                  <div className="flex justify-between text-sm font-semibold text-slate-800">
                    <span className="flex items-center gap-2">
                      {isSelf && <span className="h-2 w-2 rounded-full bg-violet-500" />}
                      {!isSelf && <span className="h-2 w-2 rounded-full bg-slate-300" />}
                      {comp.name}
                    </span>
                    <span>{bench.shareOfVoicePercentage}%</span>
                  </div>
                  <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
                    <div 
                      className={cn("h-full rounded-full transition-all duration-1000", isSelf ? "bg-violet-500" : "bg-slate-300")}
                      style={{ width: `${bench.shareOfVoicePercentage}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
          <div className="mt-8 rounded-xl bg-violet-50 p-4 border border-violet-100">
            <p className="text-sm font-semibold text-violet-800">Market Position: #2</p>
            <p className="text-xs text-violet-600 mt-1">You gained 4% share of voice this period.</p>
          </div>
        </Card>

        {/* Detailed Benchmark Table */}
        <Card className="lg:col-span-2 !p-0 overflow-hidden">
          <div className="p-6 border-b border-slate-100 flex justify-between items-center">
            <CardTitle>Growth Comparison</CardTitle>
            <Badge className="bg-sky-50 text-sky-700">Last 30 Days</Badge>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50/50 text-left text-xs font-bold uppercase tracking-wider text-slate-500">
                  <th className="px-6 py-4">Brand</th>
                  <th className="px-6 py-4 text-right">Followers</th>
                  <th className="px-6 py-4 text-right">Growth Rate</th>
                  <th className="px-6 py-4 text-right">Engagements</th>
                  <th className="px-6 py-4 text-right">Posts</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {allBenchmarks.map(bench => {
                  const comp = mockCompetitors.find(c => c.competitorId === bench.competitorId) ?? { name: "Your Brand", handle: "@yourbrand", provider: "meta" };
                  const isSelf = bench.competitorId === "self";
                  return (
                    <tr key={bench.competitorId} className={cn("transition-colors hover:bg-slate-50/50", isSelf && "bg-violet-50/30 hover:bg-violet-50/50")}>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className={cn("flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold text-white", isSelf ? "bg-violet-500" : "bg-slate-400")}>
                            {comp.name.substring(0, 1)}
                          </div>
                          <div>
                            <p className="text-sm font-semibold text-slate-800">{comp.name}</p>
                            <p className="text-xs text-slate-400">{comp.handle}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <p className="text-sm font-semibold text-slate-700">{formatNumber(bench.followers)}</p>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <span className={cn("inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-bold", bench.followerGrowth >= 2.0 ? "bg-emerald-50 text-emerald-600" : "bg-slate-100 text-slate-600")}>
                          {bench.followerGrowth > 0 ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
                          {bench.followerGrowth}%
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <p className="text-sm font-semibold text-slate-700">{formatNumber(bench.engagements)}</p>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <p className="text-sm font-semibold text-slate-700">{bench.postsPublished}</p>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
}
