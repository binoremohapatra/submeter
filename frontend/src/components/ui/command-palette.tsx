"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import { apiFetch } from "@/lib/api";
import { motion, AnimatePresence } from "framer-motion";
import { SPRING_LAYOUT, EASE_OUT_EXPO } from "@/lib/motion";
import {
  LayoutDashboard, Users, Package, Repeat2, FileText,
  Shield, Settings, BarChart2, Search, ArrowRight, User
} from "lucide-react";

// ── Types ──────────────────────────────────────────────────
interface ResultItem {
  id: string;
  label: string;
  sublabel?: string;
  type: "nav" | "customer" | "plan" | "subscription";
  href: string;
  icon: React.ReactNode;
}

// ── Static nav commands ────────────────────────────────────
const NAV_ITEMS: ResultItem[] = [
  { label: "Overview",      href: "/dashboard",               type: "nav" as const, icon: <LayoutDashboard style={{ width: 13, height: 13 }} /> },
  { label: "Customers",     href: "/dashboard/customers",     type: "nav" as const, icon: <Users style={{ width: 13, height: 13 }} /> },
  { label: "Plans",         href: "/dashboard/plans",         type: "nav" as const, icon: <Package style={{ width: 13, height: 13 }} /> },
  { label: "Subscriptions", href: "/dashboard/subscriptions", type: "nav" as const, icon: <Repeat2 style={{ width: 13, height: 13 }} /> },
  { label: "Invoices",      href: "/dashboard/invoices",      type: "nav" as const, icon: <FileText style={{ width: 13, height: 13 }} /> },
  { label: "Analytics",     href: "/dashboard/analytics",     type: "nav" as const, icon: <BarChart2 style={{ width: 13, height: 13 }} /> },
  { label: "Audit Log",     href: "/dashboard/audit-log",     type: "nav" as const, icon: <Shield style={{ width: 13, height: 13 }} /> },
  { label: "Settings",      href: "/dashboard/settings",      type: "nav" as const, icon: <Settings style={{ width: 13, height: 13 }} /> },
].map((item, i) => ({ ...item, id: `nav-${i}` }));

// ── Module-level open/close toggle ─────────────────────────
let setOpenGlobal: ((v: boolean) => void) | null = null;
export function openCommandPalette() { setOpenGlobal?.(true); }
export function registerCommands(_: unknown) {}  // kept for API compat

