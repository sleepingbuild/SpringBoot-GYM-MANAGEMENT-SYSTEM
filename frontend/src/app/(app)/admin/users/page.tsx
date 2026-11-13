import { RequireRole } from "@/components/layout/RequireRole";
import { AdminUsersWorkspace } from "@/features/admin-users/AdminUsersWorkspace";

export default function Page() {
  return (
    <RequireRole roles={["SUPER_ADMIN"]}>
      <AdminUsersWorkspace />
    </RequireRole>
  );
}
