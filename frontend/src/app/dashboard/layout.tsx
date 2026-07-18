"use client";

import { useSession, signOut } from "next-auth/react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import { useEffect, useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ToastProvider } from "@/components/ui/toast";
import { openCommandPalette } from "@/components/ui/command-palette";
import { SPRING_LAYOUT, EASE_OUT_EXPO } from "@/lib/motion";
import {
  LayoutDashboard, Users, Package, Repeat2, FileText,
  Shield, Settings, LogOut, Search, X, ChevronDown, BarChart2
} from "lucide-react";
import { NotificationBell } from "@/components/ui/notification-bell";


const NAV = [
  { label: "Overview",      href: "/dashboard",               icon: LayoutDashboard },
  { label: "Customers",     href: "/dashboard/customers",     icon: Users },
  { label: "Plans",         href: "/dashboard/plans",         icon: Package },
  { label: "Subscriptions", href: "/dashboard/subscriptions", icon: Repeat2 },
  { label: "Invoices",      href: "/dashboard/invoices",      icon: FileText },
  { label: "Analytics",     href: "/dashboard/analytics",     icon: BarChart2 },
  { label: "Audit Log",     href: "/dashboard/audit-log",     icon: Shield },
  { label: "Settings",      href: "/dashboard/settings",      icon: Settings },
];

function initials(name?: string | null, email?: string | null) {
  if (name) {
    const p = name.trim().split(/\s+/);
    return (p[0][0] + (p[1]?.[0] ?? "")).toUpperCase();
  }
  return (email?.[0] ?? "?").toUpperCase();
}

// ── Logo Mark ──────────────────────────────────────────────
function LogoMark() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
      <rect x="1" y="1" width="7" height="7" rx="2" fill="currentColor" opacity="0.9"/>
      <rect x="10" y="1" width="7" height="7" rx="2" fill="currentColor" opacity="0.5"/>
      <rect x="1" y="10" width="7" height="7" rx="2" fill="currentColor" opacity="0.5"/>
      <rect x="10" y="10" width="7" height="7" rx="2" fill="currentColor" opacity="0.2"/>
    </svg>
  );
}

