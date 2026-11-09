import type { Booking, BookingStatus } from "@/features/schedule/bookingTypes";

/** Nhan trang thai nhin tu phia nhan vien (le tan, admin). */
export const STAFF_STATUS_LABEL: Record<BookingStatus, string> = {
  PENDING: "Chờ PT xác nhận",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
  LATE_CANCELLED: "Huỷ muộn",
  NO_SHOW: "Học viên vắng",
  PT_NO_SHOW: "PT vắng",
};

const STATUS_ORDER: BookingStatus[] = [
  "PENDING",
  "CONFIRMED",
  "COMPLETED",
  "LATE_CANCELLED",
  "NO_SHOW",
  "PT_NO_SHOW",
  "CANCELLED",
];

/** Xep theo gio bat dau tang dan, khong doi mang dau vao. */
export function sortByStartTime(items: Booking[]): Booking[] {
  return [...items].sort((a, b) => a.startTime.localeCompare(b.startTime));
}

/** So buoi theo tung trang thai, chi gom trang thai co it nhat mot buoi, theo thu tu co dinh. */
export function countByStatus(items: Booking[]): { status: BookingStatus; count: number }[] {
  return STATUS_ORDER.map((status) => ({ status, count: items.filter((b) => b.status === status).length })).filter(
    (x) => x.count > 0,
  );
}
