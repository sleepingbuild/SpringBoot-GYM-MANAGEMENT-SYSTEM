import { RequireRole } from "@/components/layout/RequireRole";
import { PackageDetail } from "@/features/membership/PackageDetail";

export default function Page({ params }: { params: { id: string } }) {
  return (
    <RequireRole roles={["MEMBER", "RECEPTIONIST", "SUPER_ADMIN"]}>
      <PackageDetail id={params.id} />
    </RequireRole>
  );
}
