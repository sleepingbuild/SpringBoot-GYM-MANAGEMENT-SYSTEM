import type { Commission } from "./types";

export interface CommissionTotals {
  count: number;
  total: number;
  pending: number;
  paid: number;
}

/** Cộng các dòng đang hiển thị (một trang), KHÔNG phải tổng cả tháng nếu còn nhiều trang. */
export function sumCommissions(rows: Commission[]): CommissionTotals {
  let total = 0;
  let pending = 0;
  let paid = 0;
  for (const r of rows) {
    total += r.amount;
    if (r.status === "PAID") paid += r.amount;
    else pending += r.amount;
  }
  return { count: rows.length, total, pending, paid };
}
