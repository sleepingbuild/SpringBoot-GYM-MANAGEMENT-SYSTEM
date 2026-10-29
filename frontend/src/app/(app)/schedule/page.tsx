import { RequireRole } from "@/components/layout/RequireRole";
import { TrainerWorkspace } from "@/features/schedule/TrainerWorkspace";

export default function SchedulePage() {
  return (
    <RequireRole roles={["TRAINER"]}>
      <TrainerWorkspace />
    </RequireRole>
  );
}
