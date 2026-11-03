import type { DateRange } from "./types";

export const MAX_RANGE_DAYS = 366;

/** Ngày theo giờ máy người dùng (Việt Nam), dạng yyyy-MM-dd. Không đổi múi giờ. */
export function toIsoDate(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

/** Mặc định của backend: từ ngày 1 của tháng này đến hôm nay. */
export function defaultRange(today: Date = new Date()): DateRange {
  return { from: toIsoDate(new Date(today.getFullYear(), today.getMonth(), 1)), to: toIsoDate(today) };
}

function utcDay(iso: string): number | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!m) return null;
  return Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3])) / 86_400_000;
}

/** Trả thông báo lỗi tiếng Việt, hoặc null nếu khoảng ngày hợp lệ (khớp quy tắc backend). */
export function rangeError(range: DateRange): string | null {
  const from = utcDay(range.from);
  const to = utcDay(range.to);
  if (from === null || to === null) return "Chọn đủ ngày bắt đầu và ngày kết thúc.";
  if (from > to) return "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";
  if (to - from + 1 > MAX_RANGE_DAYS) return `Khoảng thời gian tối đa ${MAX_RANGE_DAYS} ngày.`;
  return null;
}