export function CommandPalette() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<ResultItem[]>(NAV_ITEMS);
  const [activeIdx, setActiveIdx] = useState(0);
  const [searching, setSearching] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);
  const router = useRouter();
  const { data: session } = useSession();
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  setOpenGlobal = setOpen;

  // ── Global keyboard shortcut ───────────────────────────
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        setOpen(prev => !prev);
      }
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, []);

  // ── Focus on open ─────────────────────────────────────
  useEffect(() => {
    if (open) {
      setQuery("");
      setResults(NAV_ITEMS);
      setActiveIdx(0);
      setTimeout(() => inputRef.current?.focus(), 10);
    }
  }, [open]);

  // ── Data search with debounce ──────────────────────────
  useEffect(() => {
    if (searchTimeout.current) clearTimeout(searchTimeout.current);

    const trimmed = query.trim();
    if (!trimmed) {
      setResults(NAV_ITEMS);
      setSearching(false);
      setActiveIdx(0);
      return;
    }

    // Filter nav items immediately
    const navMatches = NAV_ITEMS.filter(n =>
      n.label.toLowerCase().includes(trimmed.toLowerCase())
    );
    setResults(navMatches);
    setActiveIdx(0);

    if (!session) return;

    // Debounced API search
    setSearching(true);
    searchTimeout.current = setTimeout(async () => {
      try {
        const data = await apiFetch<{ customers: any[], invoices: any[], subscriptions: any[], plans: any[] }>(`/search?q=${encodeURIComponent(trimmed)}`, session).catch(() => ({ customers: [], invoices: [], subscriptions: [], plans: [] }));

        const customerItems: ResultItem[] = (data.customers ?? []).map((c: any) => ({
          id: `cust-${c.id}`,
          label: c.name,
          sublabel: c.email,
          type: "customer",
          href: `/dashboard/customers/${c.id}`,
          icon: <User style={{ width: 13, height: 13 }} />,
        }));

        const invoiceItems: ResultItem[] = (data.invoices ?? []).map((i: any) => ({
          id: `inv-${i.id}`,
          label: i.invoiceNumber,
          sublabel: `$${(i.amountDueCents / 100).toFixed(2)}`,
          type: "nav", // Treated as nav for icon
          href: `/dashboard/invoices/${i.id}`,
          icon: <FileText style={{ width: 13, height: 13 }} />,
        }));

        const subItems: ResultItem[] = (data.subscriptions ?? []).map((s: any) => ({
          id: `sub-${s.id}`,
          label: `Sub ${s.id.substring(0,8)}`,
          sublabel: s.status,
          type: "subscription",
          href: `/dashboard/subscriptions/${s.id}`,
          icon: <Repeat2 style={{ width: 13, height: 13 }} />,
        }));

        const planItems: ResultItem[] = (data.plans ?? []).map((p: any) => ({
          id: `plan-${p.id}`,
          label: p.name,
          sublabel: p.pricingModel,
          type: "plan",
          href: `/dashboard/plans/${p.id}`,
          icon: <Package style={{ width: 13, height: 13 }} />,
        }));

        setResults([...navMatches, ...customerItems, ...invoiceItems, ...subItems, ...planItems]);
        setActiveIdx(0);
      } catch {}
      setSearching(false);
    }, 300);

    return () => { if (searchTimeout.current) clearTimeout(searchTimeout.current); };
  }, [query, session]);

  // ── Keyboard navigation ────────────────────────────────
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIdx(i => Math.min(i + 1, results.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIdx(i => Math.max(i - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (results[activeIdx]) {
        router.push(results[activeIdx].href);
        setOpen(false);
      }
    }
  };

  // ── Scroll active into view ────────────────────────────
  useEffect(() => {
    const el = listRef.current?.children[activeIdx] as HTMLElement;
    el?.scrollIntoView({ block: "nearest" });
  }, [activeIdx]);

  const TYPE_LABEL: Record<string, string> = {
    nav: "Navigation",
    customer: "Customer",
    plan: "Plan",
    subscription: "Subscription",
  };

  if (!open) return null;

  return (
    <AnimatePresence>
      <>
          {/* Backdrop — fades in first */}
          <motion.div
            key="backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.1 }}
            onClick={() => setOpen(false)}
            style={{
              position: "fixed", inset: 0,
              background: "rgba(0,0,0,0.65)",
              backdropFilter: "blur(4px)",
              zIndex: 10000,
            }}
            aria-hidden="true"
          />

          {/* Panel — delays 50ms after backdrop, scales in from 96% */}
          <motion.div
            key="panel"
            initial={{ opacity: 0, scale: 0.96, y: -6 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: -6 }}
            transition={{ duration: 0.15, delay: 0.05, ease: EASE_OUT_EXPO }}
            role="dialog"
            aria-label="Command palette"
            aria-modal="true"
            style={{
              position: "fixed",
              top: "24px",
              left: "50%",
              transform: "translateX(-50%)",
              width: "min(640px, calc(100vw - 32px))",
              background: "var(--neutral-900)",
              border: "1px solid var(--neutral-700)",
              borderRadius: 12,
              boxShadow: "0 32px 80px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.04)",
              zIndex: 10001,
              overflow: "hidden",
            }}
            className="cmd-palette-panel"
          >
          {/* Search input */}
          <div style={{
            display: "flex", alignItems: "center", gap: 10,
            padding: "12px 16px",
            borderBottom: "1px solid var(--neutral-800)",
          }}>
            <Search style={{ width: 15, height: 15, color: "var(--neutral-500)", flexShrink: 0 }} />
            <input
              ref={inputRef}
              type="text"
              placeholder="Search pages, customers, plans…"
              value={query}
              onChange={e => setQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              style={{
                flex: 1,
                background: "transparent",
                border: "none",
                outline: "none",
                fontSize: 14,
                color: "var(--neutral-100)",
                fontFamily: "var(--font-sans)",
              }}
              aria-label="Search"
              aria-autocomplete="list"
              aria-expanded={open}
            />
            {searching && (
              <div style={{ width: 14, height: 14, borderRadius: "50%", border: "1.5px solid var(--neutral-700)", borderTopColor: "var(--neutral-400)", animation: "spin 0.8s linear infinite" }} />
            )}
            <kbd style={{ fontSize: 11, color: "var(--neutral-600)", background: "var(--neutral-800)", border: "1px solid var(--neutral-700)", borderRadius: 4, padding: "2px 5px", fontFamily: "var(--font-sans)" }}>
              Esc
            </kbd>
          </div>

          {/* Results */}
          <ul
            ref={listRef}
            role="listbox"
            style={{
              listStyle: "none",
              margin: 0,
              padding: "6px",
              maxHeight: 380,
              overflowY: "auto",
            }}
          >
            {results.length === 0 && !searching && (
              <li style={{ padding: "20px 16px", textAlign: "center", fontSize: 13, color: "var(--neutral-500)" }}>
                No results for &ldquo;{query}&rdquo;
              </li>
            )}

            <AnimatePresence initial={false}>
              {results.map((item, idx) => {
                const isActive = idx === activeIdx;
                // Stagger only first 10 items; rest appear instantly
                const staggerDelay = idx < 10 ? idx * 0.03 : 0;
                return (
                  <motion.li
                    key={item.id}
                    initial={{ opacity: 0, y: 4 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.97 }}
                    transition={{ duration: 0.15, delay: staggerDelay }}
                    role="option"
                    aria-selected={isActive}
                    onClick={() => { router.push(item.href); setOpen(false); }}
                    onMouseEnter={() => setActiveIdx(idx)}
                    tabIndex={0}
                    onKeyDown={e => { if (e.key === "Enter") { router.push(item.href); setOpen(false); } }}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 10,
                      padding: "9px 12px",
                      borderRadius: 7,
                      cursor: "pointer",
                      position: "relative",
                      // background handled by layoutId highlight below
                    }}
                  >
                    {/* Sliding highlight — layoutId so it glides between items */}
                    {isActive && (
                      <motion.div
                        layoutId="cmd-palette-highlight"
                        transition={SPRING_LAYOUT}
                        style={{
                          position: "absolute",
                          inset: 0,
                          borderRadius: 7,
                          background: "var(--neutral-800)",
                          zIndex: 0,
                        }}
                        aria-hidden="true"
                      />
                    )}
                    <span style={{ color: isActive ? "var(--neutral-300)" : "var(--neutral-500)", lineHeight: 0, position: "relative", zIndex: 1 }}>
                      {item.icon}
                    </span>
                    <span style={{ flex: 1, fontSize: 13, color: isActive ? "var(--neutral-50)" : "var(--neutral-200)", fontWeight: isActive ? 500 : 400, position: "relative", zIndex: 1 }}>
                      {item.label}
                    </span>
                    {item.sublabel && (
                      <span style={{ fontSize: 11, color: "var(--neutral-500)", fontFamily: item.type === "customer" ? "var(--font-mono)" : undefined, position: "relative", zIndex: 1 }}>
                        {item.sublabel}
                      </span>
                    )}
                    {item.type !== "nav" && (
                      <span style={{
                        fontSize: 10, fontWeight: 500, letterSpacing: "0.06em", textTransform: "uppercase",
                        color: "var(--neutral-600)", background: "var(--neutral-800)",
                        border: "1px solid var(--neutral-700)",
                        borderRadius: 3, padding: "1px 5px",
                        position: "relative", zIndex: 1,
                      }}>
                        {TYPE_LABEL[item.type]}
                      </span>
                    )}
                    {isActive && <ArrowRight style={{ width: 12, height: 12, color: "var(--neutral-500)", flexShrink: 0, position: "relative", zIndex: 1 }} />}
                  </motion.li>
                );
              })}
            </AnimatePresence>
          </ul>

          {/* Footer */}
          <div style={{
            borderTop: "1px solid var(--neutral-800)",
            padding: "8px 16px",
            display: "flex",
            gap: 16,
            fontSize: 11,
            color: "var(--neutral-600)",
          }}>
            <span>↵ open</span>
            <span>↑↓ navigate</span>
            <span>Esc close</span>
          </div>
        </motion.div>
      </>
    </AnimatePresence>
  );
}
