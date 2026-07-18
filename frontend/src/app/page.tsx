"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import Link from "next/link";
import {
  motion, useScroll, useTransform, useInView,
  useMotionValue, useSpring, AnimatePresence,
} from "framer-motion";
import { EASE_OUT_EXPO, SPRING_SNAPPY } from "@/lib/motion";

// ── Logo ───────────────────────────────────────────────────
function Logo() {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
      <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <rect x="1" y="1" width="7" height="7" rx="2" fill="white" opacity="0.9"/>
        <rect x="10" y="1" width="7" height="7" rx="2" fill="white" opacity="0.4"/>
        <rect x="1" y="10" width="7" height="7" rx="2" fill="white" opacity="0.4"/>
        <rect x="10" y="10" width="7" height="7" rx="2" fill="white" opacity="0.15"/>
      </svg>
      <span style={{ fontSize: 15, fontWeight: 600, letterSpacing: "-0.03em", color: "white" }}>
        SubMeter
      </span>
    </div>
  );
}

// ── Cursor Glow ──────────────────────────────────────────────
function useCursorGlow(containerRef: React.RefObject<HTMLElement | null>) {
  // Disabled on touch devices (no cursor)
  const isTouch = typeof window !== "undefined" && window.matchMedia("(hover: none)").matches;
  const rawX = useMotionValue(0);
  const rawY = useMotionValue(0);
  // ~10% lag — spring creates the trailing effect
  const x = useSpring(rawX, { stiffness: 80, damping: 20, mass: 1 });
  const y = useSpring(rawY, { stiffness: 80, damping: 20, mass: 1 });

  useEffect(() => {
    if (isTouch) return;
    const el = containerRef.current;
    if (!el) return;
    const onMove = (e: MouseEvent) => {
      const rect = el.getBoundingClientRect();
      rawX.set(e.clientX - rect.left);
      rawY.set(e.clientY - rect.top);
    };
    el.addEventListener("mousemove", onMove, { passive: true });
    return () => el.removeEventListener("mousemove", onMove);
  }, [containerRef, isTouch, rawX, rawY]);

  return isTouch ? null : { x, y };
}

// ── Magnetic Button ──────────────────────────────────────────
function MagneticButton({ children, className, style }: {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const springX = useSpring(x, SPRING_SNAPPY);
  const springY = useSpring(y, SPRING_SNAPPY);

  const handleMove = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    const el = ref.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const dx = e.clientX - cx;
    const dy = e.clientY - cy;
    // Pull strength: max ±6px, scaled by distance within a 60px radius
    const dist = Math.sqrt(dx * dx + dy * dy);
    const radius = 60;
    if (dist < radius) {
      const pull = (1 - dist / radius) * 6;
      x.set((dx / dist) * pull);
      y.set((dy / dist) * pull);
    }
  }, [x, y]);

  const handleLeave = useCallback(() => {
    x.set(0);
    y.set(0);
  }, [x, y]);

  return (
    <motion.div
      ref={ref}
      style={{ ...style, x: springX, y: springY, display: "inline-block" }}
      className={className}
      onMouseMove={handleMove}
      onMouseLeave={handleLeave}
    >
      {children}
    </motion.div>
  );
}

// ── Word Reveal ───────────────────────────────────────────────
function WordReveal({ text, style }: { text: string; style?: React.CSSProperties }) {
  const words = text.split(" ");
  return (
    <span style={{ display: "flex", flexWrap: "wrap", gap: "0.28em", ...style }}>
      {words.map((word, i) => (
        <motion.span
          key={i}
          style={{ overflow: "hidden", display: "inline-block" }}
          initial={{ clipPath: "inset(0 100% 0 0)" }}
          animate={{ clipPath: "inset(0 0% 0 0)" }}
          transition={{
            duration: 0.55,
            delay: 0.06 + i * 0.07,
            ease: EASE_OUT_EXPO,
          }}
        >
          {word}
        </motion.span>
      ))}
    </span>
  );
}

