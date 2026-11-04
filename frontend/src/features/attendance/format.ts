const DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/;
const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;

/**
 * "2026-10-25T18:05:00" -> "18:05 25/10/2026". Chỉ cắt chuỗi, KHÔNG đi qua Date để không bị cộng
 * trừ múi giờ (backend trả giờ Việt Nam, không kèm múi giờ).
 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const m = DATE_TIME.exec(value);
  if (!m) return value;
  const [, year, month, day, hour, minute] = m;
  return `${hour}:${minute} ${day}/${month}/${year}`;
}

/** "2026-10-25" -> "25/10/2026". */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const m = DATE_ONLY.exec(value);
  if (!m) return value;
  const [, year, month, day] = m;
  return `${day}/${month}/${year}`;
}

/** Ngày của máy người dùng dưới dạng yyyy-MM-dd (đúng định dạng ?date= của backend). */
export function toIsoDate(d: Date): string {
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}

export function checkInTypeLabel(type: string): string {
  switch (type) {
    case "GYM_VISIT":
      return "Tập tự do";
    case "PT_SESSION":
      return "Buổi tập với PT";
    default:
      return type;
  }
}

export function methodLabel(method: string | null | undefined): string {
  switch (method) {
    case "FACE":
      return "Khuôn mặt";
    case "MANUAL":
      return "Thủ công";
    case null:
    case undefined:
    case "":
      return "—";
    default:
      return method;
  }
}

export function attendanceStatusLabel(status: string): string {
  switch (status) {
    case "ON_TIME":
      return "Đúng giờ";
    case "LATE":
      return "Đi muộn";
    case "LEFT_EARLY":
      return "Về sớm";
    case "LATE_AND_LEFT_EARLY":
      return "Đi muộn và về sớm";
    case "ABSENT":
      return "Vắng";
    default:
      return status;
  }
}

export type StatusTone = "success" | "warning" | "error" | "neutral";

export function attendanceStatusTone(status: string): StatusTone {
  switch (status) {
    case "ON_TIME":
      return "success";
    case "LATE":
    case "LEFT_EARLY":
    case "LATE_AND_LEFT_EARLY":
      return "warning";
    case "ABSENT":
      return "error";
    default:
      return "neutral";
  }
}
