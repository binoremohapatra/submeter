"use client";

"use client";

import { useEffect, useState, useRef } from "react";
import { useSession } from "next-auth/react";
import { useParams, useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import Script from "next/script";
import { apiFetch, formatDate } from "@/lib/api";
import { StatusBadge } from "@/components/ui/status-dot";
import { toast } from "@/components/ui/toast";
import { ArrowLeft, Download, CreditCard, ExternalLink, AlertCircle, Copy, Check } from "lucide-react";

interface Invoice {
  id: string;
  invoiceNumber: string;
  customerName: string;
  customerEmail: string;
  periodStart: string;
  periodEnd: string;
  totalCents: number;
  subtotalCents: number;
  taxCents: number;
  status: "DRAFT" | "OPEN" | "PAID" | "VOID" | "UNCOLLECTIBLE";
  dueAt: string;
  paidAt: string | null;
  createdAt: string;
  lineItems: {
    id: string;
    description: string;
    quantity: number;
    unitAmount: number;
    amount: number;
  }[];
  payments: {
    id: string;
    amountCents: number;
    currency: string;
    status: "PENDING" | "SUCCESS" | "FAILED";
    razorpayOrderId: string;
    razorpayPaymentId: string | null;
    createdAt: string;
  }[];
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <button
      onClick={() => {
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }}
      className="btn btn-ghost btn-sm"
      style={{ padding: 4, height: "auto", display: "inline-flex", color: "var(--neutral-400)" }}
      title="Copy"
    >
      <AnimatePresence mode="wait" initial={false}>
        {copied
          ? <motion.span key="check" initial={{ scale: 0.5 }} animate={{ scale: 1 }} exit={{ scale: 0.5 }}>
              <Check style={{ width: 12, height: 12, color: "var(--green)" }} />
            </motion.span>
          : <motion.span key="copy" initial={{ scale: 0.5 }} animate={{ scale: 1 }} exit={{ scale: 0.5 }}>
              <Copy style={{ width: 12, height: 12 }} />
            </motion.span>
        }
      </AnimatePresence>
    </button>
  );
}

