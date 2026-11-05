/** FaceProfileStatusResponse.java (GET /api/v1/face-profiles/me). Không chứa descriptor. */
export interface FaceProfileStatus {
  registered: boolean;
  updatedAt: string | null;
}

/**
 * FaceAttendanceResponse.java. `action` là CHECK_IN / CHECK_OUT / NONE; `targetType` là
 * BOOKING / STAFF_ATTENDANCE / GYM_CHECK_IN hoặc null. Các trường sau `duplicateScan` chỉ có ở lượt quét của PT.
 */
export interface FaceAttendanceResult {
  matchedUserId: string;
  matchedUserName: string;
  action: string;
  targetType: string | null;
  scanTime: string;
  bookingLinked: boolean;
  duplicateScan: boolean;
  bookingId?: string | null;
  earlyLeave?: boolean;
  reasonRequired?: boolean;
  message?: string | null;
}
