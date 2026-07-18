"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useParams, useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { apiFetch, formatDate } from "@/lib/api";
import { StatusBadge } from "@/components/ui/status-dot";
import { toast } from "@/components/ui/toast";
import { ArrowLeft, Archive, Users, AlertCircle, Tag, Calendar } from "lucide-react";

interface PlanTier {
  tierOrder: number;
  upTo: number | null;
  unitAmount: number;
  flatFee: number;
}

interface Plan {
  id: string;
  name: string;
  pricingModel: "FLAT" | "TIERED" | "METERED";
  billingInterval: "MONTHLY" | "ANNUAL";
  flatAmount: number | null;
  description: string | null;
  trialDays: number;
  archived: boolean;
  createdAt: string;
  tiers?: PlanTier[];
}

interface Subscriber {
  id: string;
  customerName: string;
  status: string;
  currentPeriodStart: string;
  currentPeriodEnd: string;
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
function ArchiveModal({
  plan,
  onClose,
  onArchived,
}: {
  plan: Plan;
  onClose: () => void;
  onArchived: () => void;
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
      await apiFetch(`/plans/${plan.id}/archive`, session, { method: "POST" });
      toast.success(`"${plan.name}" archived`);
      onArchived();
    } catch (e: any) {
      setErr(e.message ?? "Failed to archive plan");
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
          borderRadius: 10, padding: 24, width: 380,
          boxShadow: "0 24px 64px rgba(0,0,0,0.6)",
        }}
        role="dialog" aria-modal="true" aria-label="Archive plan"
      >
        <h3 style={{ fontSize: 15, fontWeight: 500, color: "var(--neutral-50)", marginBottom: 10 }}>Archive plan?</h3>
        <p style={{ fontSize: 13, color: "var(--neutral-400)", lineHeight: 1.6, marginBottom: 20 }}>
          <strong style={{ color: "var(--neutral-200)" }}>{plan.name}</strong> will be archived. Existing subscribers
          will not be affected, but no new subscriptions can be created on this plan.
        </p>
        {err && (
          <div style={{ display: "flex", alignItems: "center", gap: 7, padding: "9px 12px", marginBottom: 16, background: "var(--red-soft)", border: "1px solid var(--red-ring)", borderRadius: 6, fontSize: 12, color: "var(--red)" }}>
            <AlertCircle style={{ width: 13, height: 13 }} /> {err}
          </div>
        )}
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button className="btn btn-ghost" onClick={onClose} disabled={loading}>Cancel</button>
          <button className="btn btn-secondary" onClick={confirm} disabled={loading} style={{ gap: 5 }}>
            <Archive style={{ width: 13, height: 13 }} />
            {loading ? "Archiving..." : "Archive Plan"}
          </button>
        </div>
      </motion.div>
    </>
  );
}

// ── Page ─────────────────────────────────────────────────────

