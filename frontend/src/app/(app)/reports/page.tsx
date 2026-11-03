import { RequireRole } from "@/components/layout/RequireRole";
import { ReportsPage } from "@/features/reports/ReportsPage";

export default function Page() {
  return (
    <RequireRole roles={["SUPER_ADMIN"]}>
      <ReportsPage />
    </RequireRole>
  );
}
