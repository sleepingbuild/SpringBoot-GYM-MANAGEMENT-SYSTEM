import type { MemberPackage } from "./types";

/**
 * Logic thuần cho trang "Gói của tôi" kiểu thẻ (tab lọc, chip số ngày, điều kiện Gia hạn).
 * Không phụ thuộc React hay giao diện: đổi giao diện không làm đổi các quy tắc này.
 * Ngày luôn là chuỗi yyyy-MM-dd (giờ Việt Nam), đúng định dạng backend trả về.
 */

export type MembershipTab = "all" | "active" | "history";

const DAY_MS = 86_400_000;

function toUtcDay(iso: string): number {
  const [y, m, d] = iso.split("-").map(Number);
  return Date.UTC(y, m - 1, d);
}

/** Số ngày từ `fromIso` đến `toIso` (âm nếu `toIso` trước `fromIso`). */
export function diffDays(fromIso: string, toIso: string): number {
  return Math.round((toUtcDay(toIso) - toUtcDay(fromIso)) / DAY_MS);
}

/** Ngày hôm nay theo giờ Việt Nam, dạng yyyy-MM-dd (không phụ thuộc múi giờ của máy người dùng). */
export function todayInVietnam(now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(now);
  const get = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

/** Tab "Tất cả" hiện mọi gói; "Đang hoạt động" chỉ gói ACTIVE; "Lịch sử" là gói đã hết hạn hoặc đã hủy. */
export function matchesTab(status: string, tab: MembershipTab): boolean {
  if (tab === "all") return true;
  if (tab === "active") return status === "ACTIVE";
  return status === "EXPIRED" || status === "CANCELLED";
}

export interface DaysChip {
  /** Nhãn đứng trước chip, ví dụ "Trạng thái" hoặc "Đã sử dụng". */
  label: string;
  /** Nội dung chip, ví dụ "Còn 176 ngày". */
  text: string;
  tone: "good" | "warn" | "bad";
}

/**
 * Chip số ngày trên thẻ gói. `endDate` là ngày CUỐI CÙNG gói còn hiệu lực (backend chỉ chuyển EXPIRED khi
 * hôm nay > endDate), nên đúng ngày đó hiện "Hết hạn hôm nay" chứ không phải "Còn 0 ngày".
 * Gói chờ thanh toán, đã lên lịch, đã hủy: không có chip.
 */
export function daysChip(
  mp: Pick<MemberPackage, "status" | "startDate" | "endDate">,
  today: string,
): DaysChip | null {
  if (mp.status === "ACTIVE") {
    if (!mp.endDate) return { label: "Trạng thái", text: "Không thời hạn", tone: "good" };
    const left = diffDays(today, mp.endDate);
    if (left > 0) return { label: "Trạng thái", text: `Còn ${left} ngày`, tone: "good" };
    if (left === 0) return { label: "Trạng thái", text: "Hết hạn hôm nay", tone: "warn" };
    return { label: "Trạng thái", text: "Đã quá hạn", tone: "bad" };
  }
  if (mp.status === "EXPIRED" && mp.endDate) {
    const used = Math.max(0, diffDays(mp.startDate, mp.endDate));
    return { label: "Đã sử dụng", text: `${used} ngày`, tone: "bad" };
  }
  return null;
}

/**
 * Nút "Gia hạn" chỉ cho gói đã hết hạn, và chỉ khi hội viên không còn gói nào đang chạy, chờ thanh toán
 * hoặc đã lên lịch. Lý do: mua tự động chưa chặn việc có hai gói ACTIVE cùng lúc; khi đã có gói thì dùng
 * nâng cấp/hạ cấp, còn gói chờ thanh toán thì chờ lễ tân xác nhận.
 */
export function canRenew(
  mp: Pick<MemberPackage, "status">,
  all: ReadonlyArray<Pick<MemberPackage, "status">>,
): boolean {
  if (mp.status !== "EXPIRED") return false;
  return !all.some((m) => m.status === "ACTIVE" || m.status === "PENDING" || m.status === "SCHEDULED");
}