const MODEL_LABEL: Record<string, string> = {
  FLAT_RATE: "Flat Rate",
  PER_UNIT: "Per Unit",
  TIERED: "Tiered",
  VOLUME: "Volume",
};
const MODEL_DESC: Record<string, string> = {
  FLAT_RATE: "A single fixed price per billing period.",
  PER_UNIT: "Price scales linearly with the quantity purchased.",
  TIERED: "Different prices apply to different ranges of quantity.",
  VOLUME: "The price of all units is determined by the total quantity.",
};
export default function PlanDetailPage() {
  const { data: session } = useSession();
  const { id } = useParams() as { id: string };
  const router = useRouter();

  const [plan, setPlan] = useState<Plan | null>(null);
  const [subscribers, setSubscribers] = useState<Subscriber[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showArchive, setShowArchive] = useState(false);

  const load = async () => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const [p, subsRes] = await Promise.all([
        apiFetch<Plan>(`/plans/${id}`, session),
        apiFetch<{ content: Subscriber[] }>(`/subscriptions?planId=${id}&limit=50`, session).catch(() => ({ content: [] })),
      ]);
      setPlan(p);
      setSubscribers(subsRes.content ?? []);
    } catch (e: any) {
      setError(e.message ?? "Failed to load plan");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session, id]);

  if (loading) {
    return (
      <div style={{ maxWidth: 800 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 32 }}>
          <div className="skeleton" style={{ width: 32, height: 32, borderRadius: 6 }} />
          <div className="skeleton" style={{ width: 180, height: 22, borderRadius: 4 }} />
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <div className="skeleton" style={{ height: 160, borderRadius: 10 }} />
          <div className="skeleton" style={{ height: 220, borderRadius: 10 }} />
        </div>
      </div>
    );
  }

  if (error || !plan) {
    return (
      <div style={{ maxWidth: 800 }}>
        <button onClick={() => router.push("/dashboard/plans")} className="btn btn-ghost btn-sm" style={{ marginBottom: 24, gap: 6 }}>
          <ArrowLeft style={{ width: 14, height: 14 }} /> Back to Plans
        </button>
        <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "16px 20px", background: "var(--red-soft)", border: "1px solid var(--red-ring)", borderRadius: 8, fontSize: 13, color: "var(--red)" }}>
          <AlertCircle style={{ width: 15, height: 15, flexShrink: 0 }} />
          <span>{error ?? "Plan not found."}</span>
          <button onClick={load} style={{ marginLeft: "auto", fontSize: 12, background: "none", border: "1px solid currentColor", borderRadius: 4, padding: "2px 8px", color: "inherit", cursor: "pointer" }}>Retry</button>
        </div>
      </div>
    );
  }

  const activeSubscribers = subscribers.filter(s => s.status === "ACTIVE" || s.status === "TRIAL");

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
      style={{ maxWidth: 800 }}
    >
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 28 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <button
            onClick={() => router.push("/dashboard/plans")}
            className="btn btn-ghost btn-sm"
            style={{ padding: "5px 8px" }}
            aria-label="Back to plans"
          >
            <ArrowLeft style={{ width: 14, height: 14 }} />
          </button>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
                {plan.name}
              </h1>
              {plan.archived && (
                <span style={{ fontSize: 10, fontWeight: 500, letterSpacing: "0.06em", color: "var(--neutral-600)", background: "var(--neutral-800)", border: "1px solid var(--neutral-700)", borderRadius: 3, padding: "1px 5px", textTransform: "uppercase" }}>
                  Archived
                </span>
              )}
            </div>
            <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 2 }}>
              {MODEL_LABEL[plan.pricingModel]} · {plan.billingInterval === "MONTHLY" ? "Monthly" : "Annual"}
            </p>
          </div>
        </div>
        {!plan.archived && (
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setShowArchive(true)}
            style={{ gap: 5 }}
          >
            <Archive style={{ width: 12, height: 12 }} /> Archive Plan
          </button>
        )}
      </div>

      {/* Pricing Card */}
      <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, padding: 24, marginBottom: 16 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 20 }}>
          <Tag style={{ width: 14, height: 14, color: "var(--neutral-400)" }} />
          <h2 style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)" }}>Pricing</h2>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 20, marginBottom: plan.tiers && plan.tiers.length > 0 ? 24 : 0 }}>
          <div>
            <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 6 }}>Model</div>
            <div style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-100)" }}>{MODEL_LABEL[plan.pricingModel]}</div>
            <div style={{ fontSize: 11, color: "var(--neutral-500)", marginTop: 4, lineHeight: 1.5 }}>{MODEL_DESC[plan.pricingModel]}</div>
          </div>
          <div>
            <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 6 }}>Billing Interval</div>
            <div style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-100)" }}>
              {plan.billingInterval === "MONTHLY" ? "Monthly" : "Annual"}
            </div>
          </div>
          {plan.flatAmount != null ? (
            <div>
              <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 6 }}>Base Price</div>
              <div style={{ fontSize: 22, fontWeight: 500, color: "var(--neutral-50)", fontFamily: "var(--font-mono)", letterSpacing: "-0.02em" }}>
                {formatINR(plan.flatAmount)}
              </div>
              <div style={{ fontSize: 11, color: "var(--neutral-500)", marginTop: 4 }}>
                per {plan.billingInterval === "MONTHLY" ? "month" : "year"}
              </div>
            </div>
          ) : (
            <div>
              <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 6 }}>Base Price</div>
              <div style={{ fontSize: 14, color: "var(--neutral-400)" }}>See tier table below</div>
            </div>
          )}
        </div>

        {plan.tiers && plan.tiers.length > 0 && (
          <>
            <div style={{ height: 1, background: "var(--neutral-800)", margin: "0 0 20px" }} />
            <div style={{ fontSize: 11, fontWeight: 500, letterSpacing: "0.07em", textTransform: "uppercase", color: "var(--neutral-500)", marginBottom: 12 }}>
              Tier Breakdown
            </div>
            <table className="data-table" style={{ fontSize: 12 }}>
              <thead>
                <tr>
                  <th>Tier</th>
                  <th>Up to</th>
                  <th style={{ textAlign: "right" }}>Unit Amount</th>
                  <th style={{ textAlign: "right" }}>Flat Fee</th>
                </tr>
              </thead>
              <tbody>
                {plan.tiers.map(t => (
                  <tr key={t.tierOrder}>
                    <td style={{ color: "var(--neutral-500)" }}>#{t.tierOrder + 1}</td>
                    <td style={{ fontFamily: "var(--font-mono)", color: "var(--neutral-300)" }}>
                      {t.upTo == null ? "∞ (unlimited)" : t.upTo.toLocaleString()}
                    </td>
                    <td style={{ textAlign: "right", fontFamily: "var(--font-mono)", color: "var(--neutral-200)" }}>
                      {formatINR(t.unitAmount)}<span style={{ color: "var(--neutral-600)", marginLeft: 4 }}>/unit</span>
                    </td>
                    <td style={{ textAlign: "right", fontFamily: "var(--font-mono)", color: "var(--neutral-400)" }}>
                      {t.flatFee > 0 ? formatINR(t.flatFee) : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}

        {plan.trialDays > 0 && (
          <div style={{ marginTop: 16, padding: "10px 14px", background: "var(--blue-soft)", border: "1px solid rgba(37,99,235,0.2)", borderRadius: 6, display: "flex", alignItems: "center", gap: 8 }}>
            <Calendar style={{ width: 13, height: 13, color: "var(--blue)" }} />
            <span style={{ fontSize: 12, color: "var(--blue)" }}>
              {plan.trialDays}-day free trial on new subscriptions
            </span>
          </div>
        )}
      </div>

      {/* Subscribers */}
      <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, overflow: "hidden" }}>
        <div style={{ padding: "16px 20px", borderBottom: "1px solid var(--neutral-800)", display: "flex", alignItems: "center", gap: 8 }}>
          <Users style={{ width: 14, height: 14, color: "var(--neutral-400)" }} />
          <h2 style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)" }}>Subscribers</h2>
          <div style={{ marginLeft: "auto", display: "flex", gap: 12 }}>
            <span style={{ fontSize: 11, color: "var(--green)" }}>{activeSubscribers.length} active</span>
            <span style={{ fontSize: 11, color: "var(--neutral-500)" }}>{subscribers.length} total</span>
          </div>
        </div>

        {subscribers.length === 0 ? (
          <div style={{ padding: "40px 20px", textAlign: "center", color: "var(--neutral-500)", fontSize: 13 }}>
            No subscribers on this plan yet.
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Status</th>
                <th>Current Period</th>
              </tr>
            </thead>
            <tbody>
              {subscribers.map(sub => (
                <tr
                  key={sub.id}
                  onClick={() => router.push(`/dashboard/subscriptions/${sub.id}`)}
                  style={{ cursor: "pointer", opacity: sub.status === "CANCELED" ? 0.5 : 1 }}
                >
                  <td style={{ fontWeight: 450, color: "var(--neutral-100)" }}>{sub.customerName}</td>
                  <td><StatusBadge status={sub.status} /></td>
                  <td style={{ fontSize: 12, color: "var(--neutral-500)", fontFamily: "var(--font-mono)" }}>
                    {formatDate(sub.currentPeriodStart)} → {formatDate(sub.currentPeriodEnd)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <AnimatePresence>
        {showArchive && (
          <ArchiveModal
            plan={plan}
            onClose={() => setShowArchive(false)}
            onArchived={() => { setPlan(p => p ? { ...p, archived: true } : p); setShowArchive(false); }}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}
