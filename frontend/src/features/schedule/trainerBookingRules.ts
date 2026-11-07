import type { Booking } from "./bookingTypes";

/**
 * PT da quet ra truoc gio ket thuc nhung chua nop ly do (backend: fullDuration === false)
 * va buoi van dang CONFIRMED nen con nop duoc ly do.
 */
export function needsEarlyLeaveReason(b: Booking): boolean {
  return b.status === "CONFIRMED" && Boolean(b.ptCheckOutTime) && b.fullDuration === false;
}

/**
 * Canh bao truoc khi bam "Hoan thanh buoi". Tra ve null neu khong can canh bao.
 * Co he thong tinh cong bo qua cac buoi chua du thoi luong va khong co ly do, va sau khi hoan thanh
 * khong con nop ly do duoc nua, nen can nhac nho PT truoc.
 */
export function completeWarning(b: Booking): string | null {
  if (!needsEarlyLeaveReason(b)) return null;
  return "Bạn đã quét ra trước giờ kết thúc và chưa nộp lý do rời sớm. Nếu hoàn thành bây giờ, buổi này sẽ không được tính công cho bạn và không thể nộp lý do sau đó. Vẫn hoàn thành buổi?";
}
