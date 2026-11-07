/** UserSummaryResponse.java (GET /api/v1/users?role=MEMBER). */
export interface MemberSummary {
  id: string;
  fullName: string;
  phone: string | null;
  email: string | null;
}

/**
 * FaceProfileResponse.java. Không có descriptor và không có updatedAt:
 * createdAt là lần đăng ký đầu tiên, ghi đè hồ sơ không đổi mốc này. referenceImage luôn null ở v1.
 */
export interface FaceProfileInfo {
  id: string;
  userId: string;
  userFullName: string | null;
  referenceImage: string | null;
  registeredBy: string | null;
  createdAt: string | null;
}
