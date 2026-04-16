import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Badge, Card, CardTitle, cn } from "@nexora/ui";
import { ChevronLeft, ChevronRight, Clock, CalendarDays } from "lucide-react";

import { useAuth } from "@/features/auth/auth-context";
import { listCalendarEntries } from "@/lib/api";

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

const statusColors: Record<string, string> = {
  scheduled: "bg-sky-500",
  published: "bg-emerald-500",
  publishing: "bg-amber-500",
  failed: "bg-red-500",
  draft: "bg-slate-400",
  pending_approval: "bg-violet-500",
  approved: "bg-teal-500",
  cancelled: "bg-slate-300"
};

const statusBg: Record<string, string> = {
  scheduled: "bg-sky-50 border-sky-200 text-sky-700",
  published: "bg-emerald-50 border-emerald-200 text-emerald-700",
  publishing: "bg-amber-50 border-amber-200 text-amber-700",
  failed: "bg-red-50 border-red-200 text-red-700",
  draft: "bg-slate-50 border-slate-200 text-slate-600",
  pending_approval: "bg-violet-50 border-violet-200 text-violet-700",
  approved: "bg-teal-50 border-teal-200 text-teal-700",
  cancelled: "bg-slate-50 border-slate-200 text-slate-400"
};

export function CalendarPage() {
  const { session } = useAuth();
  const accessToken = session?.session?.accessToken ?? "";
  const workspaceId = session?.currentWorkspace?.workspaceId ?? "";

  const [currentDate, setCurrentDate] = useState(new Date());
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const { firstDay, daysInMonth, monthLabel, from, to } = useMemo(() => {
    const start = new Date(year, month, 1);
    const end = new Date(year, month + 1, 0);
    return {
      firstDay: start.getDay(),
      daysInMonth: end.getDate(),
      monthLabel: start.toLocaleDateString("en-US", { month: "long", year: "numeric" }),
      from: start.toISOString(),
      to: new Date(year, month + 1, 0, 23, 59, 59).toISOString()
    };
  }, [year, month]);

  const { data: entries = [] } = useQuery({
    queryKey: ["calendar", workspaceId, from, to],
    queryFn: () => listCalendarEntries(workspaceId, accessToken, from, to),
    enabled: !!workspaceId && !!accessToken
  });

  const entriesByDay = useMemo(() => {
    const map = new Map<number, typeof entries>();
    for (const entry of entries) {
      const day = new Date(entry.scheduledFor).getDate();
      const existing = map.get(day) ?? [];
      existing.push(entry);
      map.set(day, existing);
    }
    return map;
  }, [entries]);

  const navigate = (delta: number) => {
    setCurrentDate(new Date(year, month + delta, 1));
  };

  const today = new Date();
  const isToday = (day: number) =>
    today.getFullYear() === year && today.getMonth() === month && today.getDate() === day;

  const calendarCells = [];
  for (let i = 0; i < firstDay; i++) {
    calendarCells.push(<div key={`empty-${i}`} className="min-h-[100px] rounded-2xl" />);
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const dayEntries = entriesByDay.get(day) ?? [];
    calendarCells.push(
      <div
        key={day}
        className={cn(
          "group min-h-[100px] rounded-2xl border p-2 transition-all duration-200 hover:border-sky-200 hover:shadow-sm cursor-pointer",
          isToday(day)
            ? "border-sky-300 bg-sky-50/40 shadow-sm"
            : "border-slate-100/80 bg-white/40 hover:bg-white/70"
        )}
      >
        <div className="flex items-center justify-between mb-1">
          <span
            className={cn(
              "flex h-7 w-7 items-center justify-center rounded-lg text-xs font-bold",
              isToday(day)
                ? "bg-sky-500 text-white shadow-lg shadow-sky-500/30"
                : "text-slate-600 group-hover:bg-slate-100"
            )}
          >
            {day}
          </span>
          {dayEntries.length > 0 && (
            <span className="text-[10px] font-semibold text-slate-400">{dayEntries.length}</span>
          )}
        </div>
        <div className="space-y-1">
          {dayEntries.slice(0, 3).map((entry) => (
            <div
              key={entry.scheduledJobId}
              className={cn(
                "flex items-center gap-1.5 rounded-lg border px-1.5 py-1 text-[10px] font-medium transition-all hover:shadow-sm",
                statusBg[entry.lifecycleStatus] ?? statusBg.draft
              )}
            >
              <div className={cn("h-1.5 w-1.5 shrink-0 rounded-full", statusColors[entry.lifecycleStatus] ?? statusColors.draft)} />
              <span className="truncate">{entry.title}</span>
            </div>
          ))}
          {dayEntries.length > 3 && (
            <span className="block text-center text-[10px] font-semibold text-slate-400">
              +{dayEntries.length - 3} more
            </span>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ── Header ──────────────────────────────────────────────── */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="font-display text-2xl text-slate-950">Content Calendar</h2>
          <p className="mt-1 text-sm text-slate-500">Schedule and visualize your publishing timeline.</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 rounded-2xl border border-slate-200/60 bg-white/60 p-1 backdrop-blur">
            <button
              className="rounded-xl p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700"
              onClick={() => navigate(-1)}
              type="button"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="min-w-[140px] text-center text-sm font-bold text-slate-800">{monthLabel}</span>
            <button
              className="rounded-xl p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700"
              onClick={() => navigate(1)}
              type="button"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
          <button
            className="rounded-2xl border border-slate-200/60 bg-white/60 px-4 py-2.5 text-xs font-semibold text-slate-600 backdrop-blur transition-all hover:border-sky-200 hover:shadow-sm"
            onClick={() => setCurrentDate(new Date())}
            type="button"
          >
            Today
          </button>
        </div>
      </div>

      {/* ── Legend ───────────────────────────────────────────────── */}
      <div className="flex flex-wrap gap-4">
        {[
          { label: "Scheduled", color: "bg-sky-500" },
          { label: "Published", color: "bg-emerald-500" },
          { label: "Pending", color: "bg-violet-500" },
          { label: "Draft", color: "bg-slate-400" },
          { label: "Failed", color: "bg-red-500" }
        ].map((item) => (
          <div key={item.label} className="flex items-center gap-1.5">
            <div className={cn("h-2.5 w-2.5 rounded-full", item.color)} />
            <span className="text-xs font-semibold text-slate-500">{item.label}</span>
          </div>
        ))}
      </div>

      {/* ── Calendar Grid ───────────────────────────────────────── */}
      <Card className="!p-4 overflow-hidden">
        {/* Day Headers */}
        <div className="grid grid-cols-7 gap-1 mb-2">
          {DAYS.map((day) => (
            <div key={day} className="py-2 text-center text-[10px] font-bold uppercase tracking-[0.25em] text-slate-400">
              {day}
            </div>
          ))}
        </div>
        {/* Calendar Cells */}
        <div className="grid grid-cols-7 gap-1">
          {calendarCells}
        </div>
      </Card>

      {/* ── Upcoming Summary ────────────────────────────────────── */}
      {entries.length > 0 && (
        <Card className="hover-lift">
          <div className="flex items-center gap-2 mb-4">
            <CalendarDays className="h-4 w-4 text-sky-600" />
            <CardTitle className="text-lg">Upcoming This Month</CardTitle>
            <Badge>{entries.length} posts</Badge>
          </div>
          <div className="space-y-2 stagger">
            {entries.slice(0, 5).map((entry) => (
              <div
                key={entry.scheduledJobId}
                className="flex items-center justify-between rounded-2xl border border-slate-100 px-4 py-3 transition-all hover:border-sky-200/60 hover:bg-sky-50/20 hover:shadow-sm cursor-pointer"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className={cn("h-2.5 w-2.5 shrink-0 rounded-full", statusColors[entry.lifecycleStatus] ?? statusColors.draft)} />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-slate-800">{entry.title}</p>
                    <div className="mt-0.5 flex items-center gap-1.5 text-xs text-slate-400">
                      <Clock className="h-3 w-3" />
                      {new Date(entry.scheduledFor).toLocaleDateString("en-IN", {
                        month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
                      })}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {entry.channels.map((ch) => (
                    <span key={ch} className="rounded-lg bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-500">{ch}</span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
