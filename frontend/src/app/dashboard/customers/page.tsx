"use client";

import { useEffect, useState, useRef } from "react";
import { useSession } from "next-auth/react";
import { motion } from "framer-motion";
import { useRouter } from "next/navigation";
import { apiFetch, formatDate, API_BASE } from "@/lib/api";
import { StatusBadge } from "@/components/ui/status-dot";
import { toast } from "@/components/ui/toast";
import { Search, Plus, Download, ChevronLeft, ChevronRight, X, AlertCircle } from "lucide-react";

interface Customer {
  id: string;
  name: string;
  email: string;
  activeSubscriptions: number;
  createdAt: string;
}

// ── Empty State ─────────────────────────────────────────────
function EmptyState({ onNew }: { onNew: () => void }) {
  return (
    <div style={{
      padding: "80px 24px",
      display: "flex", flexDirection: "column", alignItems: "center",
      color: "var(--neutral-500)", textAlign: "center",
    }}>
      <div style={{
        width: 40, height: 40, borderRadius: 10,
        background: "var(--neutral-800)", border: "1px solid var(--neutral-700)",
        display: "flex", alignItems: "center", justifyContent: "center",
        marginBottom: 16,
      }}>
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path d="M9 3v12M3 9h12" stroke="var(--neutral-500)" strokeWidth="1.5" strokeLinecap="round"/>
        </svg>
      </div>
      <div style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-300)", marginBottom: 6 }}>
        No customers yet
      </div>
      <div style={{ fontSize: 13, marginBottom: 20, maxWidth: 280, lineHeight: 1.5 }}>
        Add your first customer to start creating subscriptions and invoices.
      </div>
      <button className="btn btn-primary" onClick={onNew}>
        <Plus style={{ width: 13, height: 13 }} />
        New Customer
      </button>
    </div>
  );
}

// ── New Customer Modal ──────────────────────────────────────
function NewCustomerModal({ onClose, onCreated }: {
  onClose: () => void;
  onCreated: (c: Customer) => void;
}) {
  const { data: session } = useSession();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => { inputRef.current?.focus(); }, []);

  useEffect(() => {
    const handle = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handle);
    return () => window.removeEventListener("keydown", handle);
  }, [onClose]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!session) return;
    setSaving(true); setErr("");
    try {
      const res = await apiFetch<Customer>("/customers", session, {
        method: "POST",
        body: JSON.stringify({ name, email }),
      });
      toast.success(`${res.name} added`);
      onCreated(res);
    } catch (e: any) {
      setErr(e.message ?? "Failed to create customer");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.15 }}
        onClick={onClose}
        style={{
          position: "fixed", inset: 0, zIndex: 100,
          background: "rgba(0,0,0,0.7)",
          backdropFilter: "blur(4px)",
        }}
      />
      {/* Modal */}
      <motion.div
        initial={{ opacity: 0, scale: 0.97, y: 8 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.97, y: 4 }}
        transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
        style={{
          position: "fixed", top: "50%", left: "50%",
          transform: "translate(-50%, -50%)",
          zIndex: 101,
          background: "var(--neutral-900)",
          border: "1px solid var(--neutral-700)",
          borderRadius: 10,
          padding: 24,
          width: 400,
          boxShadow: "0 24px 64px rgba(0,0,0,0.6)",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 20 }}>
          <h3 style={{ fontSize: 15, fontWeight: 500, color: "var(--neutral-50)" }}>
            New Customer
          </h3>
          <button onClick={onClose} style={{ background: "none", border: "none", cursor: "pointer", color: "var(--neutral-500)", padding: 4, borderRadius: 4, lineHeight: 0 }}>
            <X style={{ width: 16, height: 16 }} />
          </button>
        </div>

        <form onSubmit={submit}>
          <div style={{ marginBottom: 14 }}>
            <label style={{ display: "block", fontSize: 12, color: "var(--neutral-400)", marginBottom: 6, fontWeight: 500 }}>
              Full Name
            </label>
            <input
              ref={inputRef}
              className="input"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Acme Corp"
              required
            />
          </div>

          <div style={{ marginBottom: 20 }}>
            <label style={{ display: "block", fontSize: 12, color: "var(--neutral-400)", marginBottom: 6, fontWeight: 500 }}>
              Email
            </label>
            <input
              className="input"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="billing@acme.com"
              required
            />
          </div>

          {err && (
            <div style={{
              display: "flex", alignItems: "center", gap: 7,
              padding: "9px 12px", marginBottom: 14,
              background: "var(--red-soft)", border: "1px solid var(--red-ring)",
              borderRadius: 6, fontSize: 12, color: "var(--red)",
            }}>
              <AlertCircle style={{ width: 13, height: 13, flexShrink: 0 }} />
              {err}
            </div>
          )}

          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Creating..." : "Create Customer"}
            </button>
          </div>
        </form>
      </motion.div>
    </>
  );
}

