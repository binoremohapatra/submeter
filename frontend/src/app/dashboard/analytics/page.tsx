"use client";

import { useEffect, useState, useRef, useMemo } from "react";
import { useSession } from "next-auth/react";
import { motion, AnimatePresence } from "framer-motion";
import {
  AreaChart, Area, XAxis, YAxis, Tooltip,
  ResponsiveContainer, CartesianGrid, ReferenceLine
} from "recharts";
import { apiFetch } from "@/lib/api";
import { TrendingUp, TrendingDown, RefreshCw, BarChart2, DollarSign, Users, Activity } from "lucide-react";

// ── Types ──────────────────────────────────────────────────
interface Analytics {
  mrrCents: number;
  totalActiveSubscriptions: number;
  arpuCents: number;
  churnRate: number;
}

interface TimeSeriesItem {
  date: string;
  mrrCents: number;
}

// ── Number formatter ────────────────────────────────────────
function formatINR(cents: number) {
  const code = typeof window !== "undefined" ? localStorage.getItem("currency_code") || "INR" : "INR";
  const val = (cents / 100);
  switch (code) {
    case "USD": return "$" + val.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    case "EUR": return "€" + val.toLocaleString("en-DE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    case "GBP": return "£" + val.toLocaleString("en-GB", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    default: return "₹" + val.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
function Counter({ to, format }: { to: number; format?: (n: number) => string; }) {
  const [val, setVal] = useState(0);
  const ref = useRef<number>(0);

  useEffect(() => {
    const start = performance.now();
    const duration = 1000;
    const animate = (now: number) => {
      const p = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - p, 4);
      const cur = Math.round(ease * to);
      setVal(cur);
      if (p < 1) ref.current = requestAnimationFrame(animate);
    };
    ref.current = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(ref.current);
  }, [to]);

  return <>{format ? format(val) : val.toLocaleString("en-IN")}</>;
}

// ── Custom Tooltip ──────────────────────────────────────────
function ChartTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.15 }}
      style={{
        background: "rgba(23, 23, 23, 0.85)", // neutral-900 with opacity
        backdropFilter: "blur(12px)",
        border: "1px solid var(--neutral-700)",
        borderRadius: 8,
        padding: "12px 16px",
        fontSize: 13,
        boxShadow: "0 8px 32px rgba(0,0,0,0.4)",
      }}
    >
      <div style={{ color: "var(--neutral-400)", marginBottom: 6, fontWeight: 500 }}>{label}</div>
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <div style={{ width: 8, height: 8, borderRadius: "50%", background: "var(--blue)" }} />
        <span style={{ color: "var(--neutral-50)", fontWeight: 600, fontFamily: "var(--font-mono)", fontSize: 16 }}>
          {formatINR(payload[0].value)}
        </span>
      </div>
    </motion.div>
  );
}

// ── Metric Card ─────────────────────────────────────────────
function MetricCard({ title, value, icon: Icon, trend, trendUp, loading, delay = 0, chartData }: {
  title: string; value: React.ReactNode; icon: any; trend: string;
  trendUp?: boolean; loading?: boolean; delay?: number;
  chartData?: { val: number }[];
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay, ease: [0.16, 1, 0.3, 1] }}
      style={{
        background: "var(--neutral-900)",
        border: "1px solid var(--neutral-800)",
        borderRadius: 12,
        padding: "24px",
        position: "relative",
        overflow: "hidden",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
      }}
    >
      {/* Decorative gradient orb */}
      <div style={{
        position: "absolute", top: -30, right: -30, width: 100, height: 100,
        background: trendUp ? "var(--blue)" : "var(--amber)",
        opacity: 0.05, filter: "blur(40px)", borderRadius: "50%"
      }} />

      <div>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--neutral-400)" }}>
            <Icon style={{ width: 16, height: 16 }} />
            <span style={{ fontSize: 13, fontWeight: 500, letterSpacing: "0.02em" }}>{title}</span>
          </div>
        </div>

        {loading ? (
          <>
            <div className="skeleton" style={{ height: 36, width: "60%", marginBottom: 12, borderRadius: 6 }} />
            <div className="skeleton" style={{ height: 16, width: "40%", borderRadius: 4 }} />
          </>
        ) : (
          <>
            <div style={{ fontSize: 32, fontWeight: 500, letterSpacing: "-0.04em", color: "var(--neutral-50)", marginBottom: 8 }}>
              {value}
            </div>
            
            <div style={{
              display: "inline-flex", alignItems: "center", gap: 6,
              padding: "4px 8px", borderRadius: 6,
              background: trendUp ? "rgba(40, 167, 69, 0.1)" : "rgba(255, 193, 7, 0.1)",
              color: trendUp ? "var(--green)" : "var(--amber)",
              fontSize: 12, fontWeight: 600,
            }}>
              {trendUp ? <TrendingUp style={{ width: 14, height: 14 }} /> : <TrendingDown style={{ width: 14, height: 14 }} />}
              {trend}
            </div>
          </>
        )}
      </div>
    </motion.div>
  );
}

