import { RequireRole } from "@/components/layout/RequireRole";
import { LeadsPage } from "@/features/leads/LeadsPage";

export default function Page() {
  return (
    <RequireRole roles={["SALES", "SUPER_ADMIN"]}>
      <LeadsPage />
    </RequireRole>
  );
}
