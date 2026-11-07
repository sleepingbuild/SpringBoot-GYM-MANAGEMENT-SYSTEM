/** Khop voi AvailableTrainerResponse cua backend (GET /api/v1/trainer-schedules/trainers). */
export interface AvailableTrainer {
  trainerId: string;
  trainerName: string | null;
  availableSlotCount: number;
}

/** Khop voi AvailableSlotResponse (GET /api/v1/trainer-schedules/available). Ngay yyyy-MM-dd, gio HH:mm:ss. */
export interface AvailableSlot {
  trainerId: string;
  date: string;
  startTime: string;
  endTime: string;
}

/**
 * Khop voi BookingRequest.
 * Hoi vien tu dat: khong gui memberId. Le tan, admin dat ho: bat buoc gui memberId.
 */
export interface CreateBookingInput {
  memberId?: string;
  trainerId: string;
  branchId?: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  notes?: string;
}

/** Khop voi BranchResponse (GET /api/v1/branches). */
export interface BranchOption {
  id: string;
  name: string;
}

/** Khop voi UserSummaryResponse (GET /api/v1/users?role=MEMBER&q=). Chi le tan va admin goi duoc. */
export interface MemberOption {
  id: string;
  fullName: string | null;
  phone: string | null;
  email: string | null;
}

/** Khop voi PageResponse<T> cua backend. */
export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
