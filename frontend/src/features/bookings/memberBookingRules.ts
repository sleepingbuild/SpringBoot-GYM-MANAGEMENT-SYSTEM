import type { Booking, BookingStatus } from "@/features/schedule/bookingTypes";

/** Nhan trang thai nhin tu phia hoc vien. */
export const MEMBER_STATUS_LABEL: Record<BookingStatus, string> = {
  PENDING: "Chờ PT xác nhận",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
  LATE_CANCELLED: "Huỷ muộn (tính 1 buổi)",
  NO_SHOW: "Bạn vắng mặt (tính 1 buổi)",
  PT_NO_SHOW: "PT vắng (không tính buổi)",
};

export const isOpenBooking = (b: Booking) => b.status === "PENDING" || b.status === "CONFIRMED";

const sortKey = (b: Booking) => `${b.bookingDate}T${b.startTime}`;

/** Buoi con hieu luc xep theo thoi gian tang dan; buoi da ket thuc xep moi nhat truoc. */
export function splitMemberBookings(items: Booking[]): { upcoming: Booking[]; past: Booking[] } {
  return {
    upcoming: items.filter(isOpenBooking).sort((a, b) => sortKey(a).localeCompare(sortKey(b))),
    past: items.filter((b) => !isOpenBooking(b)).sort((a, b) => sortKey(b).localeCompare(sortKey(a))),
  };
}

/**
 * Loi nhac truoc khi huy. Khong ghi con so gio cu the vi nguong huy muon la cau hinh cua backend;
 * backend moi la noi quyet dinh co tinh la huy muon hay khong.
 */
export function cancelNotice(status: BookingStatus): string {
  if (status === "PENDING") {
    return "Huỷ buổi này? Buổi chưa được PT xác nhận nên huỷ miễn phí.";
  }
  return "Huỷ buổi này? Buổi đã được PT xác nhận: huỷ sát giờ bắt đầu sẽ bị tính là đã dùng một buổi.";
}

/** Thong bao sau khi huy, dua vao trang thai backend tra ve. */
export function cancelDoneMessage(status: BookingStatus): string {
  return status === "LATE_CANCELLED"
    ? "Đã huỷ buổi. Vì huỷ sát giờ nên buổi này vẫn bị tính."
    : "Đã huỷ buổi.";
}
