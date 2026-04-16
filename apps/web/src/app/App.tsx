import { lazy, Suspense } from "react";
import { Link, Route, Routes } from "react-router-dom";
import { Badge, Button, Card, CardDescription, CardTitle } from "@nexora/ui";
import { useQuery } from "@tanstack/react-query";
import { getBillingPlans } from "@/lib/api";

import { AppShell, MarketingShell } from "@/components/layout/app-shell";
import {
  AcceptInvitePage,
  LoginPage,
  RequestPasswordResetPage,
  ResetPasswordPage,
  SignUpPage,
  VerifyEmailPage
} from "@/features/auth/auth-pages";
import { RedirectAuthenticated, RequireAuth } from "@/features/auth/auth-context";
import {
  featureHighlights,
  platformCards,
  pricingPlans as staticPricingPlans,
  socialProofStats
} from "@/lib/data";

// Lazy Loaded Application Features
const AnalyticsPage = lazy(() => import("@/features/analytics/analytics-page").then(m => ({ default: m.AnalyticsPage })));
const CalendarPage = lazy(() => import("@/features/calendar/calendar-page").then(m => ({ default: m.CalendarPage })));
const ComposerPage = lazy(() => import("@/features/composer/composer-page").then(m => ({ default: m.ComposerPage })));
const DashboardPage = lazy(() => import("@/features/dashboard/dashboard-page").then(m => ({ default: m.DashboardPage })));
const SettingsPage = lazy(() => import("@/features/settings/settings-page").then(m => ({ default: m.SettingsPage })));
const TeamPage = lazy(() => import("@/features/team/team-page").then(m => ({ default: m.TeamPage })));

