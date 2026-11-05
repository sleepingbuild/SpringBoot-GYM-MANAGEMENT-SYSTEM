import { RequireRole } from "@/components/layout/RequireRole";
import { LeadsHome } from "@/features/leads/LeadsHome";

export default function Page() {
  return (
    <RequireRole roles={["SALES", "SUPER_ADMIN"]}>
      <LeadsHome />
    </RequireRole>
  );
}
