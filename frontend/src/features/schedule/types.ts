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

/** Khop voi TrainerScheduleRequest. PT tu tao cho minh thi khong gui trainerId. */
export interface TrainerScheduleInput {
  workDate: string;
  startTime: string;
  endTime: string;
  active?: boolean;
}
