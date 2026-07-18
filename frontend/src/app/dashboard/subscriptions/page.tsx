"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { motion } from "framer-motion";
import { useRouter } from "next/navigation";
import { apiFetch, formatDate } from "@/lib/api";
import { StatusBadge } from "@/components/ui/status-dot";
import { toast } from "@/components/ui/toast";
import { AlertCircle, Ban } from "lucide-react";

type SubStatus = "TRIAL" | "ACTIVE" | "PAST_DUE" | "CANCELED";

interface Subscription {
  id: string;
  customerName: string;
  planName: string;
  status: SubStatus;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  canceledAt: string | null;
  trialEndAt: string | null;
}

const FILTERS: { label: string; value: SubStatus | "" }[] = [
  { label: "All", value: "" },
  { label: "Active", value: "ACTIVE" },
  { label: "Trial", value: "TRIAL" },
  { label: "Past Due", value: "PAST_DUE" },
  { label: "Canceled", value: "CANCELED" },
];

export default function SubscriptionsPage() {
  const { data: session } = useSession();
  const router = useRouter();
  const [subs, setSubs] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<SubStatus | "">("");
  const [canceling, setCanceling] = useState<string | null>(null);

  const load = async () => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const params = new URLSearchParams({ limit: "50" });
      if (filter) params.set("status", filter);
      const res = await apiFetch<{ content: Subscription[] }>(`/subscriptions?${params}`, session);
      setSubs(res.content);
    } catch (e: any) {
      setError(e.message ?? "Failed to load subscriptions");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session, filter]);

  const handleCancel = async (sub: Subscription) => {
    if (!session) return;
    if (!confirm(`Cancel subscription for ${sub.customerName}? This cannot be undone.`)) return;
    setCanceling(sub.id);
    try {
      await apiFetch(`/subscriptions/${sub.id}/cancel`, session, { method: "POST" });
      toast.success(`Canceled subscription for ${sub.customerName}`);
      load();
    } catch (e: any) {
      toast.error(e.message ?? "Failed to cancel");
    } finally {
      setCanceling(null);
    }
  };

  return (
    <div style={{ maxWidth: 1100 }}>
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
            Subscriptions
          </h1>
          <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 3 }}>
            All customer subscriptions and their billing status
          </p>
        </div>
      </div>

      {/* Filter pills */}
      <div style={{ display: "flex", gap: 6, marginBottom: 18 }}>
        {FILTERS.map(f => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            style={{
              padding: "5px 11px",
              background: filter === f.value ? "var(--neutral-200)" : "var(--neutral-800)",
              border: "1px solid",
              borderColor: filter === f.value ? "var(--neutral-200)" : "var(--neutral-700)",
              borderRadius: 20,
              fontSize: 12, fontWeight: 500,
              color: filter === f.value ? "var(--neutral-950)" : "var(--neutral-400)",
              cursor: "pointer",
              transition: "all 150ms ease",
            }}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && (
        <div style={{
          display: "flex", alignItems: "center", gap: 8,
          padding: "10px 14px", marginBottom: 16,
          background: "var(--red-soft)", border: "1px solid var(--red-ring)",
          borderRadius: 7, fontSize: 13, color: "var(--red)",
        }}>
          <AlertCircle style={{ width: 14, height: 14 }} />
          {error}
          <button onClick={load} style={{ marginLeft: "auto", fontSize: 12, background: "none", border: "1px solid currentColor", borderRadius: 4, padding: "2px 8px", color: "inherit", cursor: "pointer" }}>
            Retry
          </button>
        </div>
      )}

      <div style={{
        background: "var(--neutral-900)",
        border: "1px solid var(--neutral-800)",
        borderRadius: 8, overflow: "hidden",
      }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Plan</th>
              <th>Status</th>
              <th>Period</th>
              <th style={{ textAlign: "right" }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading && Array.from({ length: 5 }, (_, i) => (
              <tr key={i}>
                {[140, 100, 70, 160, 60].map((w, j) => (
                  <td key={j}>
                    <div className="skeleton" style={{ height: 12, width: w, borderRadius: 3, marginLeft: j === 4 ? "auto" : 0 }} />
                  </td>
                ))}
              </tr>
            ))}

            {!loading && subs.length === 0 && (
              <tr>
                <td colSpan={5} style={{ textAlign: "center", padding: "60px 24px", color: "var(--neutral-600)", fontSize: 13 }}>
                  No subscriptions{filter ? ` with status ${filter}` : ""}.
                </td>
              </tr>
            )}

            {!loading && subs.map((sub, i) => {
              const isPastDue = sub.status === "PAST_DUE";
              const isCanceled = sub.status === "CANCELED";
              const canCancel = !isCanceled && !["PAST_DUE"].includes(sub.status) || sub.status === "PAST_DUE";
              const isCancelingThis = canceling === sub.id;

              return (
                <motion.tr
                  key={sub.id}
                  initial={{ opacity: 0, y: 4 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.03 }}
                  onClick={() => router.push(`/dashboard/subscriptions/${sub.id}`)}
                  style={{
                    cursor: "pointer",
                    opacity: isCanceled ? 0.5 : 1,
                    borderLeft: isPastDue ? "2px solid var(--amber)" : "2px solid transparent",
                  }}
                >
                  <td style={{ fontWeight: 450, color: "var(--neutral-100)" }}>{sub.customerName}</td>
                  <td style={{ color: "var(--neutral-400)", fontSize: 12 }}>{sub.planName}</td>
                  <td>
                    <StatusBadge status={sub.status} />
                  </td>
                  <td style={{ fontSize: 12, color: "var(--neutral-500)", fontFamily: "var(--font-mono)" }}>
                    {formatDate(sub.currentPeriodStart)} → {formatDate(sub.currentPeriodEnd)}
                  </td>
                  <td style={{ textAlign: "right" }}>
                    {!isCanceled && (
                      <button
                        onClick={() => handleCancel(sub)}
                        disabled={isCancelingThis}
                        className="btn btn-sm"
                        style={{
                          background: "transparent",
                          border: "1px solid var(--neutral-700)",
                          color: "var(--neutral-500)",
                          fontSize: 11,
                          display: "inline-flex", alignItems: "center", gap: 5,
                          transition: "all 150ms ease",
                        }}
                        onMouseEnter={e => {
                          const b = e.currentTarget as HTMLButtonElement;
                          b.style.background = "var(--red-soft)";
                          b.style.borderColor = "var(--red-ring)";
                          b.style.color = "var(--red)";
                        }}
                        onMouseLeave={e => {
                          const b = e.currentTarget as HTMLButtonElement;
                          b.style.background = "transparent";
                          b.style.borderColor = "var(--neutral-700)";
                          b.style.color = "var(--neutral-500)";
                        }}
                      >
                        <Ban style={{ width: 11, height: 11 }} />
                        {isCancelingThis ? "Canceling..." : "Cancel"}
                      </button>
                    )}
                  </td>
                </motion.tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
