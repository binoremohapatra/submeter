"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { apiFetch, formatDate } from "@/lib/api";
import { motion, AnimatePresence } from "framer-motion";
import { AlertCircle, ChevronDown, ChevronUp, Clock } from "lucide-react";

interface AuditLog {
  id: string;
  action: "CREATE" | "UPDATE" | "DELETE" | "DUNNING_EMAIL_SENT" | "TRIAL_EXPIRED";
  entityType: string;
  entityId: string;
  actorEmail: string | null;
  createdAt: string;
  diff: any | null;
}

const ACTION_COLORS: Record<string, string> = {
  CREATE: "var(--green)",
  UPDATE: "var(--blue)",
  DELETE: "var(--red)",
  DUNNING_EMAIL_SENT: "var(--amber)",
  TRIAL_EXPIRED: "var(--neutral-400)",
};

const ACTION_BGS: Record<string, string> = {
  CREATE: "var(--green-soft)",
  UPDATE: "var(--blue-soft)",
  DELETE: "var(--red-soft)",
  DUNNING_EMAIL_SENT: "var(--amber-soft)",
  TRIAL_EXPIRED: "var(--neutral-800)",
};



function DiffView({ oldObj, newObj }: { oldObj: any; newObj: any }) {
  if (!oldObj && !newObj) return null;

  const oldKeys = oldObj ? Object.keys(oldObj) : [];
  const newKeys = newObj ? Object.keys(newObj) : [];
  const allKeys = Array.from(new Set([...oldKeys, ...newKeys])).sort();

  if (allKeys.length === 0) {
    return <div style={{ padding: 16, color: "var(--neutral-500)", fontSize: 12 }}>No changes recorded.</div>;
  }

  return (
    <div style={{ background: "var(--neutral-900)" }}>
      <div style={{
        display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 16,
        borderBottom: "1px solid var(--neutral-800)",
        fontSize: 10, padding: "8px 16px", color: "var(--neutral-500)",
        textTransform: "uppercase", letterSpacing: "0.05em",
        background: "var(--neutral-950)"
      }}>
        <div>Field</div>
        <div>Before</div>
        <div>After</div>
      </div>
      <div style={{ display: "flex", flexDirection: "column" }}>
        {allKeys.map((key, i) => {
          const oldVal = oldObj ? oldObj[key] : undefined;
          const newVal = newObj ? newObj[key] : undefined;
          const isAdded = oldVal === undefined && newVal !== undefined;
          const isRemoved = oldVal !== undefined && newVal === undefined;
          const isChanged = oldVal !== undefined && newVal !== undefined && oldVal !== newVal;
          
          const formatVal = (v: any) => {
            if (v === null) return "null";
            if (typeof v === "string") return v;
            return JSON.stringify(v);
          };

          return (
            <div key={key} style={{
              display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 16,
              padding: "10px 16px", fontSize: 12, fontFamily: "var(--font-mono)",
              borderBottom: i === allKeys.length - 1 ? "none" : "1px solid var(--neutral-800)",
              background: isAdded ? "var(--green-soft)" : isRemoved ? "var(--red-soft)" : isChanged ? "rgba(255,255,255,0.02)" : "transparent"
            }}>
              <div style={{ color: "var(--neutral-300)" }}>{key}</div>
              <div style={{ color: isRemoved || isChanged ? "var(--red)" : "var(--neutral-600)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                {oldVal !== undefined ? formatVal(oldVal) : <span style={{ fontStyle: "italic", opacity: 0.5 }}>-</span>}
              </div>
              <div style={{ color: isAdded || isChanged ? "var(--green)" : "var(--neutral-600)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                {newVal !== undefined ? formatVal(newVal) : <span style={{ fontStyle: "italic", opacity: 0.5 }}>-</span>}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function AuditLogPage() {
  const { data: session } = useSession();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const load = async () => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const res = await apiFetch<{ content: AuditLog[] }>("/audit-log?limit=50", session);
      setLogs(res.content);
    } catch (e: any) {
      setError(e.message ?? "Failed to load audit logs");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session]);

  const toggle = (id: string) => {
    setExpanded(prev => {
      const s = new Set(prev);
      s.has(id) ? s.delete(id) : s.add(id);
      return s;
    });
  };

  return (
    <div style={{ maxWidth: 1100 }}>
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
            Audit Log
          </h1>
          <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 3 }}>
            Immutable record of all mutations and system events
          </p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 12, color: "var(--neutral-400)", background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", padding: "4px 10px", borderRadius: 6 }}>
          <Clock style={{ width: 12, height: 12 }} />
          Retained for 90 days
        </div>
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
        {/* Header row */}
        <div style={{
          display: "grid", gridTemplateColumns: "140px 180px 100px 1fr 40px", gap: 16,
          padding: "10px 16px", borderBottom: "1px solid var(--neutral-800)",
          fontSize: 11, fontWeight: 500, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--neutral-500)",
        }}>
          <div>Timestamp</div>
          <div>Actor</div>
          <div>Action</div>
          <div>Entity ID</div>
          <div />
        </div>

        {/* Rows */}
        <div>
          {loading && Array.from({ length: 6 }).map((_, i) => (
            <div key={i} style={{ display: "grid", gridTemplateColumns: "140px 180px 100px 1fr 40px", gap: 16, padding: "12px 16px", borderBottom: "1px solid var(--neutral-800)" }}>
              {[100, 140, 60, 200, 20].map((w, j) => (
                <div key={j} className="skeleton" style={{ height: 12, width: w, borderRadius: 3 }} />
              ))}
            </div>
          ))}

          {!loading && logs.length === 0 && (
            <div style={{ padding: "60px 24px", textAlign: "center", color: "var(--neutral-600)", fontSize: 13 }}>
              No audit logs recorded yet.
            </div>
          )}

          {!loading && logs.map((log, i) => {
            const isExpanded = expanded.has(log.id);
            const hasPayload = log.diff && Object.keys(log.diff).length > 0;
            const c = ACTION_COLORS[log.action] ?? "var(--neutral-400)";
            const bg = ACTION_BGS[log.action] ?? "var(--neutral-800)";

            return (
              <motion.div
                key={log.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: i * 0.02 }}
                style={{ borderBottom: "1px solid var(--neutral-800)" }}
              >
                {/* Row */}
                <div
                  onClick={() => hasPayload && toggle(log.id)}
                  style={{
                    display: "grid", gridTemplateColumns: "140px 180px 100px 1fr 40px", gap: 16,
                    padding: "10px 16px", alignItems: "center",
                    cursor: hasPayload ? "pointer" : "default",
                    background: isExpanded ? "var(--neutral-800)" : "transparent",
                    transition: "background 150ms ease",
                  }}
                  onMouseEnter={e => { if (hasPayload && !isExpanded) e.currentTarget.style.background = "rgba(255,255,255,0.02)"; }}
                  onMouseLeave={e => { if (hasPayload && !isExpanded) e.currentTarget.style.background = "transparent"; }}
                >
                  <div style={{ fontSize: 12, color: "var(--neutral-400)", fontFamily: "var(--font-mono)" }}>
                    {formatDate(log.createdAt).replace(", ", " ")}
                  </div>
                  <div style={{ fontSize: 12, color: "var(--neutral-200)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    {log.actorEmail ?? <span style={{ color: "var(--neutral-500)", fontStyle: "italic" }}>System</span>}
                  </div>
                  <div>
                    <span style={{
                      display: "inline-block", padding: "3px 6px", borderRadius: 4,
                      fontSize: 10, fontWeight: 500, color: c, background: bg,
                      fontFamily: "var(--font-mono)",
                    }}>
                      {log.action}
                    </span>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, overflow: "hidden" }}>
                    <span style={{ fontSize: 11, color: "var(--neutral-500)", textTransform: "uppercase" }}>{log.entityType}</span>
                    <span style={{ fontSize: 12, color: "var(--neutral-300)", fontFamily: "var(--font-mono)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{log.entityId.split('-')[0]}</span>
                  </div>
                  <div style={{ display: "flex", justifyContent: "flex-end" }}>
                    {hasPayload && (
                      <button style={{
                        background: "none", border: "none", color: "var(--neutral-500)",
                        padding: 4, borderRadius: 4, cursor: "pointer",
                      }}>
                        {isExpanded ? <ChevronUp style={{ width: 14, height: 14 }} /> : <ChevronDown style={{ width: 14, height: 14 }} />}
                      </button>
                    )}
                  </div>
                </div>

                {/* Expanded Payload */}
                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
                      style={{ overflow: "hidden" }}
                    >
                      <div style={{ padding: "0 16px 16px 16px", background: "var(--neutral-800)" }}>
                        <div style={{ border: "1px solid var(--neutral-700)", borderRadius: 6, overflow: "hidden" }}>
                          <DiffView
                            oldObj={
                              log.action === "UPDATE" ? log.diff?.before :
                              log.action === "DELETE" ? log.diff : null
                            }
                            newObj={
                              log.action === "UPDATE" ? log.diff?.after :
                              log.action === "CREATE" ? log.diff : null
                            }
                          />
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
