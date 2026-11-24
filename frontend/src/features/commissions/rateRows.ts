import type { Trainer } from "./types";

export interface RateRow {
  trainerId: string;
  name: string;
  /** null = PT chưa được đặt mức lương mỗi buổi (backend trả 404). */
  amount: number | null;
  failed: boolean;
}

/** Ghép danh sách PT với kết quả tải mức lương (cùng thứ tự). amount undefined/null = chưa đặt; lỗi khác 404 = failed. */
export function toRateRows(
  trainers: Trainer[],
  results: ({ ok: true; amount: number | null } | { ok: false })[],
): RateRow[] {
  return trainers.map((t, i) => {
    const r = results[i];
    return {
      trainerId: t.id,
      name: t.fullName,
      amount: r && r.ok ? r.amount : null,
      failed: !r || !r.ok,
    };
  });
}
