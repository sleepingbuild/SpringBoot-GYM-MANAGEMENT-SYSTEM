import type { PackageType, PeakType } from "./types";

export const PACKAGE_TYPE_LABELS: Record<PackageType, string> = {
  TIME_BASED: "Theo thời hạn",
  SESSION_BASED: "Theo số buổi",
  PT_1ON1: "Có huấn luyện viên (1 kèm 1)",
};

export const PEAK_TYPE_LABELS: Record<PeakType, string> = {
  OFF_PEAK: "Ngoài giờ cao điểm",
  FULL_TIME: "Cả ngày",
};

export const MEMBERSHIP_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ thanh toán",
  ACTIVE: "Đang dùng",
  SCHEDULED: "Sắp bắt đầu",
  EXPIRED: "Hết hạn",
  CANCELLED: "Đã huỷ",
};

export const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  SUCCESS: "Đã thanh toán",
  FAILED: "Thất bại",
  REFUNDED: "Đã hoàn tiền",
};

export const RELATED_TYPE_LABELS: Record<string, string> = {
  MEMBERSHIP: "Gói tập",
  POS: "Bán lẻ",
};

/** Nhãn tiếng Việt cho một giá trị; giá trị lạ thì hiện nguyên văn để không giấu thông tin. */
export function labelOf(map: Record<string, string>, key: string | null | undefined): string {
  if (!key) return "—";
  return map[key] ?? key;
}

export type Tone = "success" | "warning" | "danger" | "info" | "neutral";

export function membershipTone(status: string): Tone {
  switch (status) {
    case "ACTIVE":
      return "success";
    case "PENDING":
    case "SCHEDULED":
      return "warning";
    case "CANCELLED":
      return "danger";
    default:
      return "neutral";
  }
}

export function paymentTone(status: string): Tone {
  switch (status) {
    case "SUCCESS":
      return "success";
    case "PENDING":
      return "warning";
    case "FAILED":
      return "danger";
    case "REFUNDED":
      return "info";
    default:
      return "neutral";
  }
}
