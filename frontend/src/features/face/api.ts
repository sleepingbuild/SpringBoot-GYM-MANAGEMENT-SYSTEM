import { apiFetch } from "@/lib/api/client";
import type { FaceAttendanceResult, FaceProfileStatus } from "./types";

/** POST /api/v1/face-profiles/me: tự đăng ký (ghi đè hồ sơ cũ). Chỉ gửi descriptor 128 số, không gửi ảnh. */
export const registerMyFace = (descriptor: number[]) =>
  apiFetch<unknown>("/api/v1/face-profiles/me", { method: "POST", body: { descriptor } });

/** GET /api/v1/face-profiles/me: trạng thái đăng ký của chính mình. */
export const getMyFaceStatus = (signal?: AbortSignal) =>
  apiFetch<FaceProfileStatus>("/api/v1/face-profiles/me", { signal });

/** POST /api/v1/face-attendance/self: tự điểm danh (so khớp 1:1 với tài khoản đang đăng nhập). */
export const scanSelf = (descriptor: number[]) =>
  apiFetch<FaceAttendanceResult>("/api/v1/face-attendance/self", { method: "POST", body: { descriptor } });

/**
 * POST /api/v1/face-attendance/kiosk: điểm danh tại quầy (so khớp 1:N).
 * skipAuthRedirect: trang kiosk chạy lâu, phiên hỏng chỉ báo lỗi chứ không đẩy ra trang đăng nhập.
 */
export const scanKiosk = (descriptor: number[], signal?: AbortSignal) =>
  apiFetch<FaceAttendanceResult>("/api/v1/face-attendance/kiosk", {
    method: "POST",
    body: { descriptor },
    skipAuthRedirect: true,
    signal,
  });

/** PUT /api/v1/bookings/{id}/early-leave-reason (Agent 3): PT nộp lý do rời buổi sớm. */
export const submitEarlyLeaveReason = (bookingId: string, reason: string) =>
  apiFetch<unknown>(`/api/v1/bookings/${bookingId}/early-leave-reason`, { method: "PUT", body: { reason } });
