"use client";

import { useEffect, useState, useRef } from "react";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import {
  AreaChart, Area, XAxis, YAxis, Tooltip,
  ResponsiveContainer, CartesianGrid
} from "recharts";
import { apiFetch, formatDate } from "@/lib/api";
import { StatusBadge } from "@/components/ui/status-dot";
import { TrendingUp, TrendingDown, ArrowUpRight, RefreshCw, ChevronRight } from "lucide-react";

// ── Types ──────────────────────────────────────────────────
interface Analytics {
  mrrCents: number;
  totalActiveSubscriptions: number;
  arpuCents: number;
  churnRate: number;
}

interface InvoiceRow {
  id: string;
  invoiceNumber: string;
  customerName: string;
  periodStart: string;
  totalCents: number;
  status: "DRAFT" | "OPEN" | "PAID" | "VOID" | "UNCOLLECTIBLE";
  dueAt: string;
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
// ── Counter — spring count-up with reduced-motion guard ──────────────────
function Counter({ to, prefix = "", suffix = "", format }: {
  to: number; prefix?: string; suffix?: string;
  format?: (n: number) => string;
}) {
  const shouldReduceMotion = useReducedMotion();
  const [val, setVal] = useState(shouldReduceMotion ? to : 0);
  const ref = useRef<number>(0);

  useEffect(() => {
    // Skip animation if user prefers reduced motion
    if (shouldReduceMotion) {
      setVal(to);
      return;
    }
    const start = performance.now();
    const duration = 800;
    const animate = (now: number) => {
      const p = Math.min((now - start) / duration, 1);
      const ease = 1 - Math.pow(1 - p, 3);
      const cur = Math.round(ease * to);
      setVal(cur);
      if (p < 1) ref.current = requestAnimationFrame(animate);
    };
    ref.current = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(ref.current);
  }, [to, shouldReduceMotion]);

  const display = format ? format(val) : `${prefix}${val.toLocaleString("en-IN")}${suffix}`;
  return <>{display}</>;
}

// ── Custom Tooltip ──────────────────────────────────────────
function ChartTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: "var(--neutral-800)",
      border: "1px solid var(--neutral-700)",
      borderRadius: 6,
      padding: "8px 12px",
      fontSize: 12,
      boxShadow: "0 4px 16px rgba(0,0,0,0.4)",
    }}>
      <div style={{ color: "var(--neutral-500)", marginBottom: 4 }}>{label}</div>
      <div style={{ color: "var(--neutral-50)", fontWeight: 500, fontFamily: "var(--font-mono)" }}>
        {formatINR(payload[0].value)}
      </div>
    </div>
  );
}

// ── KPI Card ─────────────────────────────────────────────────
function KpiCard({ title, value, trend, trendUp, loading, delay = 0 }: {
  title: string; value: React.ReactNode; trend: string;
  trendUp?: boolean; loading?: boolean; delay?: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay, ease: [0.16, 1, 0.3, 1] }}
      style={{
        background: "var(--neutral-900)",
        border: "1px solid var(--neutral-800)",
        borderRadius: 8,
        padding: "20px 24px",
      }}
    >
      <div style={{ fontSize: 11, color: "var(--neutral-500)", letterSpacing: "0.07em", textTransform: "uppercase", fontWeight: 500, marginBottom: 10 }}>
        {title}
      </div>

      {loading ? (
        <>
          <div className="skeleton" style={{ height: 28, width: "55%", marginBottom: 8, borderRadius: 4 }} />
          <div className="skeleton" style={{ height: 14, width: "35%", borderRadius: 4 }} />
        </>
      ) : (
        <>
          <div style={{ fontSize: 24, fontWeight: 500, letterSpacing: "-0.03em", color: "var(--neutral-50)", marginBottom: 8 }}>
            {value}
          </div>
          <div style={{
            display: "flex", alignItems: "center", gap: 4,
            fontSize: 12, fontWeight: 500,
            color: trendUp === undefined ? "var(--neutral-400)" : trendUp ? "var(--green)" : "var(--red)",
          }}>
            {trendUp !== undefined && (
              // One-time bounce-in on mount — scale 0.5 → 1.25 → 1
              <motion.span
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ scale: [0.5, 1.25, 1], opacity: 1 }}
                transition={{ duration: 0.45, delay: delay + 0.3, ease: [0.16, 1, 0.3, 1] }}
                style={{ display: "inline-flex", lineHeight: 0 }}
              >
                {trendUp
                  ? <TrendingUp style={{ width: 12, height: 12 }} />
                  : <TrendingDown style={{ width: 12, height: 12 }} />}
              </motion.span>
            )}
            {trend}
          </div>
        </>
      )}
    </motion.div>
  );
}

// ── Page ─────────────────────────────────────────────────────

