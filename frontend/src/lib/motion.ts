/**
 * SubMeter — Shared Motion Utilities
 *
 * All animation constants and helpers live here. This ensures consistency
 * across every animated component and makes global tuning a one-line change.
 *
 * prefers-reduced-motion: Use MotionConfig with reducedMotion="user" at the root
 * layout level (see providers.tsx). That one config handles ALL Framer Motion
 * animations app-wide — no need to sprinkle checks everywhere.
 */

// ── Easing curves ─────────────────────────────────────────────
/** Expo ease-out — feels fast then smooth. Use for entrances. */
export const EASE_OUT_EXPO: [number, number, number, number] = [0.16, 1, 0.3, 1];

/** Standard ease-in-out. Use for transitions between states. */
export const EASE_IN_OUT: [number, number, number, number] = [0.4, 0, 0.2, 1];

// ── Spring configs ────────────────────────────────────────────
/** Snappy spring. Use for UI responses (button press, hover). */
export const SPRING_SNAPPY = {
  type: "spring" as const,
  stiffness: 400,
  damping: 28,
};

/** Gentle spring with slight overshoot. Use for toasts, panels. */
export const SPRING_GENTLE = {
  type: "spring" as const,
  stiffness: 280,
  damping: 22,
};

/** Smooth spring for layout transitions (layoutId sliding). */
export const SPRING_LAYOUT = {
  type: "spring" as const,
  stiffness: 380,
  damping: 30,
  mass: 1,
};

// ── Duration constants ────────────────────────────────────────
export const DUR_FAST = 0.15;
export const DUR_BASE = 0.25;
export const DUR_SLOW = 0.4;

// ── Reusable animation variants ───────────────────────────────

/** Standard fade-up entrance. Use with FadeUp wrapper or motion.div. */
export const fadeUpVariants = {
  hidden: { opacity: 0, y: 12 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, ease: EASE_OUT_EXPO },
  },
};

/** Row entrance for table/list items. */
export const rowVariants = {
  hidden: { opacity: 0, y: 6 },
  visible: (delay: number) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.25, delay, ease: EASE_OUT_EXPO },
  }),
  exit: {
    opacity: 0,
    scale: 0.98,
    transition: { duration: 0.15 },
  },
};

/** Stagger container for children. */
export const staggerContainer = (stagger = 0.05, delayChildren = 0) => ({
  hidden: {},
  visible: {
    transition: { staggerChildren: stagger, delayChildren },
  },
});

/** Modal backdrop. */
export const backdropVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: 0.1 } },
  exit: { opacity: 0, transition: { duration: 0.15 } },
};

/** Modal/drawer panel. Note 50ms delay after backdrop. */
export const panelVariants = {
  hidden: { opacity: 0, scale: 0.97, y: 8 },
  visible: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: { duration: 0.15, delay: 0.05, ease: EASE_OUT_EXPO },
  },
  exit: {
    opacity: 0,
    scale: 0.97,
    y: 4,
    transition: { duration: 0.12 },
  },
};