// ── Page ─────────────────────────────────────────────────────
export default function CustomersPage() {
  const { data: session } = useSession();
  const router = useRouter();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [cursorStack, setCursorStack] = useState<string[]>([]);
  const [showNew, setShowNew] = useState(false);
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [debouncedSearch, setDebouncedSearch] = useState("");

  useEffect(() => {
    if (searchTimeout.current) clearTimeout(searchTimeout.current);
    searchTimeout.current = setTimeout(() => setDebouncedSearch(search), 300);
    return () => { if (searchTimeout.current) clearTimeout(searchTimeout.current); };
  }, [search]);

  const load = async (cur: string | null = null) => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const params = new URLSearchParams({ limit: "20" });
      if (cur) params.set("cursor", cur);
      if (debouncedSearch) params.set("search", debouncedSearch);
      const res = await apiFetch<{ content: Customer[]; nextCursor: string | null; hasNext: boolean }>(
        `/customers?${params}`, session
      );
      setCustomers(res.content);
      setCursor(res.nextCursor);
      setHasNext(res.hasNext);
    } catch (e: any) {
      setError(e.message ?? "Failed to load customers");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session, debouncedSearch]);

  const handleExport = async () => {
    if (!session) return;
    try {
      const res = await fetch(`${API_BASE}/customers/export`, {
        headers: { Authorization: `Bearer ${(session as any).accessToken}` },
      });
      if (!res.ok) throw new Error("Export failed");
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url; a.download = "customers.csv"; a.click();
      URL.revokeObjectURL(url);
      toast.success("Exported customers.csv");
    } catch (e: any) { toast.error(e.message ?? "Export failed"); }
  };

  return (
    <div style={{ maxWidth: 1100 }}>
      {/* Header */}
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 24, gap: 16 }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
            Customers
          </h1>
          <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 3 }}>
            {loading ? "Loading..." : `${customers.length} customers`}
          </p>
        </div>
        <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
          <button className="btn btn-secondary btn-sm" onClick={handleExport}>
            <Download style={{ width: 12, height: 12 }} />
            Export CSV
          </button>
          <button className="btn btn-primary btn-sm" onClick={() => setShowNew(true)}>
            <Plus style={{ width: 12, height: 12 }} />
            New Customer
          </button>
        </div>
      </div>

      {/* Search */}
      <div style={{ position: "relative", marginBottom: 16 }}>
        <Search style={{
          position: "absolute", left: 10, top: "50%", transform: "translateY(-50%)",
          width: 13, height: 13, color: "var(--neutral-500)", pointerEvents: "none",
        }} />
        <input
          className="input"
          style={{ paddingLeft: 30, maxWidth: 300 }}
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search customers..."
        />
      </div>

      {/* Error */}
      {error && (
        <div style={{
          display: "flex", alignItems: "center", gap: 8,
          padding: "10px 14px", marginBottom: 16,
          background: "var(--red-soft)", border: "1px solid var(--red-ring)",
          borderRadius: 7, fontSize: 13, color: "var(--red)",
        }}>
          <AlertCircle style={{ width: 14, height: 14 }} />
          {error}
          <button onClick={() => load()} style={{ marginLeft: "auto", fontSize: 12, background: "none", border: "1px solid currentColor", borderRadius: 4, padding: "2px 8px", color: "inherit", cursor: "pointer" }}>
            Retry
          </button>
        </div>
      )}

      {/* Table */}
      <div style={{
        background: "var(--neutral-900)",
        border: "1px solid var(--neutral-800)",
        borderRadius: 8, overflow: "hidden",
        marginBottom: 16,
      }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Subscriptions</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {loading && Array.from({ length: 5 }, (_, i) => (
              <tr key={i}>
                {[120, 180, 60, 80].map((w, j) => (
                  <td key={j}>
                    <div className="skeleton" style={{ height: 12, width: w, borderRadius: 3 }} />
                  </td>
                ))}
              </tr>
            ))}

            {!loading && customers.length === 0 && (
              <tr>
                <td colSpan={4} style={{ padding: 0 }}>
                  <EmptyState onNew={() => setShowNew(true)} />
                </td>
              </tr>
            )}

            {!loading && customers.map((c, i) => (
              <motion.tr
                key={c.id}
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.03, duration: 0.25 }}
                onClick={() => router.push(`/dashboard/customers/${c.id}`)}
                style={{ cursor: "pointer" }}
              >
                <td style={{ fontWeight: 450, color: "var(--neutral-100)" }}>{c.name}</td>
                <td style={{ fontFamily: "var(--font-mono)", fontSize: 12, color: "var(--neutral-400)" }}>
                  {c.email}
                </td>
                <td>
                  <span style={{
                    display: "inline-flex", alignItems: "center", justifyContent: "center",
                    width: 22, height: 22, borderRadius: 4,
                    background: c.activeSubscriptions > 0 ? "var(--neutral-800)" : "transparent",
                    border: c.activeSubscriptions > 0 ? "1px solid var(--neutral-700)" : "1px solid var(--neutral-800)",
                    fontSize: 12, fontWeight: 500, color: "var(--neutral-300)",
                  }}>
                    {c.activeSubscriptions}
                  </span>
                </td>
                <td style={{ fontSize: 12, color: "var(--neutral-500)" }}>
                  {formatDate(c.createdAt)}
                </td>
              </motion.tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {!loading && (cursorStack.length > 0 || hasNext) && (
        <div style={{ display: "flex", gap: 6, justifyContent: "flex-end" }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => {
              const prev = cursorStack.slice(-1)[0] ?? null;
              setCursorStack(s => s.slice(0, -1));
              load(prev);
            }}
            disabled={cursorStack.length === 0}
          >
            <ChevronLeft style={{ width: 13, height: 13 }} /> Previous
          </button>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => {
              if (!cursor) return;
              setCursorStack(s => [...s, cursor]);
              load(cursor);
            }}
            disabled={!hasNext}
          >
            Next <ChevronRight style={{ width: 13, height: 13 }} />
          </button>
        </div>
      )}

      {/* Modal */}
      {showNew && (
        <NewCustomerModal
          onClose={() => setShowNew(false)}
          onCreated={c => {
            setCustomers(prev => [c, ...prev]);
            setShowNew(false);
          }}
        />
      )}
    </div>
  );
}
