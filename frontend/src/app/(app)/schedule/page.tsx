import { RequireRole } from "@/components/layout/RequireRole";
import { ScheduleWorkspace } from "@/features/schedule/ScheduleWorkspace";

export default function SchedulePage() {
  return (
    <RequireRole roles={["TRAINER", "SUPER_ADMIN"]}>
      <ScheduleWorkspace />
    </RequireRole>
  );
}
