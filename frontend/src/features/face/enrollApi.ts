import { apiFetch } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { FaceProfileInfo } from "./enrollTypes";
import { parseMemberPage, type ParsedMemberPage } from "./members";

/** GET /api/v1/users?role=MEMBER&q=&page=&size= (Lễ tân, Super Admin). q rỗng thì không gửi. */
export async function searchMembers(q: string, page = 0, size = 10, signal?: AbortSignal): Promise<ParsedMemberPage> {
  const data = await apiFetch<unknown>("/api/v1/users", {
    query: { role: "MEMBER", q: q.trim(), page, size },
    signal,
  });
  return parseMemberPage(data);
}

const profilePath = (userId: string) => `/api/v1/face-profiles/${encodeURIComponent(userId)}`;

/** GET /api/v1/face-profiles/{userId}. Chưa đăng ký (FACE_PROFILE_NOT_FOUND hoặc 404) thì trả null. */
export async function getFaceProfile(userId: string, signal?: AbortSignal): Promise<FaceProfileInfo | null> {
  try {
    const data = await apiFetch<FaceProfileInfo | undefined>(profilePath(userId), { signal });
    return data ?? null;
  } catch (e) {
    if (e instanceof ApiError && (e.is("FACE_PROFILE_NOT_FOUND") || e.kind === "notFound")) return null;
    throw e;
  }
}

/** POST /api/v1/face-profiles/{userId}: đăng ký hộ, ghi đè nếu đã có. Chỉ gửi descriptor 128 số. */
export const registerFaceFor = (userId: string, descriptor: number[]) =>
  apiFetch<FaceProfileInfo>(profilePath(userId), { method: "POST", body: { descriptor } });

/** DELETE /api/v1/face-profiles/{userId}. */
export const deleteFaceProfile = (userId: string) => apiFetch<unknown>(profilePath(userId), { method: "DELETE" });

/** Thiếu quyền là cảnh báo, các lỗi khác là lỗi. */
export function errorTone(err: unknown): "warning" | "error" {
  return err instanceof ApiError && err.kind === "forbidden" ? "warning" : "error";
}
