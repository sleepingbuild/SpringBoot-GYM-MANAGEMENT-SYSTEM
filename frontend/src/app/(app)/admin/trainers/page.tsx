import { RequireRole } from "@/components/layout/RequireRole";
import { AdminScheduleWorkspace } from "@/features/schedule/AdminScheduleWorkspace";

export default function Page() {
  return (
    <RequireRole roles={["SUPER_ADMIN"]}>
      <AdminScheduleWorkspace />
    </RequireRole>
  );
}
