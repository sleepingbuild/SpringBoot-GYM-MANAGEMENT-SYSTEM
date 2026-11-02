import { RequireRole } from "@/components/layout/RequireRole";
import { MemberBookings } from "@/features/bookings/MemberBookings";

export default function BookingsPage() {
  return (
    <RequireRole roles={["MEMBER"]}>
      <MemberBookings />
    </RequireRole>
  );
}
