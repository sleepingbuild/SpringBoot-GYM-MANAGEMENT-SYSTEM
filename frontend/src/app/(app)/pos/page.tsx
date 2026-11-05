import { RequireRole } from "@/components/layout/RequireRole";
import { PosWorkspace } from "@/features/pos/PosWorkspace";

export default function PosPage() {
  return (
    <RequireRole roles={["RECEPTIONIST", "SUPER_ADMIN"]}>
      <PosWorkspace />
    </RequireRole>
  );
}
