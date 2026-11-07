import type { FaceAttendanceResult } from "./types";

export type ScanTone = "success" | "info" | "warning";

export interface ScanView {
  tone: ScanTone;
  title: string;
  detail: string | null;
}

const TARGET_LABEL: Record<string, string> = {
  BOOKING: "Buổi tập với PT",
  STAFF_ATTENDANCE: "Chấm công",
  GYM_CHECK_IN: "Vào phòng tập",
};

/** Đổi kết quả điểm danh của backend thành nội dung hiển thị. */
export function describeScan(r: FaceAttendanceResult): ScanView {
  const name = r.matchedUserName;

  if (r.duplicateScan) {
    return {
      tone: "warning",
      title: `${name}: bạn vừa quét xong`,
      detail: "Lượt quét lại quá sớm không được ghi nhận. Đợi một lát rồi quét lại nếu cần.",
    };
  }

  if (r.action === "NONE") {
    return { tone: "info", title: r.message || "Không có thao tác nào được ghi nhận", detail: name };
  }

  const verb = r.action === "CHECK_OUT" ? "đã ghi nhận giờ ra" : "đã ghi nhận giờ vào";

  if (r.reasonRequired) {
    return { tone: "warning", title: `${name}: ${verb}`, detail: "Bạn rời buổi sớm, cần nhập lý do bên dưới." };
  }

  const target = r.targetType ? (TARGET_LABEL[r.targetType] ?? r.targetType) : null;
  return { tone: "success", title: `${name}: ${verb}`, detail: target };
}
