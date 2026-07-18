"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import { apiFetch, formatDate } from "@/lib/api";
import { StatusBadge } from "@/components/ui/status-dot";
import { toast } from "@/components/ui/toast";
import {
  ArrowLeft, Mail, Calendar, Hash, Edit2, Trash2,
  Check, X, AlertCircle, FileText, Repeat2
} from "lucide-react";

interface Customer {
  id: string;
  name: string;
  email: string;
  phone?: string;
  address?: string;
  metadata?: Record<string, string>;
  activeSubscriptions: number;
  createdAt: string;
  updatedAt: string;
}

interface Subscription {
  id: string;
  planName: string;
  status: string;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  canceledAt: string | null;
}

interface Invoice {
  id: string;
  invoiceNumber: string;
  totalCents: number;
  status: string;
  periodStart: string;
  dueAt: string;
  paidAt: string | null;
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
function EditModal({
  customer,
  onClose,
  onSaved,
}: {
  customer: Customer;
  onClose: () => void;
  onSaved: (c: Customer) => void;
}) {
  const { data: session } = useSession();
  const [name, setName] = useState(customer.name);
  const [email, setEmail] = useState(customer.email);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState("");

  useEffect(() => {
    const handle = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", handle);
    return () => window.removeEventListener("keydown", handle);
  }, [onClose]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!session) return;
    setSaving(true); setErr("");
    try {
      const res = await apiFetch<Customer>(`/customers/${customer.id}`, session, {
        method: "PUT",
        body: JSON.stringify({ name, email }),
      });
      toast.success("Customer updated");
      onSaved(res);
    } catch (e: any) {
      setErr(e.message ?? "Failed to update");
    } finally {
      setSaving(false);
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
        role="dialog" aria-modal="true" aria-label="Edit customer"
      >
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 20 }}>
          <h3 style={{ fontSize: 15, fontWeight: 500, color: "var(--neutral-50)" }}>Edit Customer</h3>
          <button onClick={onClose} aria-label="Close"
            style={{ background: "none", border: "none", cursor: "pointer", color: "var(--neutral-500)", padding: 4, borderRadius: 4, lineHeight: 0 }}
            onMouseEnter={e => (e.currentTarget.style.color = "var(--neutral-200)")}
            onMouseLeave={e => (e.currentTarget.style.color = "var(--neutral-500)")}
          >
            <X style={{ width: 16, height: 16 }} />
          </button>
        </div>
        <form onSubmit={submit}>
          <div style={{ marginBottom: 14 }}>
            <label style={{ display: "block", fontSize: 12, color: "var(--neutral-400)", marginBottom: 6, fontWeight: 500 }}>
              Full Name
            </label>
            <input className="input" value={name} onChange={e => setName(e.target.value)} required autoFocus />
          </div>
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: "block", fontSize: 12, color: "var(--neutral-400)", marginBottom: 6, fontWeight: 500 }}>
              Email
            </label>
            <input className="input" type="email" value={email} onChange={e => setEmail(e.target.value)} required />
          </div>
          {err && (
            <div style={{ display: "flex", alignItems: "center", gap: 7, padding: "9px 12px", marginBottom: 14, background: "var(--red-soft)", border: "1px solid var(--red-ring)", borderRadius: 6, fontSize: 12, color: "var(--red)" }}>
              <AlertCircle style={{ width: 13, height: 13, flexShrink: 0 }} /> {err}
            </div>
          )}
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Saving..." : <><Check style={{ width: 13, height: 13 }} /> Save Changes</>}
            </button>
          </div>
        </form>
      </motion.div>
    </>
  );
}