export default function DashboardOverview() {
  const { data: session } = useSession();
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [mrrSeries, setMrrSeries] = useState<{ date: string; mrrCents: number }[]>([]);
  const [invoices, setInvoices] = useState<InvoiceRow[]>([]);
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
      apiFetch<{ date: string; mrrCents: number }[]>(`/analytics/mrr?days=${period}`, session).catch(() => []),
      apiFetch<{ content: InvoiceRow[] }>("/invoices?limit=6", session).catch(() => ({ content: [] })),
    ])
      .then(([a, mrr, inv]) => {
        if (cancelled) return;
        setAnalytics(a);
        setMrrSeries(mrr);
        setInvoices(inv.content ?? []);
        setLoading(false);
      })
      .catch(err => {
        if (cancelled) return;
        setError(err.message ?? "Failed to load");
        setLoading(false);
      });

    return () => { cancelled = true; };
  }, [session, period]);

  const mrr   = analytics?.mrrCents ?? 0;
  const arpu  = analytics?.arpuCents ?? 0;
  const churn = analytics?.churnRate ?? 0;
  const subs  = analytics?.totalActiveSubscriptions ?? 0;

  if (error) {
    return (
      <div style={{
        background: "var(--red-soft)", border: "1px solid var(--red-ring)",
        borderRadius: 8, padding: "16px 20px",
        display: "flex", alignItems: "center", justifyContent: "space-between",
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
    <div style={{ maxWidth: 1100 }}>
      {/* Page header */}
      <div style={{ marginBottom: 28 }}>
        <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)", marginBottom: 4 }}>
          Overview
        </h1>
        <p style={{ fontSize: 13, color: "var(--neutral-500)" }}>
          Billing metrics for your organization
        </p>
      </div>

      {/* KPI Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12, marginBottom: 28 }}
        className="grid-cols-2-mobile"
      >
        <KpiCard
          title="MRR"
          value={<Counter to={mrr} format={formatINR} />}
          trend="+4.2% mo/mo"
          trendUp
          loading={loading}
          delay={0}
        />
        <KpiCard
          title="ARPU"
          value={<Counter to={arpu} format={formatINR} />}
          trend="+1.1%"
          trendUp
          loading={loading}
          delay={0.06}
        />
        <KpiCard
          title="Churn Rate"
          value={<Counter to={Math.round(churn * 1000)} suffix="%" format={n => `${(n / 10).toFixed(1)}%`} />}
          trend="-0.4% vs last month"
          trendUp={false}
          loading={loading}
          delay={0.12}
        />
        <KpiCard
          title="Active Subs"
          value={<Counter to={subs} />}
          trend="+3 this month"
          trendUp
          loading={loading}
          delay={0.18}
        />
      </div>

      {/* MRR Chart */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.24 }}
        style={{
          background: "var(--neutral-900)",
          border: "1px solid var(--neutral-800)",
          borderRadius: 8,
          padding: "20px 24px",
          marginBottom: 28,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 20 }}>
          <div>
            <div style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)", marginBottom: 2 }}>
              MRR over time
            </div>
            <div style={{ fontSize: 12, color: "var(--neutral-500)" }}>
              Monthly Recurring Revenue
            </div>
          </div>
          <div style={{ display: "flex", background: "var(--neutral-800)", borderRadius: 6, border: "1px solid var(--neutral-700)", padding: 3 }}>
            {([30, 90, 365] as const).map(p => (
              <button
                key={p}
                onClick={() => setPeriod(p)}
                style={{
                  padding: "4px 10px",
                  borderRadius: 4,
                  border: "none",
                  fontSize: 12, fontWeight: 500,
                  cursor: "pointer",
                  transition: "all 150ms ease",
                  background: period === p ? "var(--neutral-700)" : "transparent",
                  color: period === p ? "var(--neutral-50)" : "var(--neutral-500)",
                }}
              >
                {p === 365 ? "1y" : `${p}d`}
              </button>
            ))}
          </div>
        </div>

        {/* Chart container with AnimatePresence for period cross-fade */}
        <div style={{ height: 200, position: "relative" }}>
          {loading ? (
            <div className="skeleton" style={{ height: "100%", borderRadius: 6 }} />
          ) : mrrSeries.length === 0 ? (
            <div style={{
              height: "100%", display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 13, color: "var(--neutral-600)",
              borderRadius: 6, border: "1px dashed var(--neutral-800)",
            }}>
              No data for this period
            </div>
          ) : (
            <AnimatePresence mode="wait" initial={false}>
              <motion.div
                key={period}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.2 }}
                style={{ height: "100%" }}
              >
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={mrrSeries} margin={{ top: 4, right: 0, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="mrrGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="var(--neutral-300)" stopOpacity={0.15} />
                        <stop offset="100%" stopColor="var(--neutral-300)" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid vertical={false} stroke="var(--neutral-800)" strokeDasharray="none" />
                    <XAxis
                      dataKey="date"
                      axisLine={false} tickLine={false}
                      tick={{ fill: "var(--neutral-600)", fontSize: 11 }}
                      interval="preserveStartEnd"
                    />
                    {/* Fixed Y-axis: wider to prevent label clipping */}
                    <YAxis
                      width={90}
                      axisLine={false} tickLine={false}
                      tick={{ fill: "var(--neutral-600)", fontSize: 10 }}
                      tickFormatter={v => {
                        const n = v / 100;
                        if (n >= 100000) return `₹${(n / 100000).toFixed(1)}L`;
                        if (n >= 1000) return `₹${(n / 1000).toFixed(0)}K`;
                        return `₹${n}`;
                      }}
                    />
                    <Tooltip content={<ChartTooltip />} cursor={{ stroke: "var(--neutral-700)", strokeWidth: 1 }} />
                    {/* Chart draws left-to-right on mount via isAnimationActive + duration */}
                    <Area
                      type="monotone"
                      dataKey="mrrCents"
                      stroke="var(--neutral-300)"
                      strokeWidth={1.5}
                      fill="url(#mrrGrad)"
                      dot={false}
                      activeDot={{ r: 3, fill: "var(--neutral-50)", stroke: "var(--neutral-800)", strokeWidth: 2 }}
                      isAnimationActive={true}
                      animationDuration={1100}
                      animationEasing="ease-out"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </motion.div>
            </AnimatePresence>
          )}
        </div>
      </motion.div>

      {/* Recent invoices */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.3 }}
      >
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 14 }}>
          <h2 style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-200)" }}>
            Recent Invoices
          </h2>
          <Link href="/dashboard/invoices" style={{
            display: "flex", alignItems: "center", gap: 3,
            fontSize: 12, color: "var(--neutral-500)", textDecoration: "none",
            transition: "color 150ms ease",
          }}
            onMouseEnter={e => (e.currentTarget.style.color = "var(--neutral-300)")}
            onMouseLeave={e => (e.currentTarget.style.color = "var(--neutral-500)")}
          >
            View all <ChevronRight style={{ width: 14, height: 14 }} />
          </Link>
        </div>

        <div style={{
          background: "var(--neutral-900)",
          border: "1px solid var(--neutral-800)",
          borderRadius: 8, overflow: "hidden",
        }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Invoice</th>
                <th>Customer</th>
                <th>Period</th>
                <th style={{ textAlign: "right" }}>Amount</th>
                <th style={{ textAlign: "right" }}>Status</th>
                <th style={{ width: 40 }} />
              </tr>
            </thead>
            <tbody>
              {loading && Array.from({ length: 4 }, (_, i) => (
                <tr key={i}>
                  {[80, 120, 80, 60, 50, 20].map((w, j) => (
                    <td key={j}>
                      <div className="skeleton" style={{ height: 12, width: w, borderRadius: 3 }} />
                    </td>
                  ))}
                </tr>
              ))}

              {!loading && invoices.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ textAlign: "center", padding: "40px 16px", color: "var(--neutral-600)" }}>
                    No invoices yet. Create a subscription to generate your first invoice.
                  </td>
                </tr>
              )}

              {!loading && invoices.map(inv => {
                const isPastDue = inv.status === "OPEN" && new Date(inv.dueAt).getTime() < Date.now();
                return (
                  <tr key={inv.id} style={{
                    borderLeft: isPastDue ? "2px solid var(--amber)" : "2px solid transparent",
                  }}>
                    <td>
                      <Link href={`/dashboard/invoices/${inv.id}`} style={{
                        color: "var(--neutral-300)", textDecoration: "none",
                        fontFamily: "var(--font-mono)", fontSize: 12,
                        transition: "color 150ms ease",
                      }}
                        onMouseEnter={e => (e.currentTarget.style.color = "var(--neutral-50)")}
                        onMouseLeave={e => (e.currentTarget.style.color = "var(--neutral-300)")}
                      >
                        {inv.invoiceNumber}
                      </Link>
                    </td>
                    <td style={{ color: "var(--neutral-200)", fontWeight: 450 }}>{inv.customerName}</td>
                    <td style={{ color: "var(--neutral-500)", fontSize: 12 }}>{formatDate(inv.periodStart)}</td>
                    <td style={{ textAlign: "right", fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--neutral-200)" }}>
                      {formatINR(inv.totalCents)}
                    </td>
                    <td style={{ textAlign: "right" }}>
                      <StatusBadge status={inv.status} />
                    </td>
                    <td style={{ textAlign: "right" }}>
                      {inv.status === "OPEN" && (
                        <Link href={`/dashboard/invoices/${inv.id}`} style={{
                          display: "inline-flex", alignItems: "center",
                          padding: "3px 7px", borderRadius: 4,
                          fontSize: 11, fontWeight: 500,
                          background: "var(--neutral-800)",
                          border: "1px solid var(--neutral-700)",
                          color: "var(--neutral-300)",
                          textDecoration: "none",
                          transition: "all 150ms ease",
                        }}
                          onMouseEnter={e => {
                            (e.currentTarget as HTMLAnchorElement).style.background = "var(--neutral-700)";
                            (e.currentTarget as HTMLAnchorElement).style.color = "var(--neutral-50)";
                          }}
                          onMouseLeave={e => {
                            (e.currentTarget as HTMLAnchorElement).style.background = "var(--neutral-800)";
                            (e.currentTarget as HTMLAnchorElement).style.color = "var(--neutral-300)";
                          }}
                        >
                          Pay <ArrowUpRight style={{ width: 10, height: 10, marginLeft: 2 }} />
                        </Link>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </motion.div>
    </div>
  );
}
