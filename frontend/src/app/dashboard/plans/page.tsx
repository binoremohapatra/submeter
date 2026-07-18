"use client";

import React, { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { motion } from "framer-motion";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { ChevronDown, ChevronUp, AlertCircle } from "lucide-react";

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
function TierTable({ tiers }: { tiers: PlanTier[] }) {
  return (
    <div style={{
      background: "var(--neutral-800)",
      borderTop: "1px solid var(--neutral-700)",
    }}>
      <table className="data-table" style={{ fontSize: 12 }}>
        <thead>
          <tr>
            <th style={{ paddingLeft: 32 }}>Tier</th>
            <th>Up to</th>
            <th style={{ textAlign: "right" }}>Unit Amount</th>
            <th style={{ textAlign: "right" }}>Flat Fee</th>
          </tr>
        </thead>
        <tbody>
          {tiers.map(t => (
            <tr key={t.tierOrder}>
              <td style={{ paddingLeft: 32, color: "var(--neutral-500)" }}>#{t.tierOrder + 1}</td>
              <td style={{ fontFamily: "var(--font-mono)", color: "var(--neutral-300)" }}>
                {t.upTo == null ? "∞" : t.upTo.toLocaleString()}
              </td>
              <td style={{ textAlign: "right", fontFamily: "var(--font-mono)", color: "var(--neutral-200)" }}>
                {formatINR(t.unitAmount)}
                <span style={{ color: "var(--neutral-600)", marginLeft: 4 }}>/unit</span>
              </td>
              <td style={{ textAlign: "right", fontFamily: "var(--font-mono)", color: "var(--neutral-400)" }}>
                {t.flatFee > 0 ? formatINR(t.flatFee) : "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}


const MODEL_LABEL: Record<string, string> = {
  FLAT_RATE: "Flat Rate",
  PER_UNIT: "Per Unit",
  TIERED: "Tiered",
  VOLUME: "Volume",
};
const INTERVAL_LABEL: Record<string, string> = {
  MONTHLY: "mo",
  ANNUAL: "yr",
};
export default function PlansPage() {
  const { data: session } = useSession();
  const router = useRouter();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const load = async () => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const res = await apiFetch<{ content: Plan[] }>("/plans?limit=50", session);
      setPlans(res.content);
    } catch (e: any) {
      setError(e.message ?? "Failed to load plans");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session]);

  const toggle = (id: string) =>
    setExpanded(prev => {
      const s = new Set(prev);
      s.has(id) ? s.delete(id) : s.add(id);
      return s;
    });

  return (
    <div style={{ maxWidth: 1100 }}>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
          Plans
        </h1>
        <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 3 }}>
          Pricing plans available to your customers
        </p>
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
              <th>Name</th>
              <th>Model</th>
              <th>Interval</th>
              <th style={{ textAlign: "right" }}>Price</th>
              <th style={{ textAlign: "right" }}>Trial</th>
              <th style={{ width: 40 }} />
            </tr>
          </thead>
          <tbody>
            {loading && Array.from({ length: 3 }, (_, i) => (
              <tr key={i}>
                {[140, 80, 70, 80, 50, 20].map((w, j) => (
                  <td key={j}>
                    <div className="skeleton" style={{ height: 12, width: w, borderRadius: 3 }} />
                  </td>
                ))}
              </tr>
            ))}

            {!loading && plans.length === 0 && (
              <tr>
                <td colSpan={6} style={{ textAlign: "center", padding: "60px 24px", color: "var(--neutral-600)", fontSize: 13 }}>
                  No plans found.
                </td>
              </tr>
            )}

            {!loading && plans.map((plan, i) => {
              const hasTiers = plan.pricingModel !== "FLAT" && (plan.tiers?.length ?? 0) > 0;
              const isExpanded = expanded.has(plan.id);

              return (
                <React.Fragment key={plan.id}>
                  <motion.tr
                    initial={{ opacity: 0, y: 4 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.04 }}
                    onClick={() => hasTiers ? toggle(plan.id) : router.push(`/dashboard/plans/${plan.id}`)}
                    style={{
                      cursor: "pointer",
                      opacity: plan.archived ? 0.45 : 1,
                    }}
                  >
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <span style={{ fontWeight: 450, color: "var(--neutral-100)" }}>{plan.name}</span>
                        {plan.archived && (
                          <span style={{
                            fontSize: 10, fontWeight: 500, letterSpacing: "0.06em",
                            color: "var(--neutral-600)", background: "var(--neutral-800)",
                            border: "1px solid var(--neutral-700)",
                            borderRadius: 3, padding: "1px 5px", textTransform: "uppercase",
                          }}>
                            Archived
                          </span>
                        )}
                      </div>
                    </td>
                    <td>
                      <span style={{ fontSize: 12, color: "var(--neutral-400)" }}>
                        {MODEL_LABEL[plan.pricingModel]}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontSize: 12, color: "var(--neutral-400)" }}>
                        {INTERVAL_LABEL[plan.billingInterval]}
                      </span>
                    </td>
                    <td style={{ textAlign: "right", fontFamily: "var(--font-mono)", fontSize: 12 }}>
                      {plan.flatAmount != null
                        ? <span style={{ color: "var(--neutral-200)" }}>{formatINR(plan.flatAmount)}</span>
                        : <span style={{ color: "var(--neutral-500)" }}>Tiered</span>
                      }
                    </td>
                    <td style={{ textAlign: "right", fontSize: 12, color: "var(--neutral-500)" }}>
                      {plan.trialDays > 0 ? `${plan.trialDays}d` : "—"}
                    </td>
                    <td style={{ textAlign: "right" }}>
                      {hasTiers && (
                        <button style={{
                          background: "none", border: "none", cursor: "pointer",
                          color: "var(--neutral-500)", padding: 4, borderRadius: 3,
                          transition: "color 150ms ease",
                        }}
                          onMouseEnter={e => (e.currentTarget.style.color = "var(--neutral-300)")}
                          onMouseLeave={e => (e.currentTarget.style.color = "var(--neutral-500)")}
                        >
                          {isExpanded
                            ? <ChevronUp style={{ width: 14, height: 14 }} />
                            : <ChevronDown style={{ width: 14, height: 14 }} />
                          }
                        </button>
                      )}
                    </td>
                  </motion.tr>

                  {isExpanded && hasTiers && (
                    <tr>
                      <td colSpan={6} style={{ padding: 0 }}>
                        <TierTable tiers={plan.tiers!} />
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