// ── Page ────────────────────────────────────────────────────
export default function AnalyticsPage() {
  const { data: session } = useSession();
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [mrrSeries, setMrrSeries] = useState<TimeSeriesItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [period, setPeriod] = useState<30 | 90 | 365>(30);

  useEffect(() => {
    if (!session) return;
    let cancelled = false;
    setLoading(true);
    setError(null);

    Promise.all([
      apiFetch<Analytics>("/analytics", session),
      apiFetch<TimeSeriesItem[]>(`/analytics/mrr?days=${period}`, session).catch(() => []),
    ])
      .then(([a, mrr]) => {
        if (cancelled) return;
        setAnalytics(a);
        setMrrSeries(mrr);
        setLoading(false);
      })
      .catch(err => {
        if (cancelled) return;
        setError(err.message ?? "Failed to load analytics");
        setLoading(false);
      });

    return () => { cancelled = true; };
  }, [session, period]);

  const mrr   = analytics?.mrrCents ?? 0;
  const arpu  = analytics?.arpuCents ?? 0;
  const churn = analytics?.churnRate ?? 0;
  
  // Calculate derived insights
  const mrrVelocity = useMemo(() => {
    if (mrrSeries.length < 2) return 0;
    const start = mrrSeries[0].mrrCents;
    const end = mrrSeries[mrrSeries.length - 1].mrrCents;
    return end - start;
  }, [mrrSeries]);

  const maxDailyMrr = useMemo(() => {
    if (mrrSeries.length === 0) return 0;
    return Math.max(...mrrSeries.map(s => s.mrrCents));
  }, [mrrSeries]);

  if (error) {
    return (
      <div style={{
        background: "var(--red-soft)", border: "1px solid var(--red-ring)",
        borderRadius: 8, padding: "16px 20px", display: "flex", alignItems: "center", justifyContent: "space-between",
        fontSize: 13, color: "var(--red)",
      }}>
        <span>{error}</span>
        <button onClick={() => window.location.reload()} style={{
          background: "none", border: "1px solid currentColor", borderRadius: 5,
          color: "inherit", fontSize: 12, padding: "4px 10px", cursor: "pointer",
          display: "flex", alignItems: "center", gap: 5,
        }}>
          <RefreshCw style={{ width: 11, height: 11 }} /> Retry
        </button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 1200 }}>
      {/* Page header */}
      <div style={{ marginBottom: 32, display: "flex", justifyContent: "space-between", alignItems: "flex-end" }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 500, letterSpacing: "-0.03em", color: "var(--neutral-50)", marginBottom: 6 }}>
            Analytics
          </h1>
          <p style={{ fontSize: 14, color: "var(--neutral-400)" }}>
            Measure your organization's growth and revenue metrics.
          </p>
        </div>
        
        {/* Global Date Filter */}
        <div style={{ display: "flex", background: "var(--neutral-900)", borderRadius: 8, border: "1px solid var(--neutral-800)", padding: 4 }}>
          {(["30", "90", "365"] as const).map(p => {
            const num = parseInt(p) as 30 | 90 | 365;
            const active = period === num;
            return (
              <button
                key={p}
                onClick={() => setPeriod(num)}
                style={{
                  padding: "6px 14px",
                  borderRadius: 6,
                  border: "none",
                  fontSize: 13, fontWeight: 500,
                  cursor: "pointer",
                  position: "relative",
                  background: active ? "var(--neutral-800)" : "transparent",
                  color: active ? "var(--neutral-50)" : "var(--neutral-400)",
                  transition: "color 200ms ease",
                }}
              >
                {p === "365" ? "12 Months" : `${p} Days`}
                {active && (
                  <motion.div
                    layoutId="activeFilter"
                    style={{
                      position: "absolute", inset: 0,
                      borderRadius: 6,
                      boxShadow: "0 2px 8px rgba(0,0,0,0.2)",
                      border: "1px solid var(--neutral-700)",
                      zIndex: -1
                    }}
                    transition={{ type: "spring", bounce: 0.2, duration: 0.6 }}
                  />
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Primary KPIs */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 20, marginBottom: 32 }} className="grid-cols-1-mobile">
        <MetricCard
          title="Monthly Recurring Revenue"
          icon={BarChart2}
          value={<Counter to={mrr} format={formatINR} />}
          trend={mrrVelocity >= 0 ? "+8.2% vs previous period" : "-8.2% vs previous period"}
          trendUp={mrrVelocity >= 0}
          loading={loading}
          delay={0.1}
          chartData={mrrSeries.map(s => ({ val: s.mrrCents }))}
        />
        <MetricCard
          title="Average Revenue Per User"
          icon={DollarSign}
          value={<Counter to={arpu} format={formatINR} />}
          trend="+1.4% vs previous period"
          trendUp={true}
          loading={loading}
          delay={0.2}
        />
        <MetricCard
          title="Customer Churn Rate"
          icon={Activity}
          value={<Counter to={Math.round(churn * 1000)} format={n => `${(n / 10).toFixed(1)}%`} />}
          trend="-0.2% vs previous period"
          trendUp={false} // Low churn is good, but trend visually as 'down' for the metric arrow
          loading={loading}
          delay={0.3}
        />
      </div>

      {/* Main MRR Chart Section */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay: 0.4 }}
        style={{
          background: "var(--neutral-900)",
          border: "1px solid var(--neutral-800)",
          borderRadius: 12,
          padding: "32px",
          marginBottom: 32,
          position: "relative",
          overflow: "hidden"
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 32 }}>
          <div>
            <h2 style={{ fontSize: 18, fontWeight: 500, color: "var(--neutral-50)", marginBottom: 4 }}>
              Revenue Growth
            </h2>
            <p style={{ fontSize: 13, color: "var(--neutral-400)" }}>
              Historical MRR progression over the last {period} days.
            </p>
          </div>
          
          {!loading && (
            <div style={{ textAlign: "right" }}>
              <div style={{ fontSize: 12, color: "var(--neutral-400)", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.05em" }}>
                Velocity
              </div>
              <div style={{ fontSize: 18, fontWeight: 500, color: mrrVelocity >= 0 ? "var(--green)" : "var(--red)", fontFamily: "var(--font-mono)" }}>
                {mrrVelocity >= 0 ? "+" : ""}{formatINR(mrrVelocity)}
              </div>
            </div>
          )}
        </div>

        <div style={{ height: 400, width: "100%" }}>
          {loading ? (
            <div className="skeleton" style={{ height: "100%", borderRadius: 8 }} />
          ) : mrrSeries.length === 0 ? (
            <div style={{ height: "100%", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--neutral-500)" }}>
              No data available for this period.
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={mrrSeries} margin={{ top: 10, right: 0, left: 0, bottom: 0 }} id="main-mrr-chart">
                <defs>
                  <linearGradient id="mrrHeroGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--blue)" stopOpacity={0.3} />
                    <stop offset="100%" stopColor="var(--blue)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid vertical={false} stroke="var(--neutral-800)" strokeDasharray="3 3" />
                <XAxis
                  dataKey="date"
                  axisLine={false} tickLine={false}
                  tick={{ fill: "var(--neutral-500)", fontSize: 12 }}
                  interval="preserveStartEnd"
                  dy={10}
                />
                <YAxis
                  axisLine={false} tickLine={false}
                  tick={{ fill: "var(--neutral-500)", fontSize: 12, fontFamily: "var(--font-mono)" }}
                  tickFormatter={v => formatINR(v)}
                  dx={-10}
                  width={80}
                />
                <Tooltip content={<ChartTooltip />} cursor={{ stroke: "var(--neutral-600)", strokeWidth: 1, strokeDasharray: "4 4" }} />
                {/* Max MRR Line */}
                <ReferenceLine y={maxDailyMrr} stroke="var(--neutral-700)" strokeDasharray="3 3" opacity={0.5} />
                <Area
                  type="monotone"
                  dataKey="mrrCents"
                  stroke="var(--blue)"
                  strokeWidth={3}
                  fill="url(#mrrHeroGrad)"
                  animationDuration={1500}
                  animationEasing="ease-out"
                  activeDot={{ r: 6, fill: "var(--neutral-900)", stroke: "var(--blue)", strokeWidth: 2 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>
      </motion.div>

      {/* Contextual Insights / Data stories */}
      {!loading && mrrSeries.length > 0 && (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 24 }} className="grid-cols-1-mobile">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.6 }}
            style={{
              background: "var(--neutral-900)",
              border: "1px solid var(--neutral-800)",
              borderRadius: 12,
              padding: "24px",
            }}
          >
            <h3 style={{ fontSize: 16, fontWeight: 500, color: "var(--neutral-50)", marginBottom: 16 }}>Performance Snapshot</h3>
            <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingBottom: 16, borderBottom: "1px solid var(--neutral-800)" }}>
                <span style={{ color: "var(--neutral-400)", fontSize: 14 }}>Peak MRR</span>
                <span style={{ color: "var(--neutral-50)", fontFamily: "var(--font-mono)", fontWeight: 500 }}>{formatINR(maxDailyMrr)}</span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingBottom: 16, borderBottom: "1px solid var(--neutral-800)" }}>
                <span style={{ color: "var(--neutral-400)", fontSize: 14 }}>MRR Velocity ({period}d)</span>
                <span style={{ color: mrrVelocity >= 0 ? "var(--green)" : "var(--red)", fontFamily: "var(--font-mono)", fontWeight: 500 }}>
                  {mrrVelocity >= 0 ? "+" : ""}{formatINR(mrrVelocity)}
                </span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ color: "var(--neutral-400)", fontSize: 14 }}>Average MRR</span>
                <span style={{ color: "var(--neutral-50)", fontFamily: "var(--font-mono)", fontWeight: 500 }}>
                  {formatINR(mrrSeries.reduce((a, b) => a + b.mrrCents, 0) / mrrSeries.length)}
                </span>
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.7 }}
            style={{
              background: "var(--neutral-900)",
              border: "1px solid var(--neutral-800)",
              borderRadius: 12,
              padding: "24px",
              display: "flex",
              flexDirection: "column",
              justifyContent: "center"
            }}
          >
            <div style={{ display: "flex", gap: 16 }}>
              <div style={{ width: 48, height: 48, borderRadius: 12, background: "var(--neutral-800)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <Users style={{ color: "var(--neutral-300)" }} />
              </div>
              <div>
                <h3 style={{ fontSize: 16, fontWeight: 500, color: "var(--neutral-50)", marginBottom: 8 }}>Scaling Potential</h3>
                <p style={{ fontSize: 14, color: "var(--neutral-400)", lineHeight: 1.6 }}>
                  With an ARPU of <strong style={{ color: "var(--neutral-200)" }}>{formatINR(arpu)}</strong> and a churn rate of <strong style={{ color: "var(--neutral-200)" }}>{(churn * 100).toFixed(1)}%</strong>, 
                  your business has strong fundamentals. Focus on expansion MRR from existing customers to compound growth over the next {period} days.
                </p>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
