import { RequireRole } from "@/components/layout/RequireRole";
import { PaymentsDesk } from "@/features/membership/PaymentsDesk";

export default function Page() {
  return (
    <RequireRole roles={["RECEPTIONIST", "SUPER_ADMIN"]}>
      <PaymentsDesk />
    </RequireRole>
  );
}
