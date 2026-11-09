/** Khop voi TrainerScheduleResponse cua backend. Gio la LocalTime (HH:mm:ss), ngay la yyyy-MM-dd, gio Viet Nam, khong co mui gio. */
export interface TrainerSchedule {
  id: string;
  trainerId: string;
  trainerName: string | null;
  workDate: string;
  startTime: string;
  endTime: string;
  active: boolean;
}

/**
 * Khop voi TrainerScheduleRequest.
 * PT tu tao cho minh thi khong gui trainerId. Super Admin tao ho thi bat buoc gui trainerId cua PT do.
 */
export interface TrainerScheduleInput {
  trainerId?: string;
  workDate: string;
  startTime: string;
  endTime: string;
  active?: boolean;
}

/** Khop voi TrainerResponse (GET /api/v1/trainers). */
export interface TrainerOption {
  id: string;
  fullName: string | null;
  avatarUrl: string | null;
}
