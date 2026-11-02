const VND = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });

export const formatVnd = (amount: number): string => VND.format(amount);

/** LocalDateTime dạng yyyy-MM-ddTHH:mm:ss -> dd/MM/yyyy HH:mm. Không cộng trừ múi giờ (giờ Việt Nam sẵn). */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso || iso.length < 16) return "";
  const [y, m, d] = iso.slice(0, 10).split("-");
  return `${d}/${m}/${y} ${iso.slice(11, 16)}`;
}

const pad = (n: number) => String(n).padStart(2, "0");

/** yyyy-MM-dd theo giờ máy người dùng (dùng làm mặc định cho ô ngày). */
export function todayIso(now: Date = new Date()): string {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** yyyy-MM của tháng hiện tại, khớp ô chọn tháng và tham số month của API lương. */
export function currentMonth(now: Date = new Date()): string {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}`;
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export const isUuid = (value: string): boolean => UUID_RE.test(value.trim());

/** Rút gọn UUID để hiển thị trong bảng (8 ký tự đầu). */
export const shortId = (id: string): string => id.slice(0, 8);
