import type { Booking } from "@/features/schedule/bookingTypes";

export interface MemberStats {
  /** Buổi đã hoàn thành. */
  completed: number;
  /** Buổi còn hiệu lực (chờ xác nhận hoặc đã xác nhận) từ hôm nay trở đi. */
  upcoming: number;
  /** Buổi hoàn thành trong 30 ngày gần nhất (tính cả hôm nay). */
  completedLast30Days: number;
  /** Buổi bị tính vào gói dù chưa tập đủ: huỷ muộn hoặc bạn vắng mặt. */
  chargedWithoutTraining: number;
  /** Buổi PT vắng, không tính vào gói. */
  ptNoShow: number;
  /** Buổi sắp tới gần nhất, null nếu chưa có. */
  nextBooking: Booking | null;
}

/** Cộng/trừ ngày cho chuỗi yyyy-MM-dd, tính theo UTC để không lệch do múi giờ hay giờ mùa hè. */
export function addDays(iso: string, days: number): string {
  const d = new Date(`${iso}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

const startKey = (b: Booking) => `${b.bookingDate}T${b.startTime}`;

/** Thống kê cá nhân từ danh sách buổi của chính hội viên. today dạng yyyy-MM-dd theo giờ địa phương của người dùng. */
export function computeMemberStats(bookings: readonly Booking[], today: string): MemberStats {
  const since = addDays(today, -29);
  const upcoming = bookings
    .filter((b) => (b.status === "PENDING" || b.status === "CONFIRMED") && b.bookingDate >= today)
    .sort((a, b) => startKey(a).localeCompare(startKey(b)));

  return {
    completed: bookings.filter((b) => b.status === "COMPLETED").length,
    upcoming: upcoming.length,
    completedLast30Days: bookings.filter((b) => b.status === "COMPLETED" && b.bookingDate >= since && b.bookingDate <= today).length,
    chargedWithoutTraining: bookings.filter((b) => b.status === "LATE_CANCELLED" || b.status === "NO_SHOW").length,
    ptNoShow: bookings.filter((b) => b.status === "PT_NO_SHOW").length,
    nextBooking: upcoming[0] ?? null,
  };
}
