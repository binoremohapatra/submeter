"use client";

import { SessionProvider } from "next-auth/react";
import { MotionConfig } from "framer-motion";
import { ToastProvider } from "@/components/ui/toast";
import { CommandPalette } from "@/components/ui/command-palette";

/**
 * MotionConfig reducedMotion="user" is the single global gate for
 * prefers-reduced-motion. It instructs Framer Motion to check the user's
 * OS accessibility setting and skip animations if requested.
 * This covers every motion.* element in the entire app — no per-component
 * checks needed.
 */
export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <SessionProvider>
      <MotionConfig reducedMotion="user">
        {children}
        <CommandPalette />
        <ToastProvider />
      </MotionConfig>
    </SessionProvider>
  );
}