/** Animated SVG checkmark for payment success */
function PaidCheckmark() {
  const pathRef = useRef<SVGPathElement>(null);

  useEffect(() => {
    const path = pathRef.current;
    if (!path) return;
    const length = path.getTotalLength();
    path.style.strokeDasharray = String(length);
    path.style.strokeDashoffset = String(length);
    // Trigger paint before animating
    path.getBoundingClientRect();
    path.style.transition = "stroke-dashoffset 500ms cubic-bezier(0.16, 1, 0.3, 1) 200ms";
    path.style.strokeDashoffset = "0";
  }, []);

  return (
    <div style={{ position: "relative", display: "inline-flex", alignItems: "center", justifyContent: "center" }}>
      {/* Glow ring — pulses out once */}
      <motion.div
        initial={{ scale: 1, opacity: 0.6 }}
        animate={{ scale: 1.8, opacity: 0 }}
        transition={{ duration: 0.8, delay: 0.3, ease: "easeOut" }}
        style={{
          position: "absolute",
          width: 24, height: 24,
          borderRadius: "50%",
          background: "var(--green)",
          pointerEvents: "none",
        }}
      />
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none" style={{ position: "relative", zIndex: 1 }}>
        <circle cx="10" cy="10" r="9" stroke="var(--green)" strokeWidth="1.5" fill="rgba(34,197,94,0.1)" />
        <path
          ref={pathRef}
          d="M6 10l3 3 5-5"
          stroke="var(--green)"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
        />
      </svg>
    </div>
  );
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
export default function InvoiceDetailsPage() {
  const { data: session } = useSession();
  const { id } = useParams() as { id: string };
  const router = useRouter();

  const [inv, setInv] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [paying, setPaying] = useState(false);

  const load = async () => {
    if (!session) return;
    setLoading(true); setError(null);
    try {
      const res = await apiFetch<Invoice>(`/invoices/${id}`, session);
      setInv(res);
    } catch (e: any) {
      setError(e.message ?? "Failed to load invoice");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [session, id]);

  const handlePay = async () => {
    if (!session || !inv) return;
    setPaying(true);
    try {
      const { razorpayOrderId, amountCents } = await apiFetch<{ razorpayOrderId: string; amountCents: number }>(
        `/invoices/${inv.id}/pay`, session, { method: "POST" }
      );

      const options = {
        key: "rzp_test_TDlxd8rTABFHMd",
        amount: amountCents,
        currency: "INR",
        name: "SubMeter Demo",
        description: `Invoice ${inv.invoiceNumber}`,
        order_id: razorpayOrderId,
        handler: async function (response: any) {
          try {
            await apiFetch(`/invoices/${inv.id}/verify`, session, { 
              method: "POST", 
              body: JSON.stringify(response) 
            });
            toast.success("Payment successful!");
            load();
          } catch (e: any) {
            toast.error(e.message ?? "Payment verification failed");
          }
        },
        prefill: { name: inv.customerName, email: inv.customerEmail },
        theme: { color: "#111111" },
      };

      const rzp = new (window as any).Razorpay(options);
      rzp.on("payment.failed", function (response: any) {
        toast.error(`Payment failed: ${response.error.description}`);
      });
      rzp.open();
    } catch (e: any) {
      toast.error(e.message ?? "Payment failed");
    } finally {
      setPaying(false);
    }
  };

  const handlePdf = async () => {
    if (!session || !inv) return;
    try {
      const res = await fetch(`/api/invoices/${inv.id}/pdf`, {
        headers: { Authorization: `Bearer ${(session as any).accessToken}` },
      });
      if (!res.ok) throw new Error("PDF generation failed");
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url; a.download = `${inv.invoiceNumber}.pdf`; a.click();
      URL.revokeObjectURL(url);
    } catch (e: any) { toast.error(e.message ?? "PDF failed"); }
  };

  if (loading) {
    return (
      <div style={{ padding: 40, maxWidth: 800, margin: "0 auto" }}>
        <div className="skeleton" style={{ height: 24, width: 200, marginBottom: 40, borderRadius: 4 }} />
        <div className="skeleton" style={{ height: 400, width: "100%", borderRadius: 8 }} />
      </div>
    );
  }

  if (error || !inv) {
    return (
      <div style={{ maxWidth: 800, margin: "0 auto", padding: 40 }}>
        <button onClick={() => router.back()} className="btn btn-ghost btn-sm" style={{ marginBottom: 24 }}>
          <ArrowLeft style={{ width: 14, height: 14 }} /> Back
        </button>
        <div style={{
          display: "flex", alignItems: "center", gap: 8,
          padding: "16px 20px",
          background: "var(--red-soft)", border: "1px solid var(--red-ring)",
          borderRadius: 8, fontSize: 13, color: "var(--red)",
        }}>
          <AlertCircle style={{ width: 16, height: 16 }} />
          {error ?? "Invoice not found"}
        </div>
      </div>
    );
  }

  const isPastDue = inv.status === "OPEN" && new Date(inv.dueAt).getTime() < Date.now();

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
      style={{ maxWidth: 800, margin: "0 auto" }}
    >
      <Script src="https://checkout.razorpay.com/v1/checkout.js" />

      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 24 }}>
        <button onClick={() => router.push("/dashboard/invoices")} className="btn btn-ghost btn-sm" style={{ padding: 4 }}>
          <ArrowLeft style={{ width: 16, height: 16 }} />
        </button>
        <h1 style={{ fontSize: 20, fontWeight: 500, fontFamily: "var(--font-mono)", color: "var(--neutral-50)", letterSpacing: "-0.02em" }}>
          {inv.invoiceNumber}
        </h1>
        <StatusBadge status={inv.status} />
        {isPastDue && (
          <span style={{ fontSize: 11, fontWeight: 500, color: "var(--amber)", background: "var(--amber-soft)", padding: "2px 6px", borderRadius: 4 }}>
            Past Due
          </span>
        )}
      </div>

      {/* Invoice Card */}
      <div style={{
        background: "var(--neutral-900)",
        border: "1px solid var(--neutral-800)",
        borderRadius: 12,
        overflow: "hidden",
        boxShadow: "0 12px 48px rgba(0,0,0,0.3)",
      }}>
        {/* Top actions */}
        <div style={{
          padding: "16px 24px",
          borderBottom: "1px solid var(--neutral-800)",
          display: "flex", justifyContent: "flex-end", gap: 8,
          background: "var(--neutral-950)",
        }}>
          <button onClick={handlePdf} className="btn btn-secondary btn-sm" title="Download PDF">
            <Download style={{ width: 12, height: 12 }} /> PDF
          </button>
          <a
            href={`/invoice/${inv.id}`}
            target="_blank"
            rel="noreferrer"
            className="btn btn-secondary btn-sm"
            title="Public link"
          >
            <ExternalLink style={{ width: 12, height: 12 }} /> Link
          </a>
          {inv.status === "OPEN" && (
            <button
              onClick={handlePay}
              disabled={paying}
              className="btn btn-sm"
              style={{
                background: "var(--neutral-50)", color: "var(--neutral-950)",
                border: "none", display: "inline-flex", gap: 6, alignItems: "center",
              }}
              onMouseEnter={e => (e.currentTarget as HTMLButtonElement).style.background = "white"}
              onMouseLeave={e => (e.currentTarget as HTMLButtonElement).style.background = "var(--neutral-50)"}
            >
              <CreditCard style={{ width: 12, height: 12 }} />
              {paying ? "..." : "Pay Invoice"}
            </button>
          )}
        </div>

        <div style={{ padding: "40px 48px" }}>
          {/* Header info */}
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 60 }}>
            <div>
              <div style={{ fontSize: 24, fontWeight: 600, color: "var(--neutral-50)", letterSpacing: "-0.03em", marginBottom: 24 }}>
                INVOICE
              </div>
              <div style={{ fontSize: 12, color: "var(--neutral-500)", marginBottom: 4 }}>Billed to</div>
              <div style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-100)", marginBottom: 2 }}>
                {inv.customerName}
              </div>
              <div style={{ fontSize: 13, color: "var(--neutral-400)" }}>{inv.customerEmail}</div>
            </div>
            <div style={{ textAlign: "right" }}>
              <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "8px 24px", fontSize: 13 }}>
                <div style={{ color: "var(--neutral-500)" }}>Invoice number</div>
                <div style={{ fontFamily: "var(--font-mono)", color: "var(--neutral-200)" }}>{inv.invoiceNumber}</div>

                <div style={{ color: "var(--neutral-500)" }}>Date of issue</div>
                <div style={{ color: "var(--neutral-200)" }}>{formatDate(inv.createdAt)}</div>

                <div style={{ color: "var(--neutral-500)" }}>Due date</div>
                <div style={{ color: isPastDue ? "var(--amber)" : "var(--neutral-200)", fontWeight: isPastDue ? 500 : 400 }}>
                  {formatDate(inv.dueAt)}
                </div>

                <div style={{ color: "var(--neutral-500)" }}>Billing period</div>
                <div style={{ color: "var(--neutral-200)" }}>
                  {formatDate(inv.periodStart)} – {formatDate(inv.periodEnd)}
                </div>
              </div>
            </div>
          </div>

          {/* Line Items */}
          <table style={{ width: "100%", borderCollapse: "collapse", marginBottom: 40 }}>
            <thead>
              <tr style={{ borderBottom: "1px solid var(--neutral-800)" }}>
                <th style={{ textAlign: "left", padding: "12px 0", fontSize: 11, fontWeight: 500, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--neutral-500)" }}>Description</th>
                <th style={{ textAlign: "right", padding: "12px 0", fontSize: 11, fontWeight: 500, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--neutral-500)" }}>Qty</th>
                <th style={{ textAlign: "right", padding: "12px 0", fontSize: 11, fontWeight: 500, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--neutral-500)" }}>Unit Price</th>
                <th style={{ textAlign: "right", padding: "12px 0", fontSize: 11, fontWeight: 500, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--neutral-500)" }}>Amount</th>
              </tr>
            </thead>
            <tbody>
              {inv.lineItems.map(li => (
                <tr key={li.id} style={{ borderBottom: "1px solid var(--neutral-800)" }}>
                  <td style={{ padding: "16px 0", fontSize: 13, color: "var(--neutral-200)" }}>{li.description}</td>
                  <td style={{ textAlign: "right", padding: "16px 0", fontSize: 13, fontFamily: "var(--font-mono)", color: "var(--neutral-400)" }}>{li.quantity.toLocaleString()}</td>
                  <td style={{ textAlign: "right", padding: "16px 0", fontSize: 13, fontFamily: "var(--font-mono)", color: "var(--neutral-400)" }}>{formatINR(li.unitAmount)}</td>
                  <td style={{ textAlign: "right", padding: "16px 0", fontSize: 13, fontFamily: "var(--font-mono)", color: "var(--neutral-200)", fontWeight: 500 }}>{formatINR(li.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Totals */}
          <div style={{ display: "flex", justifyContent: "flex-end" }}>
            <div style={{ width: 280, display: "grid", gridTemplateColumns: "1fr auto", gap: "12px 24px", fontSize: 13 }}>
              <div style={{ color: "var(--neutral-400)" }}>Subtotal</div>
              <div style={{ textAlign: "right", fontFamily: "var(--font-mono)", color: "var(--neutral-200)" }}>{formatINR(inv.subtotalCents)}</div>

              {inv.taxCents > 0 && (
                <>
                  <div style={{ color: "var(--neutral-400)" }}>
                    Tax ({inv.subtotalCents > 0 ? `${Math.round((inv.taxCents / inv.subtotalCents) * 100)}%` : "0%"})
                  </div>
                  <div style={{ textAlign: "right", fontFamily: "var(--font-mono)", color: "var(--neutral-200)" }}>
                    {formatINR(inv.taxCents)}
                  </div>
                </>
              )}

              <div style={{ gridColumn: "1 / -1", height: 1, background: "var(--neutral-800)", margin: "4px 0" }} />

              <div style={{ color: "var(--neutral-100)", fontWeight: 500, fontSize: 15, alignSelf: "center" }}>Total</div>
              <div style={{ textAlign: "right", fontFamily: "var(--font-mono)", color: "var(--neutral-50)", fontSize: 18, fontWeight: 500 }}>
                {formatINR(inv.totalCents)}
              </div>
            </div>
          </div>
        </div>

        {/* PAID footer — with animated checkmark on load */}
        {inv.status === "PAID" && inv.paidAt && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
            style={{ padding: "20px 48px", background: "var(--green-soft)", borderTop: "1px solid var(--green-ring)" }}
          >
            <div style={{ fontSize: 12, fontWeight: 500, color: "var(--green)", display: "flex", alignItems: "center", gap: 8 }}>
              <PaidCheckmark />
              Paid on {formatDate(inv.paidAt)}
            </div>
          </motion.div>
        )}
      </div>

      {/* Payment History — rows stagger in */}
      {inv.payments && inv.payments.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
          style={{ marginTop: 40 }}
        >
          <h2 style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-100)", marginBottom: 16 }}>Payment History</h2>
          <div style={{ background: "var(--neutral-900)", border: "1px solid var(--neutral-800)", borderRadius: 8, overflow: "hidden" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid var(--neutral-800)", background: "var(--neutral-950)" }}>
                  <th style={{ textAlign: "left", padding: "12px 16px", fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--neutral-500)" }}>Date</th>
                  <th style={{ textAlign: "left", padding: "12px 16px", fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--neutral-500)" }}>Method</th>
                  <th style={{ textAlign: "left", padding: "12px 16px", fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--neutral-500)" }}>Transaction ID</th>
                  <th style={{ textAlign: "right", padding: "12px 16px", fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--neutral-500)" }}>Amount</th>
                  <th style={{ textAlign: "right", padding: "12px 16px", fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--neutral-500)" }}>Status</th>
                </tr>
              </thead>
              <tbody>
                <AnimatePresence initial={true}>
                  {inv.payments.map((p, i) => (
                    <motion.tr
                      key={p.id}
                      initial={{ opacity: 0, y: 8 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3, delay: i * 0.07, ease: [0.16, 1, 0.3, 1] }}
                      style={{ borderBottom: "1px solid var(--neutral-800)" }}
                    >
                      <td style={{ padding: "12px 16px", fontSize: 13, color: "var(--neutral-300)" }}>
                        {formatDate(p.createdAt)}
                      </td>
                      <td style={{ padding: "12px 16px", fontSize: 13, color: "var(--neutral-300)" }}>
                        Razorpay
                      </td>
                      <td style={{ padding: "12px 16px", fontSize: 13, fontFamily: "var(--font-mono)", color: "var(--neutral-400)" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                          {p.razorpayPaymentId ? (
                            <>
                              {p.razorpayPaymentId.substring(0, 10)}...
                              <CopyButton text={p.razorpayPaymentId} />
                            </>
                          ) : "-"}
                        </div>
                      </td>
                      <td style={{ textAlign: "right", padding: "12px 16px", fontSize: 13, fontFamily: "var(--font-mono)", color: "var(--neutral-200)" }}>
                        {formatINR(p.amountCents)}
                      </td>
                      <td style={{ textAlign: "right", padding: "12px 16px" }}>
                        <span style={{
                          fontSize: 11, fontWeight: 500, padding: "2px 6px", borderRadius: 4,
                          color: p.status === "SUCCESS" ? "var(--green)" : p.status === "FAILED" ? "var(--red)" : "var(--amber)",
                          background: p.status === "SUCCESS" ? "var(--green-soft)" : p.status === "FAILED" ? "var(--red-soft)" : "var(--amber-soft)",
                        }}>
                          {p.status}
                        </span>
                      </td>
                    </motion.tr>
                  ))}
                </AnimatePresence>
              </tbody>
            </table>
          </div>
        </motion.div>
      )}
    </motion.div>
  );
}
