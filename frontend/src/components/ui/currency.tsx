/**
 * <Currency> — canonical money renderer.
 *
 * Split-weight tabular rendering:
 *   Integer part: spec weight (500), full opacity, JetBrains Mono
 *   Cents part:   60% opacity, one size step down, same font
 *
 * WCAG AA note: 60% opacity on gray-100 (#F0F0F2) over gray-800 (#1C1C1F) = ~5.8:1 ✓
 */

interface CurrencyProps {
  /** Amount in paise (1/100 INR) */
  amountPaise: number;
  /** Display size variant */
  size?: "hero" | "xl" | "base";
  /** Currency symbol, default ₹ */
  symbol?: string;
  className?: string;
}

const sizeMap: Record<
  NonNullable<CurrencyProps["size"]>,
  { intClass: string; centsClass: string }
> = {
  hero: {
    intClass: "text-hero text-gradient-accent",
    centsClass: "text-[1.25rem] font-medium leading-[1.3] text-gradient-accent",
  },
  xl: {
    intClass: "text-[1.25rem] font-medium leading-[1.3]",
    centsClass: "text-[0.9375rem] font-normal leading-[1.5]",
  },
  base: {
    intClass: "text-[0.9375rem] font-normal leading-[1.5]",
    centsClass: "text-[0.8125rem] font-normal leading-[1.4]",
  },
};

export function Currency({
  amountPaise,
  size = "base",
  symbol = "₹",
  className = "",
}: CurrencyProps) {
  // Handle negative amounts
  const negative = amountPaise < 0;
  const abs = Math.abs(amountPaise);

  const rupees = Math.floor(abs / 100);
  const paise = abs % 100;

  const rupeesFormatted = rupees.toLocaleString("en-IN");
  const paiseFormatted = String(paise).padStart(2, "0");

  const { intClass, centsClass } = sizeMap[size];

  return (
    <span
      className={`inline-flex items-baseline gap-px font-mono ${className}`}
      style={{ fontVariantNumeric: "tabular-nums" }}
      data-mono
    >
      {negative && (
        <span className={intClass} style={{ color: "var(--danger)" }}>
          −
        </span>
      )}
      <span className={intClass}>
        {symbol}
        {rupeesFormatted}
      </span>
      <span
        className={centsClass}
        style={{ opacity: 0.6 }}
        aria-hidden="true"
      >
        .{paiseFormatted}
      </span>
    </span>
  );
}
