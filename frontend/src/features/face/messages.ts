import { ApiError, errorMessage } from "../../lib/api/errors";

const FACE_MESSAGES: Record<string, string> = {
  FACE_NOT_RECOGNIZED: "Không nhận diện được khuôn mặt. Nhìn thẳng vào camera, đủ sáng, rồi thử lại.",
  FACE_PROFILE_NOT_FOUND: "Chưa có hồ sơ khuôn mặt. Hãy đăng ký khuôn mặt trước.",
  FACE_DESCRIPTOR_INVALID: "Dữ liệu khuôn mặt không hợp lệ. Hãy chụp lại.",
};

/** Chữ cho lỗi của các lời gọi khuôn mặt: mã lỗi riêng của module trước, còn lại theo message của server. */
export function faceErrorMessage(err: unknown): string {
  if (err instanceof ApiError && err.errorCode && FACE_MESSAGES[err.errorCode]) {
    return FACE_MESSAGES[err.errorCode];
  }
  return errorMessage(err);
}
