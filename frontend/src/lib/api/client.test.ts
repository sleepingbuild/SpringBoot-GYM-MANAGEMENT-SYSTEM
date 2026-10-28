import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { session } from "../auth/session";
import { apiFetch } from "./client";
import { ApiError } from "./errors";

function memoryStorage(): Storage {
  const m = new Map<string, string>();
  return {
    getItem: (k) => m.get(k) ?? null,
    setItem: (k, v) => void m.set(k, v),
    removeItem: (k) => void m.delete(k),
    clear: () => m.clear(),
    key: (i) => Array.from(m.keys())[i] ?? null,
    get length() {
      return m.size;
    },
  };
}

function json(status: number, body: unknown): Response {
  return new Response(body === null ? "" : JSON.stringify(body), { status });
}

const ok = (data: unknown) => json(200, { success: true, message: "ok", data });
const authData = {
  accessToken: "access-2",
  refreshToken: "refresh-2",
  email: "a@gym.test",
  fullName: "A",
  roles: ["MEMBER"],
};

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  vi.stubGlobal("localStorage", memoryStorage());
  session._resetForTests();
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

function signIn() {
  session.set({ ...authData, accessToken: "access-1", refreshToken: "refresh-1" } as never);
}

describe("apiFetch", () => {
  it("bóc data khỏi ApiResponse và gắn Authorization", async () => {
    signIn();
    fetchMock.mockResolvedValueOnce(ok({ hello: "world" }));

    const data = await apiFetch<{ hello: string }>("/api/v1/profile/me");

    expect(data).toEqual({ hello: "world" });
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>).Authorization).toBe("Bearer access-1");
  });

  it("auth:false không gắn token và không thử làm mới khi 401 có errorCode", async () => {
    signIn();
    fetchMock.mockResolvedValueOnce(
      json(401, { success: false, message: "Email hoặc mật khẩu không đúng", errorCode: "AUTH_INVALID_CREDENTIALS" }),
    );

    await expect(apiFetch("/api/v1/auth/login", { method: "POST", body: {}, auth: false })).rejects.toMatchObject({
      errorCode: "AUTH_INVALID_CREDENTIALS",
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it("403 không có errorCode: làm mới token rồi gửi lại và thành công", async () => {
    signIn();
    fetchMock
      .mockResolvedValueOnce(json(403, null)) //                       request gốc bị chặn ở tầng lọc
      .mockResolvedValueOnce(ok(authData)) //                           POST /auth/refresh
      .mockResolvedValueOnce(ok({ done: true })); //                    gửi lại

    const data = await apiFetch<{ done: boolean }>("/api/v1/profile/me");

    expect(data).toEqual({ done: true });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    const retry = fetchMock.mock.calls[2][1] as RequestInit;
    expect((retry.headers as Record<string, string>).Authorization).toBe("Bearer access-2");
  });

  it("làm mới token gửi refreshToken dạng form (backend dùng @RequestParam), không phải JSON, không đặt token trên URL", async () => {
    signIn();
    fetchMock
      .mockResolvedValueOnce(json(403, null))
      .mockResolvedValueOnce(ok(authData))
      .mockResolvedValueOnce(ok({ done: true }));

    await apiFetch("/api/v1/profile/me");

    const [refreshUrl, refreshInit] = fetchMock.mock.calls[1] as [string, RequestInit];
    expect(refreshUrl.endsWith("/api/v1/auth/refresh")).toBe(true);
    expect(refreshInit.method).toBe("POST");
    expect((refreshInit.headers as Record<string, string>)["Content-Type"]).toBe("application/x-www-form-urlencoded");
    expect(refreshInit.body).toBe("refreshToken=refresh-1");
  });

  it("403 không có errorCode và làm mới thất bại: kind session, phiên bị xoá, báo hết hạn", async () => {
    signIn();
    const expired = vi.fn();
    session.onExpired(expired);
    fetchMock.mockResolvedValueOnce(json(403, null)).mockResolvedValueOnce(json(401, null));

    await expect(apiFetch("/api/v1/profile/me")).rejects.toMatchObject({ kind: "session" });
    expect(expired).toHaveBeenCalledTimes(1);
    expect(session.getAccessToken()).toBeNull();
    expect(session.getRefreshToken()).toBeNull();
  });

  it("skipAuthRedirect: kind session nhưng KHÔNG xoá phiên và không báo hết hạn", async () => {
    signIn();
    const expired = vi.fn();
    session.onExpired(expired);
    fetchMock.mockResolvedValueOnce(json(403, null)).mockResolvedValueOnce(json(401, null));

    await expect(apiFetch("/api/v1/face-attendance/kiosk", { skipAuthRedirect: true })).rejects.toMatchObject({
      kind: "session",
    });
    expect(expired).not.toHaveBeenCalled();
    expect(session.getRefreshToken()).toBe("refresh-1");
  });

  it("401 FACE_NOT_RECOGNIZED: chuyển nguyên lỗi, KHÔNG làm mới, KHÔNG xoá phiên", async () => {
    signIn();
    const expired = vi.fn();
    session.onExpired(expired);
    fetchMock.mockResolvedValueOnce(
      json(401, { success: false, message: "Không nhận diện được khuôn mặt", errorCode: "FACE_NOT_RECOGNIZED" }),
    );

    const err = (await apiFetch("/api/v1/face-attendance/kiosk", { method: "POST", body: {} }).catch((e: unknown) => e)) as ApiError;

    expect(err).toBeInstanceOf(ApiError);
    expect(err.errorCode).toBe("FACE_NOT_RECOGNIZED");
    expect(err.kind).toBe("other");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(expired).not.toHaveBeenCalled();
    expect(session.getAccessToken()).toBe("access-1");
  });

  it("403 AUTH_FORBIDDEN_ROLE: kind forbidden, không làm mới, giữ phiên", async () => {
    signIn();
    const expired = vi.fn();
    session.onExpired(expired);
    fetchMock.mockResolvedValueOnce(
      json(403, { success: false, message: "Bạn không có quyền", errorCode: "AUTH_FORBIDDEN_ROLE" }),
    );

    await expect(apiFetch("/api/v1/reports/revenue")).rejects.toMatchObject({ kind: "forbidden" });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(expired).not.toHaveBeenCalled();
    expect(session.getAccessToken()).toBe("access-1");
  });

  it("nhiều request cùng lỗi phiên chỉ gọi làm mới một lần", async () => {
    signIn();
    let refreshCalls = 0;
    fetchMock.mockImplementation(async (url: string, init?: RequestInit) => {
      if (String(url).endsWith("/api/v1/auth/refresh")) {
        refreshCalls += 1;
        await new Promise((r) => setTimeout(r, 10));
        return ok(authData);
      }
      const bearer = (init?.headers as Record<string, string>).Authorization;
      return bearer === "Bearer access-2" ? ok({ n: 1 }) : json(403, null);
    });

    const results = await Promise.all([apiFetch("/a"), apiFetch("/b"), apiFetch("/c")]);

    expect(results).toHaveLength(3);
    expect(refreshCalls).toBe(1);
  });

  it("lỗi mạng: kind network", async () => {
    signIn();
    fetchMock.mockRejectedValueOnce(new TypeError("Failed to fetch"));

    await expect(apiFetch("/api/v1/profile/me")).rejects.toMatchObject({ kind: "network" });
  });

  it("400 VALIDATION_ERROR: kind validation kèm message từ server", async () => {
    signIn();
    fetchMock.mockResolvedValueOnce(
      json(400, { success: false, message: "Họ tên không được để trống", errorCode: "VALIDATION_ERROR" }),
    );

    await expect(apiFetch("/api/v1/profile/me", { method: "PUT", body: {} })).rejects.toMatchObject({
      kind: "validation",
      message: "Họ tên không được để trống",
    });
  });
});
