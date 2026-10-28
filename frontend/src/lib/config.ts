/** Địa chỉ backend. Đặt qua NEXT_PUBLIC_API_BASE_URL (xem .env.example). Không ghi cứng URL ở nơi khác. */
export const API_BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081").replace(/\/+$/, "");
