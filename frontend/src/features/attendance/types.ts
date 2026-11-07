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

/** Trạng thái chấm công do backend tính (StaffAttendanceResponse.computedStatus). */
export type AttendanceStatus = "ON_TIME" | "LATE" | "LEFT_EARLY" | "LATE_AND_LEFT_EARLY" | "ABSENT";

/** StaffAttendanceResponse.java. `date` dạng yyyy-MM-dd; giờ là LocalDateTime giờ Việt Nam. */
export interface StaffAttendanceItem {
  id: string;
  staffId: string;
  staffName: string;
  date: string;
  checkInTime: string | null;
  checkOutTime: string | null;
  method: string | null;
  computedStatus: AttendanceStatus | string;
  notes: string | null;
}