// ── Fallback Loading State ────────────────────────────────────────────────
function PageLoader() {
  return (
    <div className="flex h-full min-h-[400px] w-full items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-sky-500" />
        <p className="text-sm font-semibold text-slate-500 tracking-widest uppercase">Loading</p>
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════════════════
   Landing Page
   ═══════════════════════════════════════════════════════════════════════════ */

function LandingPage() {
  return (
    <div className="animate-fade-in">
      {/* ── Hero ───────────────────────────────────────────────────── */}
      <section className="grid items-center gap-12 py-8 lg:grid-cols-[1.15fr_0.85fr] lg:py-16">
        <div>
          <Badge className="animate-fade-in-down">Scalable social operations</Badge>
          <h1 className="mt-6 max-w-[600px] font-display text-[3.2rem] leading-[1.1] tracking-tight lg:text-[4.2rem]">
            <span className="text-gradient">Command center</span>{" "}
            <span className="text-slate-950">for modern social teams.</span>
          </h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-slate-500">
            Nexora brings scheduling, approvals, analytics, inbox workflows, and AI modules
            into a refined operating system for brands and agencies.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/auth/signup">
              <Button className="shadow-lg shadow-sky-600/25 transition-all hover:scale-[1.03] hover:shadow-sky-600/35">
                Start Your Workspace
              </Button>
            </Link>
            <Link to="/pricing">
              <Button variant="secondary" className="hover-lift">See Pricing</Button>
            </Link>
          </div>
          {/* ── Mini Stats ──────────────────────────────────────────── */}
          <div className="mt-12 flex flex-wrap gap-8">
            {socialProofStats.slice(0, 3).map((stat) => (
              <div key={stat.label} className="animate-fade-in-up">
                <p className="font-display text-3xl font-semibold text-slate-950">{stat.value}</p>
                <p className="mt-1 text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>

        {/* ── Platform Preview Card ──────────────────────────────── */}
        <Card className="glass-dark bg-slate-950 text-white hover-lift border-white/5 animate-slide-in-right">
          <CardTitle className="text-white">Platform Preview</CardTitle>
          <CardDescription className="mt-2 text-slate-400">
            Cloud-native architecture supporting 6+ social networks.
          </CardDescription>
          <div className="mt-6 space-y-3 stagger">
            {platformCards.map((platform) => (
              <div
                className={`rounded-2xl bg-gradient-to-r ${platform.gradient} border border-white/8 px-5 py-4 transition-all hover:border-white/15 hover:bg-white/5 cursor-pointer`}
                key={platform.name}
              >
                <div className="flex items-center gap-3">
                  <span className="text-xl">{platform.icon}</span>
                  <p className="text-sm font-semibold">{platform.name}</p>
                </div>
                <p className="mt-2 text-sm leading-6 text-slate-400">{platform.detail}</p>
              </div>
            ))}
          </div>
        </Card>
      </section>

      {/* ── Features Grid ──────────────────────────────────────────── */}
      <section className="py-20">
        <div className="text-center">
          <Badge>Features</Badge>
          <h2 className="mt-6 font-display text-4xl text-slate-950 lg:text-5xl">
            Everything your team needs
          </h2>
          <p className="mx-auto mt-4 max-w-xl text-lg text-slate-500">
            From content creation to performance reporting — one platform to manage it all.
          </p>
        </div>
        <div className="mt-14 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 stagger">
          {featureHighlights.map((feature) => (
            <Card key={feature.title} className="group hover-lift glow-border cursor-pointer">
              <div className={`mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br ${feature.gradient} text-xl shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3`}>
                {feature.icon}
              </div>
              <CardTitle className="text-lg">{feature.title}</CardTitle>
              <CardDescription className="mt-2">{feature.description}</CardDescription>
            </Card>
          ))}
        </div>
      </section>

      {/* ── Social Proof ───────────────────────────────────────────── */}
      <section className="py-16">
        <Card className="glass-dark bg-slate-950 text-white border-white/5">
          <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
            {socialProofStats.map((stat) => (
              <div key={stat.label} className="text-center">
                <p className="font-display text-4xl font-semibold text-white stat-number">{stat.value}</p>
                <p className="mt-2 text-sm font-semibold text-sky-400">{stat.label}</p>
                <p className="mt-1 text-xs text-slate-500">{stat.detail}</p>
              </div>
            ))}
          </div>
        </Card>
      </section>

      {/* ── CTA ────────────────────────────────────────────────────── */}
      <section className="py-16 text-center">
        <h2 className="font-display text-4xl text-slate-950">Ready to elevate your workflow?</h2>
        <p className="mx-auto mt-4 max-w-md text-lg text-slate-500">
          Join thousands of teams who trust Nexora for their social media operations.
        </p>
        <div className="mt-8 flex justify-center gap-3">
          <Link to="/auth/signup">
            <Button className="shadow-lg shadow-sky-600/25 transition-all hover:scale-[1.03]">
              Get Started Free
            </Button>
          </Link>
        </div>
      </section>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════════════════
   Pricing Page
   ═══════════════════════════════════════════════════════════════════════════ */

function PricingPage() {
  const { data: plans } = useQuery({
    queryKey: ["billing-plans"],
    queryFn: getBillingPlans,
    retry: 1
  });

  const displayPlans = plans?.length 
    ? plans.map(p => ({
        name: p.name,
        price: p.priceMinor === 0 ? "Free" : `$${p.priceMinor / 100}`,
        period: p.priceMinor === 0 ? "" : p.interval === 'monthly' ? "/mo" : "/yr",
        description: p.features?.description || "A powerful plan for your team.",
        cta: p.priceMinor === 0 ? "Start for free" : "Start 14-day free trial",
        popular: p.code === 'pro',
        features: Object.entries(p.features)
          .filter(([k]) => k !== 'description')
          .map(([k, v]) => `${k.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}: ${v === -1 ? 'Unlimited' : v === true ? 'Yes' : v}`)
      }))
    : staticPricingPlans;

  return (
    <div className="py-8 animate-fade-in">
      <div className="text-center">
        <Badge>Pricing</Badge>
        <h1 className="mt-6 font-display text-5xl text-slate-950">Plans that scale with your team</h1>
        <p className="mx-auto mt-4 max-w-xl text-lg text-slate-500">
          Start free, upgrade when you need more power. All plans include 14-day trials.
        </p>
      </div>

      <section className="mt-14 grid gap-5 lg:grid-cols-3 stagger">
        {displayPlans.map((plan) => (
          <Card
            key={plan.name}
            className={`relative hover-lift glow-border ${
              plan.popular
                ? "glass-dark bg-slate-950 text-white border-sky-500/20 shadow-glow-lg scale-[1.03]"
                : "bg-white/90"
            }`}
          >
            {plan.popular && (
              <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                <span className="rounded-full bg-gradient-to-r from-sky-500 to-sky-600 px-4 py-1.5 text-[11px] font-bold uppercase tracking-[0.2em] text-white shadow-lg shadow-sky-500/30">
                  Most Popular
                </span>
              </div>
            )}
            <div className="pt-2">
              <p className={`text-sm font-bold uppercase tracking-[0.2em] ${plan.popular ? "text-sky-400" : "text-sky-600"}`}>
                {plan.name}
              </p>
              <div className="mt-4 flex items-baseline gap-1">
                <span className={`font-display text-5xl font-semibold ${plan.popular ? "text-white" : "text-slate-950"}`}>
                  {plan.price}
                </span>
                <span className={`text-sm ${plan.popular ? "text-slate-400" : "text-slate-500"}`}>
                  {plan.period}
                </span>
              </div>
              <p className={`mt-3 text-sm leading-6 ${plan.popular ? "text-slate-300" : "text-slate-500"}`}>
                {plan.description}
              </p>
            </div>

            <div className="mt-6 space-y-3">
              {plan.features.map((feature) => (
                <div key={feature} className="flex items-center gap-2.5">
                  <div className={`flex h-5 w-5 items-center justify-center rounded-lg ${
                    plan.popular ? "bg-sky-500/20 text-sky-400" : "bg-sky-100 text-sky-600"
                  }`}>
                    <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <span className={`text-sm ${plan.popular ? "text-slate-300" : "text-slate-600"}`}>{feature}</span>
                </div>
              ))}
            </div>

            <div className="mt-8">
              <Link to="/auth/signup">
                <Button
                  variant={plan.popular ? "primary" : "secondary"}
                  className={`w-full ${plan.popular ? "shadow-lg shadow-sky-600/25" : ""}`}
                >
                  {plan.cta}
                </Button>
              </Link>
            </div>
          </Card>
        ))}
      </section>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════════════════
   Router
   ═══════════════════════════════════════════════════════════════════════════ */

export function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route element={<MarketingShell />}>
          <Route element={<LandingPage />} path="/" />
          <Route element={<PricingPage />} path="/pricing" />
        </Route>
        <Route element={<RedirectAuthenticated />} path="/auth">
          <Route element={<LoginPage />} path="login" />
          <Route element={<SignUpPage />} path="signup" />
          <Route element={<RequestPasswordResetPage />} path="password/request" />
          <Route element={<ResetPasswordPage />} path="password/reset" />
        </Route>
        <Route element={<VerifyEmailPage />} path="/auth/verify" />
        <Route element={<AcceptInvitePage />} path="/auth/invite/accept" />
        <Route element={<RequireAuth />} path="/app">
          <Route element={<AppShell />}>
            <Route element={<DashboardPage />} path="dashboard" />
            <Route element={<CalendarPage />} path="calendar" />
            <Route element={<ComposerPage />} path="composer" />
            <Route element={<AnalyticsPage />} path="analytics" />
            <Route element={<TeamPage />} path="team" />
            <Route element={<SettingsPage />} path="settings" />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  );
}

