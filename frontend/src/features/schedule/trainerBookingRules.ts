import type { Booking } from "./bookingTypes";

/** Khop mac dinh gms.booking.min-complete-minutes cua backend (backend moi la noi quyet dinh, day chi de nhac truoc). */
export const MIN_COMPLETE_MINUTES = 45;
export const MIN_REASON_LENGTH = 5;

/** Ngay yyyy-MM-dd + gio HH:mm[:ss] (khong mui gio) -> Date theo gio may PT (gia dinh o Viet Nam). */
const at = (date: string, time: string) => new Date(`${date}T${time.length === 5 ? `${time}:00` : time.slice(0, 8)}`);

const hasSavedReason = (b: Booking) => (b.ptEarlyLeaveReason ?? "").trim().length > 0;

/**
 * So phut PT da day: tu luc PT quet vao (chua quet thi tu gio bat dau theo lich) den luc PT quet ra
 * (chua quet ra thi den bay gio). Giong cach backend tinh khi PT bam "Hoan thanh".
 */
export function workedMinutes(b: Booking, now: Date): number {
  const start = b.ptCheckInTime ? new Date(b.ptCheckInTime) : at(b.bookingDate, b.startTime);
  const end = b.ptCheckOutTime ? new Date(b.ptCheckOutTime) : now;
  return Math.floor((end.getTime() - start.getTime()) / 60000);
}

export interface CompletionPlan {
  minutes: number;
  /** Duoi so phut toi thieu va chua co ly do: bat buoc nhap ly do moi hoan thanh duoc. */
  reasonRequired: boolean;
  /** Du so phut nhung chua du gio ket thuc theo lich va chua co ly do: hoan thanh duoc nhung khong tinh cong. */
  reasonSuggested: boolean;
}

export function completionPlan(b: Booking, now: Date): CompletionPlan {
  const minutes = workedMinutes(b, now);
  const reasoned = hasSavedReason(b);
  const effectiveEnd = b.ptCheckOutTime ? new Date(b.ptCheckOutTime) : now;
  const scheduledEnd = at(b.bookingDate, b.endTime);
  const reasonRequired = minutes < MIN_COMPLETE_MINUTES && !reasoned;
  const reasonSuggested = !reasonRequired && !reasoned && effectiveEnd.getTime() < scheduledEnd.getTime();
  return { minutes, reasonRequired, reasonSuggested };
}

/** Loi chan truoc khi goi API ("khong cho luu"), hoac null neu hop le. Backend van kiem tra lai. */
export function completeBlocker(plan: CompletionPlan, draft: string): string | null {
  const d = draft.trim();
  if (d.length > 0 && d.length < MIN_REASON_LENGTH) return `Lý do phải từ ${MIN_REASON_LENGTH} ký tự trở lên.`;
  if (plan.reasonRequired && d.length === 0) {
    return `Buổi chưa đủ ${MIN_COMPLETE_MINUTES} phút, cần nhập lý do để hoàn thành.`;
  }
  return null;
}

/** Canh bao truoc khi hoan thanh (hoi lai), hoac null neu khong can. */
export function completeWarning(plan: CompletionPlan, draft: string): string | null {
  if (!plan.reasonSuggested || draft.trim().length > 0) return null;
  return "Buổi chưa đủ thời lượng theo lịch và bạn chưa nhập lý do. Nếu hoàn thành bây giờ, buổi này sẽ không được tính công cho bạn và không thể nộp lý do sau đó. Vẫn hoàn thành buổi?";
}
