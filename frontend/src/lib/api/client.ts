import { API_BASE_URL } from "../config";
import { session } from "../auth/session";
import type { ApiResponse } from "../types";
import { ApiError, kindFromResponse } from "./errors";

export interface ApiOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  /** Body JSON (tự stringify). Dùng `formData` cho upload. */
  body?: unknown;
  formData?: FormData;
  query?: Record<string, string | number | boolean | null | undefined>;
  /** false cho các lời gọi công khai (đăng nhập, đăng ký, xem gói công khai): không gắn token, không thử làm mới. */
  auth?: boolean;
  /**
   * true cho trang chạy lâu như kiosk điểm danh: khi phiên hỏng thì chỉ ném ApiError kind "session",
   * KHÔNG xoá phiên và KHÔNG chuyển về trang đăng nhập.
   */
  skipAuthRedirect?: boolean;
  signal?: AbortSignal;
}

const SESSION_ERROR_CODES = new Set(["AUTH_TOKEN_EXPIRED", "AUTH_TOKEN_INVALID"]);

/**
 * Quy tắc phiên (xem REPLIES_FRONTEND_PLANS.md mục 0.1) - xử lý theo errorCode, không theo mã HTTP:
 * - 401/403 KHÔNG có errorCode (Spring Security chặn ở tầng lọc) hoặc errorCode AUTH_TOKEN_*: phiên hỏng.
 * - 403 AUTH_FORBIDDEN_ROLE: thiếu quyền, không đụng tới phiên.
 * - 401 có errorCode khác (FACE_NOT_RECOGNIZED, AUTH_INVALID_CREDENTIALS): chuyển nguyên cho trang gọi.
 */
function isSessionProblem(status: number, payload: ApiResponse<unknown> | null): boolean {
  if (status !== 401 && status !== 403) return false;
  const code = payload?.errorCode;
  if (!code) return true;
  return SESSION_ERROR_CODES.has(code);
}

function buildUrl(path: string, query?: ApiOptions["query"]): string {
  const url = new URL(path.startsWith("http") ? path : `${API_BASE_URL}${path}`);
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null && v !== "") url.searchParams.set(k, String(v));
    }
  }
  return url.toString();
}

async function readPayload(res: Response): Promise<ApiResponse<unknown> | null> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as ApiResponse<unknown>;
  } catch {
    return null;
  }
}

/**
 * Gọi backend và trả về `data` đã bóc khỏi ApiResponse. Lỗi luôn là ApiError.
 *
 *   const profile = await apiFetch<Profile>("/api/v1/profile/me");
 *   await apiFetch("/api/v1/auth/login", { method: "POST", body: {...}, auth: false });
 */
export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { method = "GET", body, formData, query, auth = true, skipAuthRedirect = false, signal } = options;
  const url = buildUrl(path, query);

  const send = async (): Promise<{ res: Response; payload: ApiResponse<unknown> | null }> => {
    const headers: Record<string, string> = { Accept: "application/json" };
    let requestBody: BodyInit | undefined;
    if (formData) {
      requestBody = formData; // trình duyệt tự đặt Content-Type multipart
    } else if (body !== undefined) {
      headers["Content-Type"] = "application/json";
      requestBody = JSON.stringify(body);
    }
    const token = session.getAccessToken();
    if (auth && token) headers.Authorization = `Bearer ${token}`;

    let res: Response;
    try {
      res = await fetch(url, { method, headers, body: requestBody, signal });
    } catch (e) {
      if (e instanceof DOMException && e.name === "AbortError") throw e;
      throw new ApiError("network", 0, "Không kết nối được máy chủ.");
    }
    return { res, payload: await readPayload(res) };
  };

  let { res, payload } = await send();

  if (auth && isSessionProblem(res.status, payload)) {
    const refreshed = await session.refresh();
    if (refreshed) {
      ({ res, payload } = await send());
    }
    if (!refreshed || isSessionProblem(res.status, payload)) {
      if (!skipAuthRedirect) session.expire();
      throw new ApiError("session", res.status, "Phiên đăng nhập đã hết hạn.", payload?.errorCode);
    }
  }

  if (!res.ok || payload?.success === false) {
    const code = payload?.errorCode;
    throw new ApiError(kindFromResponse(res.status, code), res.status, payload?.message ?? "", code);
  }

  return (payload?.data ?? undefined) as T;
}