// ── Typewriter ────────────────────────────────────────────────
function Typewriter({ words }: { words: string[] }) {
  const [index, setIndex] = useState(0);
  const [text, setText] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    const currentWord = words[index];

    if (isDeleting) {
      if (text === "") {
        setIsDeleting(false);
        setIndex((prev) => (prev + 1) % words.length);
        timer = setTimeout(() => {}, 400);
      } else {
        timer = setTimeout(() => {
          setText(currentWord.substring(0, text.length - 1));
        }, 40);
      }
    } else {
      if (text === currentWord) {
        timer = setTimeout(() => {
          setIsDeleting(true);
        }, 2000);
      } else {
        timer = setTimeout(() => {
          setText(currentWord.substring(0, text.length + 1));
        }, 80);
      }
    }

    return () => clearTimeout(timer);
  }, [text, isDeleting, index, words]);

  return (
    <span style={{ position: "relative" }}>
      {text}
      <motion.span
        animate={{ opacity: [1, 0, 1] }}
        transition={{ repeat: Infinity, duration: 0.8, ease: "linear" }}
        style={{
          position: "absolute",
          right: -8,
          borderRight: "3px solid var(--neutral-400)",
          height: "0.9em",
          top: "0.05em",
        }}
      />
    </span>
  );
}

// ── Fade-Up wrapper ─────────────────────────────────────────
function FadeUp({ children, delay = 0, className = "" }: {
  children: React.ReactNode;
  delay?: number;
  className?: string;
}) {
  const ref = useRef(null);
  const inView = useInView(ref, { once: true, margin: "-80px" });

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 20 }}
      animate={inView ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.6, ease: EASE_OUT_EXPO, delay }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

// ── Nav ─────────────────────────────────────────────────────
function Nav() {
  return (
    <motion.nav
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
      style={{
        position: "fixed", top: 0, left: 0, right: 0, zIndex: 50,
        display: "flex", alignItems: "center", justifyContent: "space-between",
        padding: "0 40px", height: 60,
        borderBottom: "1px solid var(--neutral-800)",
        background: "rgba(8, 8, 8, 0.85)",
        backdropFilter: "blur(20px)",
      }}
    >
      <Logo />

      <div style={{ display: "flex", alignItems: "center", gap: 32 }}>
        {["Features", "Pricing", "Docs"].map(item => (
          <a
            key={item}
            href="#"
            style={{
              fontSize: 13, color: "var(--neutral-500)",
              textDecoration: "none", fontWeight: 450,
              transition: "color 150ms ease",
            }}
            onMouseEnter={e => (e.currentTarget.style.color = "var(--neutral-200)")}
            onMouseLeave={e => (e.currentTarget.style.color = "var(--neutral-500)")}
          >
            {item}
          </a>
        ))}
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <Link href="/login" style={{ textDecoration: "none" }}>
          <button style={{
            padding: "7px 14px",
            background: "transparent",
            border: "1px solid var(--neutral-700)",
            borderRadius: 6,
            fontSize: 13, fontWeight: 500,
            color: "var(--neutral-300)",
            cursor: "pointer",
            transition: "all 150ms ease",
          }}
            onMouseEnter={e => {
              (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--neutral-500)";
              (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-100)";
            }}
            onMouseLeave={e => {
              (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--neutral-700)";
              (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-300)";
            }}
          >
            Sign in
          </button>
        </Link>
        <Link href="/login" style={{ textDecoration: "none" }}>
          <button style={{
            padding: "7px 16px",
            background: "var(--neutral-50)",
            border: "none",
            borderRadius: 6,
            fontSize: 13, fontWeight: 500,
            color: "var(--neutral-950)",
            cursor: "pointer",
            transition: "background 150ms ease",
          }}
            onMouseEnter={e => (e.currentTarget as HTMLButtonElement).style.background = "var(--neutral-200)"}
            onMouseLeave={e => (e.currentTarget as HTMLButtonElement).style.background = "var(--neutral-50)"}
          >
            Start free
          </button>
        </Link>
      </div>
    </motion.nav>
  );
}

