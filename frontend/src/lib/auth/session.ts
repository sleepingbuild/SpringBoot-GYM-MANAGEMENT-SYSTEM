import { API_BASE_URL } from "../config";
import type { ApiResponse, AuthResponse, SessionUser } from "../types";

/**
 * Kho phiên đăng nhập dùng chung (ngoài React để bộ gọi API dùng được).
 * - accessToken: chỉ giữ trong bộ nhớ (mất khi tải lại trang, sẽ được lấy lại bằng refreshToken).
 * - refreshToken: lưu localStorage để giữ đăng nhập sau khi tải lại trang (chấp nhận được cho bản học tập).
 * Không bao giờ ghi log token.
 */
const REFRESH_KEY = "gms.refreshToken";

type Listener = () => void;

let accessToken: string | null = null;
let user: SessionUser | null = null;
let refreshInFlight: Promise<boolean> | null = null;
const changeListeners = new Set<Listener>();
const expiredListeners = new Set<Listener>();

function storage(): Storage | null {
  try {
    return (globalThis as { localStorage?: Storage }).localStorage ?? null;
  } catch {
    return null;
  }
}

function notifyChange() {
  changeListeners.forEach((l) => l());
}

export const session = {
  getAccessToken: () => accessToken,
  getUser: () => user,
  getRefreshToken: () => storage()?.getItem(REFRESH_KEY) ?? null,

  /** Lưu kết quả đăng nhập/đăng ký/làm mới. */
  set(auth: AuthResponse) {
    accessToken = auth.accessToken;
    user = { email: auth.email, fullName: auth.fullName, roles: auth.roles };
    storage()?.setItem(REFRESH_KEY, auth.refreshToken);
    notifyChange();
  },

  /** Xoá phiên (đăng xuất chủ động). */
  clear() {
    accessToken = null;
    user = null;
    storage()?.removeItem(REFRESH_KEY);
    notifyChange();
  },

  /** Phiên hết hạn ngoài ý muốn: xoá và báo cho giao diện chuyển về trang đăng nhập. */
  expire() {
    session.clear();
    expiredListeners.forEach((l) => l());
  },

  onChange(l: Listener) {
    changeListeners.add(l);
    return () => changeListeners.delete(l);
  },
  onExpired(l: Listener) {
    expiredListeners.add(l);
    return () => expiredListeners.delete(l);
  },

  /**
   * Làm mới token bằng refreshToken. Nhiều request cùng gặp lỗi phiên chỉ gọi một lần (single-flight).
   * Trả true nếu có phiên mới. Giả định endpoint: POST /api/v1/auth/refresh với body { refreshToken }.
   */
  refresh(): Promise<boolean> {
    if (refreshInFlight) return refreshInFlight;
    const token = session.getRefreshToken();
    if (!token) return Promise.resolve(false);

    refreshInFlight = (async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: token }),
        });
        if (!res.ok) return false;
        const body = (await res.json()) as ApiResponse<AuthResponse>;
        if (!body.success || !body.data) return false;
        session.set(body.data);
        return true;
      } catch {
        return false;
      } finally {
        refreshInFlight = null;
      }
    })();
    return refreshInFlight;
  },

  /** Chỉ dùng trong test. */
  _resetForTests() {
    accessToken = null;
    user = null;
    refreshInFlight = null;
    changeListeners.clear();
    expiredListeners.clear();
  },
};
