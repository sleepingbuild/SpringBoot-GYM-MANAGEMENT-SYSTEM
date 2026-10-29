export type BookingStatus =
  | "PENDING"
  | "CONFIRMED"
  | "COMPLETED"
  | "CANCELLED"
  | "LATE_CANCELLED"
  | "NO_SHOW"
  | "PT_NO_SHOW";

/** Khop voi BookingResponse cua backend. Ngay yyyy-MM-dd, gio HH:mm:ss, moc quet mat la LocalDateTime gio Viet Nam (khong mui gio). */
export interface Booking {
  id: string;
  memberId: string;
  memberName: string | null;
  trainerId: string;
  trainerName: string | null;
  branchId: string | null;
  bookingDate: string;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  checkInTime: string | null;
  checkInMethod: string | null;
  checkOutTime: string | null;
  checkOutMethod: string | null;
  ptCheckInTime: string | null;
  ptCheckInMethod: string | null;
  ptCheckOutTime: string | null;
  ptCheckOutMethod: string | null;
  ptEarlyLeaveReason: string | null;
  /** null = PT chua quet ra. false = quet ra som va chua nop ly do. */
  fullDuration: boolean | null;
  notes: string | null;
}
