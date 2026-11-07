import type { PackageItem } from "./types";

/** 1500000 -> "1.500.000 ₫". Tự định dạng để kết quả không phụ thuộc dữ liệu ngôn ngữ của trình duyệt. */
export function formatVnd(amount: number): string {
  const rounded = Math.round(amount);
  const sign = rounded < 0 ? "-" : "";
  return `${sign}${Math.abs(rounded).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".")} ₫`;
}

/** "2026-10-27" -> "27/10/2026". Cắt chuỗi, không tạo Date, để không bị lệch múi giờ. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  return m ? `${m[3]}/${m[2]}/${m[1]}` : value;
}

/** "2026-10-27T17:05:09.123" -> "17:05 27/10/2026" (giờ Việt Nam do backend trả, hiển thị nguyên). */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const m = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/.exec(value);
  return m ? `${m[4]}:${m[5]} ${m[3]}/${m[2]}/${m[1]}` : value;
}

export function shortId(id: string | null | undefined): string {
  return id ? id.slice(0, 8) : "—";
}

/** Mô tả ngắn quyền lợi của gói, ví dụ "12 buổi · 90 ngày". */
export function packageSummary(p: Pick<PackageItem, "durationDays" | "sessionCount">): string {
  const parts: string[] = [];
  if (p.sessionCount != null) parts.push(`${p.sessionCount} buổi`);
  if (p.durationDays != null) parts.push(`${p.durationDays} ngày`);
  return parts.length > 0 ? parts.join(" · ") : "Không giới hạn";
}

/** Giải thích giới hạn đặt PT theo tuần (NULL = không giới hạn, 0 = không cho đặt PT, N = tối đa N buổi/tuần). */
export function weeklyLimitText(max: number | null): string {
  if (max == null) return "Không giới hạn";
  if (max === 0) return "Không đặt được buổi PT";
  return `Tối đa ${max} buổi PT mỗi tuần`;
}
