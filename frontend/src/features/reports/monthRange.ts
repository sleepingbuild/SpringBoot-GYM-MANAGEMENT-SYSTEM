import type { MonthRange } from "./types";

export const MAX_RANGE_MONTHS = 12;

/** Tháng theo giờ máy người dùng (Việt Nam), dạng yyyy-MM. Không đổi múi giờ. */
export function toIsoMonth(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

/** Mặc định của backend: chỉ tháng hiện tại. */
export function defaultMonthRange(today: Date = new Date()): MonthRange {
  const m = toIsoMonth(today);
  return { from: m, to: m };
}

function monthIndex(iso: string): number | null {
  const m = /^(\d{4})-(0[1-9]|1[0-2])$/.exec(iso);
  return m ? Number(m[1]) * 12 + Number(m[2]) - 1 : null;
}

/** Trả thông báo lỗi tiếng Việt, hoặc null nếu khoảng tháng hợp lệ (khớp quy tắc backend). */
export function monthRangeError(range: MonthRange): string | null {
  const from = monthIndex(range.from);
  const to = monthIndex(range.to);
  if (from === null || to === null) return "Chọn đủ tháng bắt đầu và tháng kết thúc (dạng yyyy-MM).";
  if (from > to) return "Tháng bắt đầu phải trước hoặc bằng tháng kết thúc.";
  if (to - from + 1 > MAX_RANGE_MONTHS) return `Khoảng thời gian tối đa ${MAX_RANGE_MONTHS} tháng.`;
  return null;
}