// ── Hero — Dashboard Preview ────────────────────────────────
function DashboardPreview() {
  return (
    <motion.div 
      initial="hidden"
      whileInView="visible"
      viewport={{ once: true, margin: "-40px" }}
      variants={{
        visible: { transition: { staggerChildren: 0.05 } }
      }}
      style={{
        width: "100%",
        background: "var(--neutral-900)",
        border: "1px solid var(--neutral-800)",
        borderRadius: 12,
        overflow: "hidden",
        fontFamily: "var(--font-mono)",
      }}
    >
      {/* Title bar */}
      <div style={{
        padding: "12px 16px",
        borderBottom: "1px solid var(--neutral-800)",
        display: "flex",
        alignItems: "center",
        gap: 6,
      }}>
        {["#FF5F56", "#FEBC2E", "#28C840"].map(c => (
          <div key={c} style={{ width: 10, height: 10, borderRadius: "50%", background: c }} />
        ))}
        <div style={{
          flex: 1, display: "flex", justifyContent: "center",
          fontSize: 11, color: "var(--neutral-500)",
        }}>
          app.submeter.io/dashboard
        </div>
      </div>

      <div style={{ display: "flex" }}>
        {/* Sidebar mini */}
        <div style={{
          width: 160, flexShrink: 0,
          borderRight: "1px solid var(--neutral-800)",
          padding: "16px 10px",
        }}>
          {[
            { label: "Overview", active: true },
            { label: "Customers", active: false },
            { label: "Plans", active: false },
            { label: "Subscriptions", active: false },
            { label: "Invoices", active: false },
            { label: "Analytics", active: false },
          ].map((item, i) => (
            <motion.div 
              key={item.label}
              variants={{
                hidden: { opacity: 0, x: -10 },
                visible: { opacity: 1, x: 0, transition: { type: "spring", stiffness: 300, damping: 24 } }
              }}
              style={{
                padding: "5px 8px", marginBottom: 1,
                borderRadius: 4,
                fontSize: 11,
                fontFamily: "var(--font-sans)",
                color: item.active ? "var(--neutral-100)" : "var(--neutral-500)",
                background: item.active ? "var(--neutral-800)" : "transparent",
                fontWeight: item.active ? 500 : 400,
              }}
            >
              {item.label}
            </motion.div>
          ))}
        </div>

        {/* Content */}
        <div style={{ flex: 1, padding: 20, minHeight: 340 }}>
          {/* KPI row */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 10, marginBottom: 16 }}>
            {[
              { label: "MRR", value: "₹4.2L", change: "+12%" },
              { label: "ARPU", value: "₹8,400", change: "+4%" },
              { label: "Churn", value: "1.8%", change: "-0.3%" },
              { label: "Active Subs", value: "48", change: "+5" },
            ].map(kpi => (
              <motion.div 
                key={kpi.label} 
                variants={{
                  hidden: { opacity: 0, y: 10, scale: 0.98 },
                  visible: { opacity: 1, y: 0, scale: 1, transition: { type: "spring", stiffness: 400, damping: 30 } }
                }}
                style={{
                  background: "var(--neutral-800)",
                  border: "1px solid var(--neutral-700)",
                  borderRadius: 6, padding: "10px 12px",
                }}
              >
                <div style={{ fontSize: 9, color: "var(--neutral-500)", letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 4, fontFamily: "var(--font-sans)" }}>
                  {kpi.label}
                </div>
                <div style={{ fontSize: 15, fontWeight: 600, color: "var(--neutral-50)", letterSpacing: "-0.02em" }}>
                  {kpi.value}
                </div>
                <div style={{ fontSize: 10, color: "var(--green)", marginTop: 2 }}>
                  {kpi.change}
                </div>
              </motion.div>
            ))}
          </div>

          {/* Chart area */}
          <motion.div 
            variants={{
              hidden: { opacity: 0 },
              visible: { opacity: 1, transition: { duration: 0.4 } }
            }}
            style={{
              background: "var(--neutral-800)",
              border: "1px solid var(--neutral-700)",
              borderRadius: 6, height: 100, marginBottom: 16,
              display: "flex", alignItems: "flex-end",
              padding: "0 12px 12px",
              gap: 3,
              overflow: "hidden",
            }}
          >
            {[35,52,40,68,58,75,65,82,70,88,76,94,80,100,88].map((h, i) => (
              <motion.div
                key={i}
                initial={{ height: 0 }}
                whileInView={{ height: `${h}%` }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: 0.2 + i * 0.03, ease: [0.16, 1, 0.3, 1] }}
                style={{
                  flex: 1,
                  background: i === 14 ? "var(--neutral-300)" : "var(--neutral-700)",
                  borderRadius: "2px 2px 0 0",
                  minWidth: 0,
                  transformOrigin: "bottom",
                }}
              />
            ))}
          </motion.div>

          {/* Invoice table rows */}
          <motion.div variants={{ visible: { transition: { staggerChildren: 0.1, delayChildren: 0.4 } } }}>
            {[
              { num: "INV-0031", name: "Acme Corp", amount: "₹12,000", status: "Paid" },
              { num: "INV-0030", name: "Bright Labs", amount: "₹5,400", status: "Open" },
              { num: "INV-0029", name: "Zeta Inc", amount: "₹18,200", status: "Paid" },
            ].map((row, i) => (
              <motion.div 
                key={row.num}
                variants={{
                  hidden: { opacity: 0, x: -10 },
                  visible: { opacity: 1, x: 0, transition: { type: "spring", stiffness: 300, damping: 25 } }
                }}
                style={{
                  display: "flex", alignItems: "center",
                  justifyContent: "space-between",
                  padding: "6px 0",
                  borderBottom: "1px solid var(--neutral-800)",
                  fontSize: 11, fontFamily: "var(--font-sans)",
                }}
              >
                <span style={{ color: "var(--neutral-400)", fontFamily: "var(--font-mono)", fontSize: 10 }}>
                  {row.num}
                </span>
                <span style={{ color: "var(--neutral-300)", flex: 1, paddingLeft: 12 }}>{row.name}</span>
                <span style={{ color: "var(--neutral-200)", fontVariantNumeric: "tabular-nums" }}>{row.amount}</span>
                <span style={{
                  marginLeft: 12,
                  padding: "1px 6px", borderRadius: 3,
                  fontSize: 10,
                  background: row.status === "Paid" ? "var(--green-soft)" : "var(--amber-soft)",
                  color: row.status === "Paid" ? "var(--green)" : "var(--amber)",
                }}>
                  {row.status}
                </span>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </div>
    </motion.div>
  );
}

// ── Feature Row ─────────────────────────────────────────────
function FeatureRow({ eyebrow, title, desc, visual, flip = false }: {
  eyebrow: string; title: string; desc: string; visual: React.ReactNode; flip?: boolean;
}) {
  return (
    <div style={{
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 80,
      alignItems: "center",
      padding: "80px 0",
      borderBottom: "1px solid var(--neutral-800)",
    }}
      className="flex-col-mobile"
    >
      <FadeUp delay={0.05} className={flip ? "order-2" : ""}>
        <div style={{ fontSize: 11, fontWeight: 500, letterSpacing: "0.1em", textTransform: "uppercase", color: "var(--neutral-500)", marginBottom: 16 }}>
          {eyebrow}
        </div>
        <h3 style={{ fontSize: "clamp(1.5rem, 2.5vw, 2rem)", fontWeight: 500, letterSpacing: "-0.025em", lineHeight: 1.2, color: "var(--neutral-50)", marginBottom: 16 }}>
          {title}
        </h3>
        <p style={{ fontSize: 15, color: "var(--neutral-400)", lineHeight: 1.7, maxWidth: 380 }}>
          {desc}
        </p>
      </FadeUp>

      <FadeUp delay={0.15} className={flip ? "order-1" : ""}>
        {visual}
      </FadeUp>
    </div>
  );
}

// ── Billing Engine Visual ────────────────────────────────────
function BillingEngineVisual() {
  return (
    <div style={{
      background: "var(--neutral-900)",
      border: "1px solid var(--neutral-800)",
      borderRadius: 10, padding: 24,
      fontFamily: "var(--font-mono)",
    }}>
      <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 16 }}>
        Nightly Billing Engine — 02:00 UTC
      </div>
      {[
        { time: "02:00:01", msg: "Processing 48 active subscriptions...", ok: true },
        { time: "02:00:03", msg: "Usage aggregated for metered plans", ok: true },
        { time: "02:00:04", msg: "Generated INV-0031 for Acme Corp • ₹12,000", ok: true },
        { time: "02:00:04", msg: "Generated INV-0032 for Bright Labs • ₹5,400", ok: true },
        { time: "02:00:05", msg: "Applied proration for plan upgrade • Zeta Inc", ok: true },
        { time: "02:00:06", msg: "Dunning: 2 reminders dispatched", ok: true },
        { time: "02:00:06", msg: "Completed. 46 invoices created.", ok: true },
      ].map((log, i) => (
        <motion.div
          key={i}
          initial={{ opacity: 0, x: -4 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.1 + i * 0.12, duration: 0.3 }}
          style={{
            display: "flex", alignItems: "baseline", gap: 12,
            padding: "3px 0",
            fontSize: 12, lineHeight: 1.6,
          }}
        >
          <span style={{ color: "var(--neutral-600)", flexShrink: 0, fontSize: 10 }}>{log.time}</span>
          <span style={{ color: log.ok ? "var(--neutral-300)" : "var(--red)" }}>
            {log.ok && <span style={{ color: "var(--green)", marginRight: 6 }}>✓</span>}
            {log.msg}
          </span>
        </motion.div>
      ))}
    </div>
  );
}

