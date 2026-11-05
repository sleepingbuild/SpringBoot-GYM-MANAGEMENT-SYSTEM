import { RequireRole } from "@/components/layout/RequireRole";
import { CommissionsWorkspace } from "@/features/commissions/CommissionsWorkspace";

export default function CommissionsPage() {
  return (
    <RequireRole roles={["TRAINER", "SUPER_ADMIN"]}>
      <CommissionsWorkspace />
    </RequireRole>
  );
}
