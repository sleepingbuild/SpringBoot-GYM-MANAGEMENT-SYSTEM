const DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/;

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
