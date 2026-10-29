/**
 * CheckInHistoryItem.java (Agent 4). Hợp của hai nguồn loại trừ nhau:
 * GYM_VISIT = lượt vào phòng không có PT (gym_check_ins), PT_SESSION = buổi có PT (bookings).
 * Thời gian là LocalDateTime giờ Việt Nam, không có múi giờ trong chuỗi.
 */
export type CheckInType = "GYM_VISIT" | "PT_SESSION";

export interface CheckInHistoryItem {
  type: CheckInType;
  id: string;
  checkInTime: string;
  checkOutTime: string | null;
  method: string | null;
  trainerName: string | null;
}
