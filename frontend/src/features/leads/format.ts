/** createdAt là LocalDateTime giờ Việt Nam không có múi giờ: cắt chuỗi, không đổi múi giờ. */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "";
  const m = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/.exec(value);
  return m ? `${m[3]}/${m[2]}/${m[1]} ${m[4]}:${m[5]}` : value;
}

/** endDate là LocalDate yyyy-MM-dd: cắt chuỗi, không đổi múi giờ. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "";
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  return m ? `${m[3]}/${m[2]}/${m[1]}` : value;
}

export function formatDaysLeft(days: number): string {
  if (days <= 0) return "Hết hạn hôm nay";
  if (days === 1) return "Còn 1 ngày";
  return `Còn ${days} ngày`;
}