// ── Delete Confirm Modal ─────────────────────────────────────
function DeleteModal({
  customer,
  onClose,
  onDeleted,
}: {
  customer: Customer;
  onClose: () => void;
  onDeleted: () => void;
}) {
  const { data: session } = useSession();
  const [deleting, setDeleting] = useState(false);
  const [err, setErr] = useState("");

  useEffect(() => {
    const handle = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", handle);
    return () => window.removeEventListener("keydown", handle);
  }, [onClose]);

  const confirm = async () => {
    if (!session) return;
    setDeleting(true); setErr("");
    try {
      await apiFetch(`/customers/${customer.id}`, session, { method: "DELETE" });
      toast.success(`${customer.name} deleted`);
      onDeleted();
    } catch (e: any) {
      setErr(e.message ?? "Failed to delete");
      setDeleting(false);
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
        role="dialog" aria-modal="true" aria-label="Confirm delete"
      >
        <h3 style={{ fontSize: 15, fontWeight: 500, color: "var(--neutral-50)", marginBottom: 10 }}>Delete customer?</h3>
        <p style={{ fontSize: 13, color: "var(--neutral-400)", lineHeight: 1.6, marginBottom: 20 }}>
          <strong style={{ color: "var(--neutral-200)" }}>{customer.name}</strong> and all associated data
          will be permanently removed. This action cannot be undone.
        </p>
        {err && (
          <div style={{ display: "flex", alignItems: "center", gap: 7, padding: "9px 12px", marginBottom: 16, background: "var(--red-soft)", border: "1px solid var(--red-ring)", borderRadius: 6, fontSize: 12, color: "var(--red)" }}>
            <AlertCircle style={{ width: 13, height: 13 }} /> {err}
          </div>
        )}
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button className="btn btn-ghost" onClick={onClose} disabled={deleting}>Cancel</button>
          <button className="btn btn-danger" onClick={confirm} disabled={deleting}>
            <Trash2 style={{ width: 13, height: 13 }} />
            {deleting ? "Deleting..." : "Delete Customer"}
          </button>
        </div>
      </motion.div>
    </>
  );
}

// ── Page ─────────────────────────────────────────────────────
export default function CustomerDetailPage() {
  const { data: session } = useSession();
  const { id } = useParams() as { id: string };
  const router = useRouter();

  const [customer, setCustomer] = useState<Customer | null>(null);
  const [subs, setSubs] = useState<Subscription[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showEdit, setShowEdit] = useState(false);
  const [showDelete, setShowDelete] = useState(false);

  const load = async () => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const [cust, subsRes, invRes] = await Promise.all([
        apiFetch<Customer>(`/customers/${id}`, session),
        apiFetch<{ content: Subscription[] }>(`/subscriptions?customerId=${id}&limit=50`, session).catch(() => ({ content: [] })),
        apiFetch<{ content: Invoice[] }>(`/invoices?customerId=${id}&limit=50`, session).catch(() => ({ content: [] })),
      ]);
      setCustomer(cust);
      setSubs(subsRes.content ?? []);
      setInvoices(invRes.content ?? []);
    } catch (e: any) {
      setError(e.message ?? "Failed to load customer");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session, id]);

  // ── Skeleton ──────────────────────────────────────────────
  if (loading) {
    return (
      <div style={{ maxWidth: 900 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 32 }}>
          <div className="skeleton" style={{ width: 32, height: 32, borderRadius: 6 }} />
          <div className="skeleton" style={{ width: 180, height: 22, borderRadius: 4 }} />
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "280px 1fr", gap: 20 }}>
          <div className="skeleton" style={{ height: 280, borderRadius: 10 }} />
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div className="skeleton" style={{ height: 180, borderRadius: 10 }} />
            <div className="skeleton" style={{ height: 180, borderRadius: 10 }} />
          </div>
        </div>
      </div>
    );
  }

  if (error || !customer) {
    return (
      <div style={{ maxWidth: 900 }}>
        <button onClick={() => router.push("/dashboard/customers")} className="btn btn-ghost btn-sm" style={{ marginBottom: 24, gap: 6 }}>
          <ArrowLeft style={{ width: 14, height: 14 }} /> Back to Customers
        </button>
        <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "16px 20px", background: "var(--red-soft)", border: "1px solid var(--red-ring)", borderRadius: 8, fontSize: 13, color: "var(--red)" }}>
          <AlertCircle style={{ width: 15, height: 15, flexShrink: 0 }} />
          <span>{error ?? "Customer not found."}</span>
          <button onClick={load} style={{ marginLeft: "auto", fontSize: 12, background: "none", border: "1px solid currentColor", borderRadius: 4, padding: "2px 8px", color: "inherit", cursor: "pointer" }}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
      style={{ maxWidth: 900 }}
    >
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 28 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <button
            onClick={() => router.push("/dashboard/customers")}
            className="btn btn-ghost btn-sm"
            style={{ padding: "5px 8px" }}
            aria-label="Back to customers"
          >
            <ArrowLeft style={{ width: 14, height: 14 }} />
          </button>
          <div>
            <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
              {customer.name}
            </h1>
            <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 2 }}>{customer.email}</p>
          </div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setShowEdit(true)}
            style={{ gap: 5 }}
          >
            <Edit2 style={{ width: 12, height: 12 }} /> Edit
          </button>
          <button
            className="btn btn-sm"
            onClick={() => setShowDelete(true)}
            style={{
              background: "var(--red-soft)", color: "var(--red)",
              border: "1px solid var(--red-ring)", gap: 5,
            }}
            onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.background = "var(--red)"; (e.currentTarget as HTMLButtonElement).style.color = "white"; }}
            onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.background = "var(--red-soft)"; (e.currentTarget as HTMLButtonElement).style.color = "var(--red)"; }}
          >
            <Trash2 style={{ width: 12, height: 12 }} /> Delete
          </button>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "260px 1fr", gap: 20, alignItems: "start" }}>
        {/* Profile Card */}
        <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, padding: 20 }}>
          {/* Avatar */}
          <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginBottom: 20, paddingBottom: 20, borderBottom: "1px solid var(--neutral-800)" }}>
            <div style={{
              width: 60, height: 60, borderRadius: "50%",
              background: "var(--neutral-800)", border: "1px solid var(--neutral-700)",
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 22, fontWeight: 500, color: "var(--neutral-300)", marginBottom: 12,
            }}>
              {customer.name[0]?.toUpperCase()}
            </div>
            <div style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-100)", marginBottom: 4, textAlign: "center" }}>
              {customer.name}
            </div>
            <div style={{
              display: "inline-flex", alignItems: "center", gap: 4,
              padding: "2px 8px", borderRadius: 12,
              background: customer.activeSubscriptions > 0 ? "var(--green-soft)" : "var(--neutral-800)",
              border: `1px solid ${customer.activeSubscriptions > 0 ? "var(--green-ring)" : "var(--neutral-700)"}`,
              fontSize: 11, fontWeight: 500,
              color: customer.activeSubscriptions > 0 ? "var(--green)" : "var(--neutral-500)",
            }}>
              {customer.activeSubscriptions > 0 ? `${customer.activeSubscriptions} active sub${customer.activeSubscriptions > 1 ? "s" : ""}` : "No active subscriptions"}
            </div>
          </div>

          {/* Details */}
          <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            {[
              { icon: Mail, label: "Email", value: customer.email },
              { icon: Calendar, label: "Customer since", value: formatDate(customer.createdAt) },
              { icon: Hash, label: "Customer ID", value: customer.id.slice(0, 8) + "…", mono: true },
            ].map(({ icon: Icon, label, value, mono }) => (
              <div key={label} style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
                <Icon style={{ width: 13, height: 13, color: "var(--neutral-500)", marginTop: 2, flexShrink: 0 }} />
                <div>
                  <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 2 }}>{label}</div>
                  <div style={{ fontSize: 12, color: "var(--neutral-200)", fontFamily: mono ? "var(--font-mono)" : undefined }}>
                    {value}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Column */}
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {/* Subscriptions */}
          <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, overflow: "hidden" }}>
            <div style={{ padding: "16px 20px", borderBottom: "1px solid var(--neutral-800)", display: "flex", alignItems: "center", gap: 8 }}>
              <Repeat2 style={{ width: 14, height: 14, color: "var(--neutral-400)" }} />
              <h2 style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)" }}>Subscriptions</h2>
              <span style={{ marginLeft: "auto", fontSize: 11, color: "var(--neutral-500)" }}>{subs.length} total</span>
            </div>

            {subs.length === 0 ? (
              <div style={{ padding: "32px 20px", textAlign: "center", color: "var(--neutral-500)", fontSize: 13 }}>
                No subscriptions yet.
              </div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Plan</th>
                    <th>Status</th>
                    <th>Period</th>
                  </tr>
                </thead>
                <tbody>
                  {subs.map(sub => (
                    <tr
                      key={sub.id}
                      onClick={() => router.push(`/dashboard/subscriptions/${sub.id}`)}
                      style={{ cursor: "pointer", opacity: sub.status === "CANCELED" ? 0.55 : 1 }}
                    >
                      <td style={{ fontWeight: 450, color: "var(--neutral-100)" }}>{sub.planName}</td>
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

          {/* Invoices */}
          <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 10, overflow: "hidden" }}>
            <div style={{ padding: "16px 20px", borderBottom: "1px solid var(--neutral-800)", display: "flex", alignItems: "center", gap: 8 }}>
              <FileText style={{ width: 14, height: 14, color: "var(--neutral-400)" }} />
              <h2 style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)" }}>Invoices</h2>
              <span style={{ marginLeft: "auto", fontSize: 11, color: "var(--neutral-500)" }}>{invoices.length} total</span>
            </div>

            {invoices.length === 0 ? (
              <div style={{ padding: "32px 20px", textAlign: "center", color: "var(--neutral-500)", fontSize: 13 }}>
                No invoices generated yet.
              </div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Invoice</th>
                    <th>Period</th>
                    <th style={{ textAlign: "right" }}>Amount</th>
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
                      <td style={{ textAlign: "right" }}>
                        <StatusBadge status={inv.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {/* Modals */}
      <AnimatePresence>
        {showEdit && (
          <EditModal
            customer={customer}
            onClose={() => setShowEdit(false)}
            onSaved={c => { setCustomer(c); setShowEdit(false); }}
          />
        )}
        {showDelete && (
          <DeleteModal
            customer={customer}
            onClose={() => setShowDelete(false)}
            onDeleted={() => router.push("/dashboard/customers")}
          />
        )}
      </AnimatePresence>
    </motion.div>
  );
}
