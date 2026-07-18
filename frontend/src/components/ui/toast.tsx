"use client";

import { motion, AnimatePresence } from "framer-motion";
import { useEffect, useState, useRef } from "react";

interface Toast {
  id: string;
  message: string;
  type: "success" | "error" | "info";
}

let addToast: (msg: string, type: Toast["type"]) => void = () => {};

export const toast = {
  success: (msg: string) => addToast(msg, "success"),
  error:   (msg: string) => addToast(msg, "error"),
  info:    (msg: string) => addToast(msg, "info"),
};

function ToastItem({ t, onRemove }: { t: Toast; onRemove: (id: string) => void }) {
  return (
    <motion.div
      key={t.id}
      layout
      initial={{ opacity: 0, x: 20, scale: 0.95 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      exit={{ opacity: 0, x: 16, scale: 0.95 }}
      transition={{
        default: { type: "spring", stiffness: 400, damping: 28 },
        opacity: { duration: 0.15 },
        x: { duration: 0.15 },
        scale: { duration: 0.15 },
      }}
      style={{
        pointerEvents: "auto",
        display: "flex",
        alignItems: "flex-start",
        gap: 10,
        padding: "12px 16px",
        background: "var(--neutral-900)",
        border: "1px solid var(--neutral-700)",
        borderRadius: 8,
        boxShadow: "0 8px 32px rgba(0,0,0,0.4), 0 2px 8px rgba(0,0,0,0.3)",
        maxWidth: 340,
        minWidth: 240,
        fontSize: 13,
        color: "var(--neutral-200)",
        lineHeight: 1.4,
        backdropFilter: "blur(12px)",
      }}
    >
      <span style={{ marginTop: 1, flexShrink: 0 }}>{icons[t.type]}</span>
      <span style={{ flex: 1 }}>{t.message}</span>
      <button
        onClick={() => onRemove(t.id)}
        style={{ background: "none", border: "none", cursor: "pointer", color: "var(--neutral-500)", padding: 0 }}
      >
        ×
      </button>
    </motion.div>
  );
}

const icons = {
  success: (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <path d="M2.5 7L5.5 10L11.5 4" stroke="var(--green)" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  error: (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <path d="M4 4L10 10M10 4L4 10" stroke="var(--red)" strokeWidth="1.5" strokeLinecap="round"/>
    </svg>
  ),
  info: (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <circle cx="7" cy="7" r="5.5" stroke="var(--blue)" strokeWidth="1.5"/>
      <path d="M7 6.5V9.5" stroke="var(--blue)" strokeWidth="1.5" strokeLinecap="round"/>
      <circle cx="7" cy="4.5" r="0.5" fill="var(--blue)"/>
    </svg>
  ),
};

export function ToastProvider() {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const counterRef = useRef(0);

  useEffect(() => {
    addToast = (message, type) => {
      const id = String(++counterRef.current);
      setToasts(prev => [...prev.slice(-3), { id, message, type }]);
      setTimeout(() => {
        setToasts(prev => prev.filter(t => t.id !== id));
      }, 4000);
    };
  }, []);

  return (
    <div
      style={{
        position: "fixed",
        bottom: 24,
        right: 24,
        zIndex: 9999,
        display: "flex",
        flexDirection: "column",
        gap: 8,
        pointerEvents: "none",
        maxWidth: 360,
      }}
    >
      <AnimatePresence mode="popLayout">
        {toasts.map((t, i) => (
          <div
            key={t.id}
            style={{
              // Cascade offset: each stacked toast peeks slightly (-4px gap)
              marginTop: i > 0 ? -4 : 0,
              pointerEvents: "auto",
            }}
          >
            <ToastItem
              t={t}
              onRemove={id => setToasts(prev => prev.filter(t => t.id !== id))}
            />
          </div>
        ))}
      </AnimatePresence>
    </div>
  );
}
