import { RequireRole } from "@/components/layout/RequireRole";
import { AdminPackages } from "@/features/membership/AdminPackages";

export default function Page() {
  return (
    <RequireRole roles={["SUPER_ADMIN"]}>
      <AdminPackages />
    </RequireRole>
  );
}
