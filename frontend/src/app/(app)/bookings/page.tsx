import { RequireRole } from "@/components/layout/RequireRole";
import { BookingsWorkspace } from "@/features/bookings/BookingsWorkspace";

export default function BookingsPage() {
  return (
    <RequireRole roles={["MEMBER", "RECEPTIONIST", "SUPER_ADMIN"]}>
      <BookingsWorkspace />
    </RequireRole>
  );
}
