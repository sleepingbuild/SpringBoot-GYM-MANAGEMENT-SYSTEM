import type { BadgeTone } from "../../components/ui/Badge";

/** Mau nhan trang thai booking (dung chung cho hoc vien va PT). Trang thai la gia tri backend tra ve. */
export function bookingStatusTone(status: string): BadgeTone {
  switch (status) {
    case "PENDING":
      return "warning"; // cho PT xac nhan
    case "CONFIRMED":
      return "info"; // da xac nhan, sap dien ra
    case "COMPLETED":
      return "success";
    case "NO_SHOW":
      return "danger"; // hoc vien vang, mat buoi
    case "LATE_CANCELLED":
      return "warning"; // huy muon, bi tinh buoi
    default:
      return "neutral"; // CANCELLED, PT_NO_SHOW (khong tinh buoi) va gia tri la
  }
}
