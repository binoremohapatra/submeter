/**
 * StatusBadge — inline pill badges. Clean, restrained.
 * Replaces the old StatusDot with a more readable format.
 */

type Status =
  | "ACTIVE" | "TRIAL" | "PAST_DUE" | "CANCELED"
  | "DRAFT" | "OPEN" | "PAID" | "VOID" | "UNCOLLECTIBLE"
  | "PENDING" | "SUCCESS" | "FAILED" | "REFUNDED"
  | "FLAT" | "TIERED" | "METERED";

const config: Record<Status, { dot: string; label: string }> = {
  ACTIVE:        { dot: "var(--green)",          label: "Active" },
  TRIAL:         { dot: "var(--blue)",            label: "Trial" },
  PAST_DUE:      { dot: "var(--amber)",           label: "Past Due" },
  CANCELED:      { dot: "var(--neutral-500)",     label: "Canceled" },
  DRAFT:         { dot: "var(--neutral-500)",     label: "Draft" },
  OPEN:          { dot: "var(--amber)",           label: "Open" },
  PAID:          { dot: "var(--green)",           label: "Paid" },
  VOID:          { dot: "var(--neutral-500)",     label: "Void" },
  UNCOLLECTIBLE: { dot: "var(--red)",             label: "Uncollectible" },
  PENDING:       { dot: "var(--amber)",           label: "Pending" },
  SUCCESS:       { dot: "var(--green)",           label: "Success" },
  FAILED:        { dot: "var(--red)",             label: "Failed" },
  REFUNDED:      { dot: "var(--neutral-400)",     label: "Refunded" },
  FLAT:          { dot: "var(--neutral-300)",     label: "Flat" },
  TIERED:        { dot: "var(--neutral-300)",     label: "Tiered" },
  METERED:       { dot: "var(--neutral-300)",     label: "Metered" },
};

interface StatusBadgeProps {
  status: Status | string;
  className?: string;
}

export function StatusBadge({ status, className = "" }: StatusBadgeProps) {
  const c = config[status as Status] ?? { dot: "var(--neutral-500)", label: status };
  const isLive = status === "ACTIVE" || status === "SUCCESS" || status === "PAID";

  return (
    <span
      className={`inline-flex items-center gap-[5px] ${className}`}
      style={{
        fontSize: "11px",
        fontWeight: 500,
        letterSpacing: "0.05em",
        color: "var(--neutral-400)",
        whiteSpace: "nowrap",
      }}
    >
      <span
        aria-hidden="true"
        style={{
          display: "inline-block",
          width: 6,
          height: 6,
          borderRadius: "50%",
          background: c.dot,
          flexShrink: 0,
          animation: isLive ? "pulse-live 2s ease-in-out infinite" : "none",
        }}
      />
      {c.label}
    </span>
  );
}

// Keep old name as alias for backward compat
export { StatusBadge as StatusDot };