// ── Metering Visual ──────────────────────────────────────────
function MeteringVisual() {
  const events = [
    { customer: "acme-corp", event: "api.call", units: 1, ts: "14:22:01.334" },
    { customer: "bright-labs", event: "data.transfer", units: 128, ts: "14:22:01.891" },
    { customer: "acme-corp", event: "api.call", units: 1, ts: "14:22:02.001" },
    { customer: "zeta-inc", event: "storage.write", units: 512, ts: "14:22:02.445" },
    { customer: "bright-labs", event: "api.call", units: 1, ts: "14:22:02.881" },
  ];

  return (
    <div style={{
      background: "var(--neutral-900)",
      border: "1px solid var(--neutral-800)",
      borderRadius: 10, padding: 24,
    }}>
      <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 16, fontFamily: "var(--font-mono)" }}>
        Live event stream — 847 events/sec
        <span style={{ display: "inline-block", width: 6, height: 6, borderRadius: "50%", background: "var(--green)", marginLeft: 8, verticalAlign: "middle", animation: "pulse-live 1.5s ease infinite" }} />
      </div>
      {events.map((ev, i) => (
        <motion.div
          key={i}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: i * 0.15 }}
          style={{
            display: "flex", alignItems: "center", gap: 10,
            padding: "5px 0",
            borderBottom: "1px solid var(--neutral-800)",
            fontSize: 11,
          }}
        >
          <span style={{ color: "var(--neutral-600)", fontFamily: "var(--font-mono)", fontSize: 10, flexShrink: 0 }}>{ev.ts}</span>
          <span style={{ color: "var(--neutral-500)", flexShrink: 0 }}>{ev.customer}</span>
          <span style={{ color: "var(--neutral-300)", flex: 1, fontFamily: "var(--font-mono)" }}>{ev.event}</span>
          <span style={{ color: "var(--neutral-400)", fontFamily: "var(--font-mono)" }}>×{ev.units}</span>
        </motion.div>
      ))}
    </div>
  );
}

