import { RequireRole } from "@/components/layout/RequireRole";
import { MyMembership } from "@/features/membership/MyMembership";

export default function Page() {
  return (
    <RequireRole roles={["MEMBER"]}>
      <MyMembership />
    </RequireRole>
  );
}
