import { ApiError, errorMessage } from "../../lib/api/errors";

/** Ma loi dat lich (va goi tap) -> cau tieng Viet cho nguoi dung. Xu ly theo errorCode, khong theo status. */
export const BOOKING_ERROR_MESSAGES: Record<string, string> = {
  BOOKING_TOO_SOON: "Khung giờ này quá gần so với thời điểm hiện tại. Hãy chọn khung giờ muộn hơn.",
  BOOKING_WEEKLY_LIMIT_EXCEEDED: "Đã đạt số buổi tối đa được đặt trong tuần. Hãy chọn một ngày ở tuần khác.",
  BOOKING_SLOT_TAKEN: "Khung giờ này vừa có người khác đặt. Danh sách khung giờ đã được tải lại.",
  BOOKING_DOUBLE_BOOKED: "Hội viên đã có một buổi khác trùng khung giờ này. Hãy chọn khung giờ khác.",
  BOOKING_OUT_OF_WORKING_HOURS: "Khung giờ này nằm ngoài ca làm việc của huấn luyện viên.",
  MEMBERSHIP_EXPIRED: "Gói tập đã hết hạn nên chưa thể đặt lịch. Hãy gia hạn hoặc mua gói mới.",
  MEMBERSHIP_NO_SESSION: "Gói tập đã hết buổi nên chưa thể đặt lịch. Hãy gia hạn hoặc mua gói mới.",
};

/** Ma loi co trong bang tren thi dung cau cua bang; con lai dung errorMessage chung. */
export function bookingErrorMessage(err: unknown): string {
  if (err instanceof ApiError && err.errorCode) {
    const mapped = BOOKING_ERROR_MESSAGES[err.errorCode];
    if (mapped) return mapped;
  }
  return errorMessage(err);
}