// ── Metrics Strip ────────────────────────────────────────────
const METRICS = [
  { val: "₹4.2L", label: "MRR" },
  { val: "48",    label: "Active Subscriptions" },
  { val: "1.8%",  label: "Churn Rate" },
  { val: "99.97%",label: "Uptime SLA" },
  { val: "<2ms",  label: "API Latency" },
  { val: "130+",  label: "Tax Jurisdictions" },
];

// ── Main Page ─────────────────────────────────────────────────
export default function LandingPage() {
  const heroRef = useRef<HTMLElement>(null);
  const glowPos = useCursorGlow(heroRef);

  return (
    <div style={{ background: "var(--neutral-950)", minHeight: "100dvh" }}>
      <Nav />

      {/* Hero — with cursor glow */}
      <section
        ref={heroRef}
        style={{
          position: "relative",
          maxWidth: 1100,
          margin: "0 auto",
          padding: "140px 40px 80px",
          overflow: "hidden",
        }}
      >
        {/* GPU-cheap radial glow that follows cursor */}
        {glowPos && (
          <motion.div
            style={{
              position: "absolute",
              left: 0,
              top: 0,
              width: "100%",
              height: "100%",
              pointerEvents: "none",
              zIndex: 0,
              background: glowPos
                ? undefined
                : "none",
            }}
            aria-hidden="true"
          >
            <motion.div
              style={{
                position: "absolute",
                width: 500,
                height: 500,
                borderRadius: "50%",
                background: "radial-gradient(circle, rgba(255,255,255,0.04) 0%, transparent 70%)",
                transform: "translate(-50%, -50%)",
                pointerEvents: "none",
                x: glowPos.x,
                y: glowPos.y,
              }}
            />
          </motion.div>
        )}

        {/* Headline — word-by-word reveal */}
        <h1
          style={{
            fontSize: "clamp(2.8rem, 6vw, 4.5rem)",
            fontWeight: 500,
            letterSpacing: "-0.04em",
            lineHeight: 1.15,
            color: "var(--neutral-50)",
            maxWidth: 740,
            marginBottom: 24,
            position: "relative",
            zIndex: 1,
          }}
        >
          <WordReveal text="Billing infrastructure" />
          <span style={{ color: "var(--neutral-400)", display: "block", minHeight: "1.2em" }}>
            <Typewriter words={[
              "built for scale.",
              "designed for founders.",
              "automated for finance.",
              "loved by developers."
            ]} />
          </span>
        </h1>

        {/* Subhead */}
        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.5, ease: EASE_OUT_EXPO }}
          style={{
            fontSize: 17, lineHeight: 1.65,
            color: "var(--neutral-400)",
            maxWidth: 500, marginBottom: 40,
            position: "relative", zIndex: 1,
          }}
        >
          Meter usage in real-time, automate invoicing, and understand
          your revenue with deep analytics. Trusted by finance teams.
        </motion.p>

        {/* CTAs — magnetic on primary */}
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.56, ease: EASE_OUT_EXPO }}
          style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 24, position: "relative", zIndex: 1, flexWrap: "wrap" }}
        >
          <MagneticButton>
            <Link href="/login" style={{ textDecoration: "none" }}>
              <button style={{
                padding: "10px 20px",
                background: "var(--neutral-50)",
                border: "none",
                borderRadius: 7,
                fontSize: 14, fontWeight: 500,
                color: "var(--neutral-950)",
                cursor: "pointer",
                transition: "background 150ms ease, box-shadow 200ms ease",
              }}
                onMouseEnter={e => {
                  (e.currentTarget as HTMLButtonElement).style.background = "white";
                  (e.currentTarget as HTMLButtonElement).style.boxShadow = "0 0 24px rgba(255,255,255,0.15)";
                }}
                onMouseLeave={e => {
                  (e.currentTarget as HTMLButtonElement).style.background = "var(--neutral-50)";
                  (e.currentTarget as HTMLButtonElement).style.boxShadow = "none";
                }}
              >
                Get started free
              </button>
            </Link>
          </MagneticButton>
          <Link href="/login" style={{ textDecoration: "none" }}>
            <button style={{
              padding: "10px 20px",
              background: "transparent",
              border: "1px solid var(--neutral-700)",
              borderRadius: 7,
              fontSize: 14, fontWeight: 500,
              color: "var(--neutral-300)",
              cursor: "pointer",
              transition: "all 150ms ease",
            }}
              onMouseEnter={e => {
                (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--neutral-500)";
                (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-100)";
              }}
              onMouseLeave={e => {
                (e.currentTarget as HTMLButtonElement).style.borderColor = "var(--neutral-700)";
                (e.currentTarget as HTMLButtonElement).style.color = "var(--neutral-300)";
              }}
            >
              View dashboard →
            </button>
          </Link>
        </motion.div>

        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.62, duration: 0.4 }}
          style={{ fontSize: 12, color: "var(--neutral-600)", position: "relative", zIndex: 1 }}
        >
          No credit card required · 14-day free trial · Cancel anytime
        </motion.p>
      </section>

      {/* Dashboard Preview */}
      <section style={{ maxWidth: 1100, margin: "0 auto", padding: "0 40px 100px" }}>
        <motion.div
          initial={{ opacity: 0, y: 30, rotateX: 4 }}
          animate={{ opacity: 1, y: 0, rotateX: 0 }}
          transition={{ duration: 0.8, delay: 0.25, ease: [0.16, 1, 0.3, 1] }}
          style={{
            transformOrigin: "50% 0%",
            perspective: 1200,
            boxShadow: "0 40px 80px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.05)",
            borderRadius: 12,
          }}
        >
          <DashboardPreview />
        </motion.div>
      </section>

      {/* Metrics strip */}
      <section style={{
        borderTop: "1px solid var(--neutral-800)",
        borderBottom: "1px solid var(--neutral-800)",
        padding: "40px 0",
      }}>
        <div style={{
          maxWidth: 1100, margin: "0 auto",
          display: "grid",
          gridTemplateColumns: "repeat(6, 1fr)",
          gap: 0,
          padding: "0 40px",
        }}
          className="grid-cols-3-mobile"
        >
          {METRICS.map((m, i) => (
            <FadeUp key={m.label} delay={i * 0.07}>
              <div style={{
                padding: "20px 24px",
                borderRight: i < METRICS.length - 1 ? "1px solid var(--neutral-800)" : "none",
              }}>
                <div style={{ fontSize: 22, fontWeight: 500, letterSpacing: "-0.03em", color: "var(--neutral-50)", marginBottom: 4 }}>
                  {m.val}
                </div>
                <div style={{ fontSize: 12, color: "var(--neutral-500)" }}>{m.label}</div>
              </div>
            </FadeUp>
          ))}
        </div>
      </section>

      {/* Feature sections */}
      <section style={{ maxWidth: 1100, margin: "0 auto", padding: "0 40px" }}>
        <FeatureRow
          eyebrow="Billing Engine"
          title="Automated invoicing that just works."
          desc="A cron-driven nightly billing engine processes every active subscription — aggregating usage, applying proration, generating invoices, and triggering payment flows. Zero manual intervention."
          visual={<BillingEngineVisual />}
        />

        <FeatureRow
          eyebrow="Usage Metering"
          title="Ingest usage events at any scale."
          desc="Send usage events via REST API. SubMeter aggregates them per billing period, applies your tiered or metered pricing model, and computes the exact invoice amount."
          visual={<MeteringVisual />}
          flip
        />

        <FeatureRow
          eyebrow="Revenue Analytics"
          title="MRR, Churn, ARPU — live."
          desc="A real-time analytics engine gives you the metrics that matter to SaaS founders and investors. Drill into individual customer revenue, track cohort churn, and export to CSV."
          visual={
            <div style={{
              background: "var(--neutral-900)",
              border: "1px solid var(--neutral-800)",
              borderRadius: 10, padding: 24,
            }}>
              <div style={{ fontSize: 11, color: "var(--neutral-500)", marginBottom: 20, fontFamily: "var(--font-mono)" }}>
                MRR growth — last 90 days
              </div>
              <div style={{ display: "flex", alignItems: "flex-end", gap: 2, height: 100 }}>
                {Array.from({ length: 45 }, (_, i) => {
                  const h = 30 + Math.sin(i * 0.18) * 15 + i * 1.3;
                  return (
                    <motion.div
                      key={i}
                      initial={{ height: 0 }}
                      whileInView={{ height: `${Math.min(h, 100)}%` }}
                      viewport={{ once: true }}
                      transition={{ delay: i * 0.015, duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
                      style={{
                        flex: 1, borderRadius: "1px 1px 0 0",
                        background: i > 40 ? "var(--neutral-200)" : "var(--neutral-700)",
                      }}
                    />
                  );
                })}
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10, marginTop: 20 }}>
                {[
                  { label: "MRR", val: "₹4.2L", delta: "+12%" },
                  { label: "Churn", val: "1.8%", delta: "-0.3%" },
                  { label: "ARPU", val: "₹8,400", delta: "+4%" },
                ].map(kpi => (
                  <div key={kpi.label} style={{
                    padding: "10px 12px",
                    background: "var(--neutral-800)",
                    border: "1px solid var(--neutral-700)",
                    borderRadius: 6,
                  }}>
                    <div style={{ fontSize: 9, color: "var(--neutral-500)", letterSpacing: "0.08em", textTransform: "uppercase", fontFamily: "var(--font-sans)" }}>
                      {kpi.label}
                    </div>
                    <div style={{ fontSize: 16, fontWeight: 600, color: "var(--neutral-50)", letterSpacing: "-0.02em", margin: "2px 0" }}>
                      {kpi.val}
                    </div>
                    <div style={{ fontSize: 10, color: "var(--green)" }}>{kpi.delta}</div>
                  </div>
                ))}
              </div>
            </div>
          }
        />
      </section>

      {/* CTA */}
      <section style={{
        borderTop: "1px solid var(--neutral-800)",
        padding: "100px 40px",
        textAlign: "center",
      }}>
        <FadeUp>
          <div style={{ fontSize: 11, fontWeight: 500, letterSpacing: "0.1em", textTransform: "uppercase", color: "var(--neutral-500)", marginBottom: 20 }}>
            Ready to ship
          </div>
          <h2 style={{ fontSize: "clamp(2rem, 4vw, 3rem)", fontWeight: 500, letterSpacing: "-0.03em", color: "var(--neutral-50)", marginBottom: 20 }}>
            Your billing infrastructure,<br />
            <span style={{ color: "var(--neutral-400)" }}>from day one.</span>
          </h2>
          <p style={{ fontSize: 15, color: "var(--neutral-500)", marginBottom: 40, maxWidth: 420, margin: "0 auto 40px" }}>
            Stop building billing. Focus on your product. SubMeter handles the rest.
          </p>
          <div style={{ display: "flex", justifyContent: "center", gap: 10 }}>
            <MagneticButton>
              <Link href="/login" style={{ textDecoration: "none" }}>
                <button style={{
                  padding: "12px 24px",
                  background: "var(--neutral-50)",
                  border: "none",
                  borderRadius: 7,
                  fontSize: 14, fontWeight: 500,
                  color: "var(--neutral-950)",
                  cursor: "pointer",
                  transition: "background 150ms ease, box-shadow 200ms ease",
                }}
                  onMouseEnter={e => {
                    (e.currentTarget as HTMLButtonElement).style.background = "white";
                    (e.currentTarget as HTMLButtonElement).style.boxShadow = "0 0 28px rgba(255,255,255,0.15)";
                  }}
                  onMouseLeave={e => {
                    (e.currentTarget as HTMLButtonElement).style.background = "var(--neutral-50)";
                    (e.currentTarget as HTMLButtonElement).style.boxShadow = "none";
                  }}
                >
                  Create your account
                </button>
              </Link>
            </MagneticButton>
          </div>
        </FadeUp>
      </section>

      {/* Footer */}
      <footer style={{
        borderTop: "1px solid var(--neutral-800)",
        padding: "32px 40px",
        display: "flex", alignItems: "center", justifyContent: "space-between",
        maxWidth: 1100, margin: "0 auto",
      }}>
        <Logo />
        <div style={{ display: "flex", gap: 24 }}>
          {["Privacy", "Terms", "Status", "Docs"].map(item => (
            <a key={item} href="#" style={{
              fontSize: 12, color: "var(--neutral-500)", textDecoration: "none",
              transition: "color 150ms ease",
            }}
              onMouseEnter={e => (e.currentTarget.style.color = "var(--neutral-300)")}
              onMouseLeave={e => (e.currentTarget.style.color = "var(--neutral-500)")}
            >
              {item}
            </a>
          ))}
        </div>
        <div style={{ fontSize: 12, color: "var(--neutral-600)" }}>
          © 2025 SubMeter
        </div>
      </footer>
    </div>
  );
}
