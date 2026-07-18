"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import { apiFetch, formatDate } from "@/lib/api";
import { StatusBadge } from "@/components/ui/status-dot";
import { toast } from "@/components/ui/toast";
import { ArrowLeft, AlertCircle, Ban, FileText, Activity, Check, X } from "lucide-react";

type SubStatus = "TRIAL" | "ACTIVE" | "PAST_DUE" | "CANCELED";

interface Subscription {
  id: string;
  customerName: string;
  customerId: string;
  planName: string;
  planId: string;
  status: SubStatus;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  canceledAt: string | null;
  trialEndAt: string | null;
  createdAt: string;
}

interface Invoice {
  id: string;
  invoiceNumber: string;
  totalCents: number;
  status: string;
  periodStart: string;
  dueAt: string;
}

interface UsageSummary {
  totalUnits: number;
  eventCount: number;
}

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
function LifecycleStateMachine({ currentStatus }: { currentStatus: SubStatus }) {
  const currentIdx = LIFECYCLE_STAGES.findIndex(s => s.status === currentStatus);

  return (
    <div style={{ padding: "20px 24px" }}>
      <div style={{ position: "relative", display: "flex", alignItems: "center" }}>
        {/* Connector track */}
        <div style={{
          position: "absolute", top: 14, left: 14, right: 14,
          height: 1, background: "var(--neutral-800)", zIndex: 0,
        }} />
        {/* Colored fill showing progress */}
        <div style={{
          position: "absolute", top: 14, left: 14,
          height: 1,
          width: currentIdx > 0
            ? `calc(${(currentIdx / (LIFECYCLE_STAGES.length - 1)) * 100}% - 14px)`
            : 0,
          background: STATUS_COLOR[currentStatus] ?? "var(--neutral-500)",
          zIndex: 0,
          transition: "width 600ms ease",
        }} />

        {LIFECYCLE_STAGES.map((stage, idx) => {
          const isPast    = idx < currentIdx;
          const isCurrent = idx === currentIdx;
          const isFuture  = idx > currentIdx;

          const color = isCurrent ? STATUS_COLOR[stage.status] :
                        isPast    ? "var(--neutral-600)" :
                                    "var(--neutral-700)";

          return (
            <div
              key={stage.status}
              style={{
                flex: 1, display: "flex", flexDirection: "column",
                alignItems: "center", gap: 10, position: "relative", zIndex: 1,
              }}
            >
              {/* Node — uses layoutId so it slides when status changes */}
              <div style={{ position: "relative" }}>
                {isCurrent && (
                  <motion.div
                    layoutId="lifecycle-active-node"
                    style={{
                      position: "absolute",
                      inset: -4,
                      borderRadius: "50%",
                      border: `2px solid ${color}`,
                      opacity: 0.5,
                    }}
                    animate={{
                      // Pulsing glow ring
                      boxShadow: [
                        `0 0 0 0 ${color}44`,
                        `0 0 0 8px ${color}00`,
                        `0 0 0 0 ${color}00`,
                      ],
                    }}
                    transition={{ repeat: Infinity, duration: 2.5, ease: "easeOut" }}
                  />
                )}
                <div style={{
                  width: 28, height: 28, borderRadius: "50%",
                  background: isCurrent ? color : "var(--neutral-950)",
                  border: `2px solid ${color}`,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  opacity: isFuture ? 0.4 : 1,
                  transition: "all 400ms ease",
                }}>
                  {isPast && <Check style={{ width: 12, height: 12, color: "var(--neutral-600)" }} />}
                  {isCurrent && (
                    <div style={{ width: 8, height: 8, borderRadius: "50%", background: "white" }} />
                  )}
                </div>
              </div>

              {/* Label */}
              <div style={{ textAlign: "center" }}>
                <div style={{
                  fontSize: 12, fontWeight: isCurrent ? 500 : 400,
                  color: isCurrent ? "var(--neutral-50)" : isFuture ? "var(--neutral-700)" : "var(--neutral-500)",
                  marginBottom: 2,
                }}>
                  {stage.label}
                </div>
                {isCurrent && (
                  <motion.div
                    initial={{ opacity: 0, y: 4 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3, delay: 0.1 }}
                    style={{ fontSize: 11, color: "var(--neutral-500)", lineHeight: 1.4 }}
                  >
                    {stage.desc}
                  </motion.div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Cancel Confirm Modal ─────────────────────────────────────
function CancelModal({
  sub,
  onClose,
  onCanceled,
}: {
  sub: Subscription;
  onClose: () => void;
  onCanceled: () => void;
}) {
  const { data: session } = useSession();
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  useEffect(() => {
    const handle = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", handle);
    return () => window.removeEventListener("keydown", handle);
  }, [onClose]);

  const confirm = async () => {
    if (!session) return;
    setLoading(true); setErr("");
    try {
      await apiFetch(`/subscriptions/${sub.id}/cancel`, session, { method: "POST" });
      toast.success(`Canceled subscription for ${sub.customerName}`);
      onCanceled();
    } catch (e: any) {
      setErr(e.message ?? "Failed to cancel subscription");
      setLoading(false);
    }
  };

  return (
    <>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        transition={{ duration: 0.15 }}
        onClick={onClose}
        style={{ position: "fixed", inset: 0, zIndex: 100, background: "rgba(0,0,0,0.7)", backdropFilter: "blur(4px)" }}
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.97, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.97, y: 4 }}
        transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
        style={{
          position: "fixed", top: "50%", left: "50%",
          transform: "translate(-50%, -50%)",
          zIndex: 101,
          background: "var(--neutral-900)", border: "1px solid var(--neutral-700)",
          borderRadius: 10, padding: 24, width: 400,
          boxShadow: "0 24px 64px rgba(0,0,0,0.6)",
        }}
        role="dialog" aria-modal="true" aria-label="Cancel subscription"
      >
        <div style={{ display: "flex", alignItems: "flex-start", gap: 12, marginBottom: 16 }}>
          <div style={{ width: 36, height: 36, borderRadius: 8, background: "var(--red-soft)", border: "1px solid var(--red-ring)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
            <Ban style={{ width: 16, height: 16, color: "var(--red)" }} />
          </div>
          <div>
            <h3 style={{ fontSize: 15, fontWeight: 500, color: "var(--neutral-50)", marginBottom: 6 }}>Cancel subscription?</h3>
            <p style={{ fontSize: 13, color: "var(--neutral-400)", lineHeight: 1.6 }}>
              The subscription for <strong style={{ color: "var(--neutral-200)" }}>{sub.customerName}</strong> on
              plan <strong style={{ color: "var(--neutral-200)" }}>{sub.planName}</strong> will be immediately
              canceled. The customer will lose access at the end of the current billing period.
            </p>
          </div>
        </div>

        {err && (
          <div style={{ display: "flex", alignItems: "center", gap: 7, padding: "9px 12px", marginBottom: 16, background: "var(--red-soft)", border: "1px solid var(--red-ring)", borderRadius: 6, fontSize: 12, color: "var(--red)" }}>
            <AlertCircle style={{ width: 13, height: 13 }} /> {err}
          </div>
        )}

        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button className="btn btn-ghost" onClick={onClose} disabled={loading}>Keep subscription</button>
          <button className="btn btn-danger" onClick={confirm} disabled={loading} style={{ gap: 5 }}>
            <Ban style={{ width: 13, height: 13 }} />
            {loading ? "Canceling..." : "Cancel Subscription"}
          </button>
        </div>
      </motion.div>
    </>
  );
}

// ── Page ─────────────────────────────────────────────────────

const LIFECYCLE_STAGES = [
  { status: "TRIALING", label: "Trial", desc: "Evaluating" },
  { status: "ACTIVE", label: "Active", desc: "Subscribed" },
  { status: "PAST_DUE", label: "Past Due", desc: "Payment failed" },
  { status: "CANCELED", label: "Canceled", desc: "Ended" },
  { status: "PAUSED", label: "Paused", desc: "Suspended" },
];
const STATUS_COLOR: Record<string, string> = {
  TRIALING: "var(--amber)",
  ACTIVE: "var(--green)",
  PAST_DUE: "var(--red)",
  CANCELED: "var(--neutral-500)",
  PAUSED: "var(--neutral-400)",
};
export default function SubscriptionDetailPage() {
  const { data: session } = useSession();
  const { id } = useParams() as { id: string };
  const router = useRouter();

  const [sub, setSub] = useState<Subscription | null>(null);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [usage, setUsage] = useState<UsageSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCancel, setShowCancel] = useState(false);

  const load = async () => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const [s, invRes] = await Promise.all([
        apiFetch<Subscription>(`/subscriptions/${id}`, session),
        apiFetch<{ content: Invoice[] }>(`/invoices?subscriptionId=${id}&limit=20`, session).catch(() => ({ content: [] })),
      ]);
      setSub(s);
      setInvoices(invRes.content ?? []);

      // Usage summary — best effort
      apiFetch<UsageSummary>(`/usage?subscriptionId=${id}&summary=true`, session)
        .then(u => setUsage(u))
        .catch(() => {});
    } catch (e: any) {
      setError(e.message ?? "Failed to load subscription");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session, id]);

  if (loading) {
    return (
      <div style={{ maxWidth: 860 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 32 }}>
          <div className="skeleton" style={{ width: 32, height: 32, borderRadius: 6 }} />
          <div className="skeleton" style={{ width: 200, height: 22, borderRadius: 4 }} />
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <div className="skeleton" style={{ height: 130, borderRadius: 10 }} />
          <div className="skeleton" style={{ height: 100, borderRadius: 10 }} />
          <div className="skeleton" style={{ height: 200, borderRadius: 10 }} />
        </div>
      </div>
    );
  }

  if (error || !sub) {
    return (
      <div style={{ maxWidth: 860 }}>
        <button onClick={() => router.push("/dashboard/subscriptions")} className="btn btn-ghost btn-sm" style={{ marginBottom: 24, gap: 6 }}>
          <ArrowLeft style={{ width: 14, height: 14 }} /> Back to Subscriptions
        </button>
        <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "16px 20px", background: "var(--red-soft)", border: "1px solid var(--red-ring)", borderRadius: 8, fontSize: 13, color: "var(--red)" }}>
          <AlertCircle style={{ width: 15, height: 15, flexShrink: 0 }} />
          <span>{error ?? "Subscription not found."}</span>
          <button onClick={load} style={{ marginLeft: "auto", fontSize: 12, background: "none", border: "1px solid currentColor", borderRadius: 4, padding: "2px 8px", color: "inherit", cursor: "pointer" }}>Retry</button>
        </div>
      </div>
    );
  }

  const isCanceled = sub.status === "CANCELED";

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
      style={{ maxWidth: 860 }}
    >
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 28 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <button
            onClick={() => router.push("/dashboard/subscriptions")}
            className="btn btn-ghost btn-sm"
            style={{ padding: "5px 8px" }}
            aria-label="Back to subscriptions"
          >
            <ArrowLeft style={{ width: 14, height: 14 }} />
          </button>
          <div>
            <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
              {sub.customerName}
            </h1>
            <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 2 }}>{sub.planName}</p>
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <StatusBadge status={sub.status} />
          {!isCanceled && (
            <button
              className="btn btn-sm"
              onClick={() => setShowCancel(true)}
              style={{
                background: "var(--red-soft)", color: "var(--red)",
                border: "1px solid var(--red-ring)", gap: 5,
              }}
              onMouseEnter={e => { const b = e.currentTarget as HTMLButtonElement; b.style.background = "var(--red)"; b.style.color = "white"; b.style.borderColor = "var(--red)"; }}
              onMouseLeave={e => { const b = e.currentTarget as HTMLButtonElement; b.style.background = "var(--red-soft)"; b.style.color = "var(--red)"; b.style.borderColor = "var(--red-ring)"; }}
            >
              <Ban style={{ width: 12, height: 12 }} /> Cancel
            </button>
          )}
        </div>
      </div>

      {/* Lifecycle State Machine */}
      <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, marginBottom: 16, overflow: "hidden" }}>
        <div style={{ padding: "16px 24px", borderBottom: "1px solid var(--neutral-800)", display: "flex", alignItems: "center", gap: 8 }}>
          <Activity style={{ width: 14, height: 14, color: "var(--neutral-400)" }} />
          <h2 style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)" }}>Subscription Lifecycle</h2>
        </div>
        <LifecycleStateMachine currentStatus={sub.status} />
      </div>

      {/* Details Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 16 }}>
        {/* Period & Dates */}
        <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, padding: 20 }}>
          <div style={{ fontSize: 11, fontWeight: 500, letterSpacing: "0.07em", textTransform: "uppercase", color: "var(--neutral-500)", marginBottom: 16 }}>
            Billing Details
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {[
              { label: "Current period", value: `${formatDate(sub.currentPeriodStart)} → ${formatDate(sub.currentPeriodEnd)}`, mono: true },
              { label: "Customer", value: sub.customerName, link: `/dashboard/customers/${sub.customerId}` },
              { label: "Plan", value: sub.planName, link: `/dashboard/plans/${sub.planId}` },
              { label: "Started", value: formatDate(sub.createdAt) },
              ...(sub.canceledAt ? [{ label: "Canceled at", value: formatDate(sub.canceledAt) }] : []),
              ...(sub.trialEndAt ? [{ label: "Trial ends", value: formatDate(sub.trialEndAt) }] : []),
            ].map(({ label, value, mono, link }) => (
              <div key={label} style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 12 }}>
                <span style={{ fontSize: 12, color: "var(--neutral-500)", flexShrink: 0 }}>{label}</span>
                {link ? (
                  <Link href={link} style={{ fontSize: 12, color: "var(--neutral-300)", textDecoration: "none", fontFamily: mono ? "var(--font-mono)" : undefined }}
                    onMouseEnter={e => (e.currentTarget.style.color = "var(--neutral-50)")}
                    onMouseLeave={e => (e.currentTarget.style.color = "var(--neutral-300)")}
                  >
                    {value} →
                  </Link>
                ) : (
                  <span style={{ fontSize: 12, color: "var(--neutral-200)", fontFamily: mono ? "var(--font-mono)" : undefined, textAlign: "right" }}>
                    {value}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Usage */}
        <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, padding: 20 }}>
          <div style={{ fontSize: 11, fontWeight: 500, letterSpacing: "0.07em", textTransform: "uppercase", color: "var(--neutral-500)", marginBottom: 16 }}>
            Usage This Period
          </div>
          {usage ? (
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20 }}>
              <div>
                <div style={{ fontSize: 28, fontWeight: 500, color: "var(--neutral-50)", fontFamily: "var(--font-mono)", letterSpacing: "-0.02em", marginBottom: 4 }}>
                  {usage.totalUnits.toLocaleString("en-IN")}
                </div>
                <div style={{ fontSize: 11, color: "var(--neutral-500)" }}>Total units</div>
              </div>
              <div>
                <div style={{ fontSize: 28, fontWeight: 500, color: "var(--neutral-50)", fontFamily: "var(--font-mono)", letterSpacing: "-0.02em", marginBottom: 4 }}>
                  {usage.eventCount.toLocaleString("en-IN")}
                </div>
                <div style={{ fontSize: 11, color: "var(--neutral-500)" }}>Usage events</div>
              </div>
            </div>
          ) : (
            <div style={{ color: "var(--neutral-500)", fontSize: 13, paddingTop: 12 }}>
              No usage data recorded for this billing period.
            </div>
          )}
        </div>
      </div>

      {/* Invoices */}
      <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, overflow: "hidden" }}>
        <div style={{ padding: "16px 20px", borderBottom: "1px solid var(--neutral-800)", display: "flex", alignItems: "center", gap: 8 }}>
          <FileText style={{ width: 14, height: 14, color: "var(--neutral-400)" }} />
          <h2 style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)" }}>Invoices</h2>
          <span style={{ marginLeft: "auto", fontSize: 11, color: "var(--neutral-500)" }}>{invoices.length} total</span>
        </div>

        {invoices.length === 0 ? (
          <div style={{ padding: "40px 20px", textAlign: "center", color: "var(--neutral-500)", fontSize: 13 }}>
            No invoices generated yet for this subscription.
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Invoice</th>
                <th>Period</th>
                <th style={{ textAlign: "right" }}>Amount</th>
                <th style={{ textAlign: "right" }}>Due</th>
                <th style={{ textAlign: "right" }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map(inv => (
                <tr
                  key={inv.id}
                  onClick={() => router.push(`/dashboard/invoices/${inv.id}`)}
                  style={{ cursor: "pointer" }}
                >
                  <td style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--neutral-300)" }}>
                    {inv.invoiceNumber}
                  </td>
                  <td style={{ fontSize: 12, color: "var(--neutral-500)" }}>{formatDate(inv.periodStart)}</td>
                  <td style={{ textAlign: "right", fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--neutral-200)" }}>
                    {formatINR(inv.totalCents)}
                  </td>
                  <td style={{ textAlign: "right", fontSize: 12, color: "var(--neutral-500)" }}>
                    {formatDate(inv.dueAt)}
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <StatusBadge status={inv.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <AnimatePresence>
        {showCancel && sub && (
          <CancelModal
            sub={sub}
            onClose={() => setShowCancel(false)}
            onCanceled={() => { setSub(s => s ? { ...s, status: "CANCELED", canceledAt: new Date().toISOString() } : s); setShowCancel(false); }}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}
