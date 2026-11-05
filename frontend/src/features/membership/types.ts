/** Kiểu dữ liệu của module gói tập và thanh toán (Agent 2), khớp với các DTO phía backend. */

export type PackageType = "TIME_BASED" | "SESSION_BASED" | "PT_1ON1";
export type PeakType = "OFF_PEAK" | "FULL_TIME";

/** PackageResponse.java */
export interface PackageItem {
  id: string;
  name: string;
  description: string | null;
  price: number;
  durationDays: number | null;
  sessionCount: number | null;
  packageType: PackageType;
  peakType: PeakType;
  maxSessionsPerWeek: number | null;
  active: boolean;
}

/** PackageRequest.java (tạo, sửa gói) */
export interface PackageRequestBody {
  name: string;
  description: string | null;
  price: number;
  durationDays: number | null;
  sessionCount: number | null;
  packageType: PackageType;
  peakType: PeakType;
  maxSessionsPerWeek: number | null;
  active: boolean;
}

/** BranchResponse.java (Agent 1, GET /api/v1/branches). */
export interface Branch {
  id: string;
  name: string;
}

/** MemberPackageResponse.java. status: PENDING, ACTIVE, EXPIRED, CANCELLED, SCHEDULED. */
export interface MemberPackage {
  id: string;
  userId: string;
  userFullName: string | null;
  packageId: string;
  packageName: string;
  branchId: string | null;
  status: string;
  startDate: string; // yyyy-MM-dd
  endDate: string | null;
  remainingSessions: number | null; // null = không giới hạn buổi
}

/** PaymentTransactionResponse.java. status: PENDING, SUCCESS, FAILED, REFUNDED. */
export interface PaymentTransaction {
  id: string;
  userId: string | null; // null: khách vãng lai của POS (sau V7)
  userFullName?: string | null; // có sau H3
  relatedType: string; // MEMBERSHIP hoặc POS
  relatedId: string;
  amount: number;
  paymentMethod: string;
  status: string;
  transactionCode: string;
  createdAt: string; // LocalDateTime giờ Việt Nam, không có múi giờ
}

/** PackagePurchaseResponse.java: kết quả đăng ký, nâng cấp, hạ cấp. */
export interface PackagePurchase {
  memberPackage: MemberPackage;
  paymentTransaction: PaymentTransaction;
}

/** Kết quả tìm hội viên (GET /api/v1/users?role=MEMBER&q=, do Agent 1 làm). */
export interface MemberSummary {
  id: string;
  fullName: string;
  phone: string | null;
  email: string;
}
