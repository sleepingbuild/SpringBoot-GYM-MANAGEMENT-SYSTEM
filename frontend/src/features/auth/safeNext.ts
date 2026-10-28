/** Chỉ cho phép quay lại đường dẫn nội bộ, tránh chuyển hướng ra trang lạ (open redirect). */
export function safeNext(next: string | null | undefined, fallback = "/dashboard"): string {
  if (!next) return fallback;
  if (!next.startsWith("/") || next.startsWith("//") || next.startsWith("/\\")) return fallback;
  if (next === "/login" || next.startsWith("/login?") || next === "/register") return fallback;
  return next;
}
