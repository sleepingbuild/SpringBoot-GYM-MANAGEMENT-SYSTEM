import { RequireRole } from "@/components/layout/RequireRole";
import { PosEntry } from "@/features/pos/PosEntry";

export default function PosPage() {
  return (
    <RequireRole roles={["RECEPTIONIST", "SUPER_ADMIN", "MEMBER"]}>
      <PosEntry />
    </RequireRole>
  );
}
