import { RequireRole } from "@/components/layout/RequireRole";
import { ScheduleManager } from "@/features/schedule/ScheduleManager";

export default function SchedulePage() {
  return (
    <RequireRole roles={["TRAINER"]}>
      <ScheduleManager />
    </RequireRole>
  );
}
