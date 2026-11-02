const MONEY = new Intl.NumberFormat("vi-VN");
const COMPACT = new Intl.NumberFormat("vi-VN", { notation: "compact", maximumFractionDigits: 1 });
const PERCENT = new Intl.NumberFormat("vi-VN", { style: "percent", minimumFractionDigits: 0, maximumFractionDigits: 1 });

export function formatMoney(value: number): string {
  return `${MONEY.format(value)} ₫`;
}

export function formatCompact(value: number): string {
  return COMPACT.format(value);
}

export function formatNumber(value: number): string {
  return MONEY.format(value);
}

/** rate trong [0, 1]. */
export function formatPercent(rate: number): string {
  return PERCENT.format(rate);
}

/** yyyy-MM-dd thành dd/MM/yyyy. Không dùng Date để tránh lệch múi giờ. */
export function formatIsoDate(iso: string): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  return m ? `${m[3]}/${m[2]}/${m[1]}` : iso;
}

/** Nhãn kỳ doanh thu: yyyy-MM-dd thành dd/MM, yyyy-MM thành MM/yyyy. */
export function formatPeriod(period: string): string {
  const day = /^(\d{4})-(\d{2})-(\d{2})$/.exec(period);
  if (day) return `${day[3]}/${day[2]}`;
  const month = /^(\d{4})-(\d{2})$/.exec(period);
  return month ? `${month[2]}/${month[1]}` : period;
}