// ── Sidebar Content ────────────────────────────────────────
function SidebarContent({ session, pathname }: { session: any; pathname: string }) {
  const ini = initials(session?.user?.name, session?.user?.email);
  const [orgName, setOrgName] = useState("");

  useEffect(() => {
    if (session) {
      import("@/lib/api").then(({ apiFetch }) => {
        apiFetch<any>("/settings", session)
          .then(res => setOrgName(res.name || "My Organization"))
          .catch(() => setOrgName("My Organization"));
      });
    }
  }, [session]);

  const orgIni = orgName ? orgName.charAt(0).toUpperCase() : "O";

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        background: "var(--neutral-950)",
        borderRight: "1px solid var(--neutral-800)",
      }}
    >
      {/* Wordmark */}
      <div style={{ padding: "20px 16px 16px", borderBottom: "1px solid var(--neutral-800)" }}>
        <Link href="/" style={{ display: "flex", alignItems: "center", gap: 9, marginBottom: 16, textDecoration: "none" }}>
          <div style={{ color: "var(--neutral-100)" }}>
            <LogoMark />
          </div>
          <span style={{ fontSize: 14, fontWeight: 600, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
            SubMeter
          </span>
        </Link>

        {/* Org selector */}
        <button
          style={{
            width: "100%",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "7px 10px",
            background: "var(--neutral-900)",
            border: "1px solid var(--neutral-800)",
            borderRadius: 6,
            cursor: "pointer",
            fontSize: 12,
            color: "var(--neutral-300)",
            transition: "all 150ms ease",
          }}
          onMouseEnter={e => {
            (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--neutral-700)";
            (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-100)";
          }}
          onMouseLeave={e => {
            (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--neutral-800)";
            (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-300)";
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <div style={{
              width: 18, height: 18, borderRadius: 4,
              background: "var(--neutral-700)",
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 10, fontWeight: 700, color: "var(--neutral-300)",
              flexShrink: 0,
            }}>
              {orgIni}
            </div>
            <span style={{ fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {orgName || "Loading..."}
            </span>
          </div>
          <ChevronDown style={{ width: 12, height: 12, flexShrink: 0, opacity: 0.5 }} />
        </button>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, overflowY: "auto", padding: "16px 12px" }}>
        {NAV.map(item => {
          const isActive = item.href === "/dashboard"
            ? pathname === "/dashboard"
            : pathname.startsWith(item.href);
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`nav-item${isActive ? " active" : ""}`}
              style={{ marginBottom: 1, position: "relative" }}
            >
              {/* Sliding active indicator — layoutId makes it glide between items */}
              {isActive && (
                <motion.div
                  layoutId="sidebar-active-pill"
                  transition={SPRING_LAYOUT}
                  style={{
                    position: "absolute",
                    left: -1,
                    top: "20%",
                    height: "60%",
                    width: 2,
                    background: "var(--neutral-300)",
                    borderRadius: "0 2px 2px 0",
                  }}
                  aria-hidden="true"
                />
              )}
              <Icon style={{ width: 14, height: 14, flexShrink: 0, opacity: isActive ? 0.9 : 0.5 }} />
              {item.label}
            </Link>
          );
        })}
      </nav>

      {/* User */}
      <div style={{ padding: "12px 16px", borderTop: "1px solid var(--neutral-800)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 9, marginBottom: 10 }}>
          <div style={{
            width: 28, height: 28, borderRadius: 6,
            background: "var(--neutral-800)",
            border: "1px solid var(--neutral-700)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 11, fontWeight: 600, color: "var(--neutral-300)",
            flexShrink: 0, letterSpacing: "0.02em",
          }}>
            {ini}
          </div>
          <div style={{ overflow: "hidden", flex: 1 }}>
            <div style={{ fontSize: 12, fontWeight: 500, color: "var(--neutral-200)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {session?.user?.name ?? session?.user?.email}
            </div>
            <div style={{ fontSize: 10, color: "var(--neutral-500)", letterSpacing: "0.06em", textTransform: "uppercase", marginTop: 1 }}>
              {session?.user?.role ?? "Owner"}
            </div>
          </div>
        </div>

        <button
          onClick={() => signOut({ callbackUrl: "/" })}
          style={{
            width: "100%",
            display: "flex",
            alignItems: "center",
            gap: 7,
            padding: "6px 10px",
            background: "transparent",
            border: "none",
            borderRadius: 5,
            cursor: "pointer",
            fontSize: 12,
            color: "var(--neutral-500)",
            transition: "all 150ms ease",
          }}
          onMouseEnter={e => {
            (e.currentTarget as HTMLButtonElement).style.background = "var(--red-soft)";
            (e.currentTarget as HTMLButtonElement).style.color = "var(--red)";
          }}
          onMouseLeave={e => {
            (e.currentTarget as HTMLButtonElement).style.background = "transparent";
            (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-500)";
          }}
        >
          <LogOut style={{ width: 12, height: 12 }} />
          Sign out
        </button>
      </div>
    </div>
  );
}

// ── Layout ─────────────────────────────────────────────────
export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { data: session, status } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    if (status === "unauthenticated") router.push("/login");
  }, [status, router]);

  useEffect(() => { setMobileOpen(false); }, [pathname]);

  if (status === "loading") {
    return (
      <div style={{
        minHeight: "100dvh", background: "var(--neutral-950)",
        display: "flex", alignItems: "center", justifyContent: "center",
        flexDirection: "column", gap: 16,
      }}>
        <div style={{
          width: 20, height: 20, borderRadius: "50%",
          border: "1.5px solid var(--neutral-700)",
          borderTopColor: "var(--neutral-300)",
          animation: "spin 0.8s linear infinite",
        }} />
        <style>{`@keyframes spin { to { transform: rotate(360deg) } }`}</style>
        <span style={{ fontSize: 13, color: "var(--neutral-500)" }}>Loading...</span>
      </div>
    );
  }

  if (!session) return null;

  return (
    <div style={{ display: "flex", minHeight: "100vh", background: "var(--neutral-950)" }}>
      {/* Desktop Sidebar */}
      <aside style={{
        width: "var(--sidebar-w)", flexShrink: 0,
        position: "sticky", top: 0, height: "100vh",
      }}
        className="hidden md:flex md:flex-col"
      >
        <SidebarContent session={session} pathname={pathname} />
      </aside>

      {/* Mobile top bar */}
      <div
        className="flex md:hidden"
        style={{
          position: "fixed", top: 0, left: 0, right: 0, zIndex: 40,
          height: 52, background: "var(--neutral-950)",
          borderBottom: "1px solid var(--neutral-800)",
          alignItems: "center",
          justifyContent: "space-between", padding: "0 16px",
        }}
      >
        <Link href="/" style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--neutral-100)", textDecoration: "none" }}>
          <LogoMark />
          <span style={{ fontSize: 14, fontWeight: 600, letterSpacing: "-0.02em" }}>SubMeter</span>
        </Link>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          {/* Mobile search button — tap-accessible entry point for command palette */}
          <button
            onClick={() => openCommandPalette()}
            aria-label="Open search"
            style={{
              background: "none", border: "none", cursor: "pointer",
              color: "var(--neutral-400)", padding: 6, borderRadius: 5,
              minWidth: 44, minHeight: 44,
              display: "flex", alignItems: "center", justifyContent: "center",
            }}
          >
            <Search style={{ width: 16, height: 16 }} />
          </button>
          <NotificationBell />
          <button
            onClick={() => setMobileOpen(v => !v)}
            aria-label={mobileOpen ? "Close menu" : "Open menu"}
            style={{
              background: "none", border: "none", cursor: "pointer",
              color: "var(--neutral-400)", padding: 6, borderRadius: 5,
              minWidth: 44, minHeight: 44,
              display: "flex", alignItems: "center", justifyContent: "center",
            }}
          >
            {mobileOpen
              ? <X style={{ width: 18, height: 18 }} />
              : <div style={{ display: "flex", flexDirection: "column", gap: 4, alignItems: "center" }}>
                  {[1,2,3].map(i => (
                    <div key={i} style={{ width: 16, height: 1.5, background: "currentColor", borderRadius: 1 }} />
                  ))}
                </div>
            }
          </button>
        </div>
      </div>

      {/* Mobile drawer */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
              style={{
                position: "fixed", inset: 0, zIndex: 45,
                background: "rgba(0,0,0,0.6)", backdropFilter: "blur(4px)",
              }}
              onClick={() => setMobileOpen(false)}
            />
            <motion.aside
              initial={{ x: -260 }}
              animate={{ x: 0 }}
              exit={{ x: -260 }}
              transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
              style={{
                position: "fixed", top: 0, left: 0, bottom: 0,
                zIndex: 50, width: 240, display: "flex", flexDirection: "column",
              }}
            >
              <SidebarContent session={session} pathname={pathname} />
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* Main */}
      <main style={{
        flex: 1, minWidth: 0, display: "flex", flexDirection: "column",
      }}>
        <div className="md:hidden" style={{ height: 52, flexShrink: 0 }} />

        {/* Global Desktop Header */}
        <header
          className="hidden md:flex"
          style={{
            position: "sticky", top: 0, zIndex: 30,
            height: 60, padding: "0 32px",
            alignItems: "center", justifyContent: "flex-end",
            gap: 16,
            background: "rgba(10, 10, 10, 0.7)",
            backdropFilter: "blur(12px)",
            borderBottom: "1px solid rgba(255, 255, 255, 0.05)",
          }}
        >
          {/* Global Search Bar */}
          <button
            onClick={() => openCommandPalette()}
            aria-label="Open search (Ctrl+K)"
            style={{
              width: 280,
              display: "flex",
              alignItems: "center",
              gap: 10,
              padding: "0 12px",
              height: 34,
              background: "rgba(255,255,255,0.03)",
              border: "1px solid rgba(255,255,255,0.08)",
              borderRadius: 6,
              cursor: "text",
              fontSize: 13,
              color: "var(--neutral-400)",
              transition: "all 200ms ease",
              boxShadow: "0 2px 8px rgba(0,0,0,0.1)",
            }}
            onMouseEnter={e => {
              (e.currentTarget as HTMLButtonElement).style.borderColor = "rgba(255,255,255,0.15)";
              (e.currentTarget as HTMLButtonElement).style.background = "rgba(255,255,255,0.05)";
              (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-200)";
            }}
            onMouseLeave={e => {
              (e.currentTarget as HTMLButtonElement).style.borderColor = "rgba(255,255,255,0.08)";
              (e.currentTarget as HTMLButtonElement).style.background = "rgba(255,255,255,0.03)";
              (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-400)";
            }}
          >
            <Search style={{ width: 14, height: 14, opacity: 0.7 }} />
            <span style={{ flex: 1, textAlign: "left" }}>Search or jump to...</span>
            <kbd style={{
              display: "inline-flex", alignItems: "center", gap: 2,
              background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)",
              borderRadius: 4, padding: "2px 6px",
              fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--neutral-400)",
            }}>
              ⌘K
            </kbd>
          </button>
          
          <div style={{ width: 1, height: 24, background: "rgba(255, 255, 255, 0.08)" }} />
          
          {/* Global Notification Bell */}
          <div style={{ opacity: 0.85, transition: "opacity 150ms ease" }} 
               onMouseEnter={e => (e.currentTarget.style.opacity = "1")} 
               onMouseLeave={e => (e.currentTarget.style.opacity = "0.85")}>
            <NotificationBell />
          </div>
        </header>

        <motion.div
          key={pathname}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2, ease: EASE_OUT_EXPO }}
          style={{
            flex: 1,
            padding: "32px 32px",
            maxWidth: 1200,
            width: "100%",
            margin: "0 auto",
          }}
          className="px-4 sm:px-6 lg:!px-8"
        >
          {children}
        </motion.div>
      </main>

      <ToastProvider />
    </div>
  );
}
