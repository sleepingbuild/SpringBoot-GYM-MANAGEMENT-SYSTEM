import { RequireRole } from "@/components/layout/RequireRole";
import { PackageList } from "@/features/membership/PackageList";

export default function Page() {
  return (
    <RequireRole roles={["MEMBER", "RECEPTIONIST", "SUPER_ADMIN"]}>
      <PackageList />
    </RequireRole>
  );
}
