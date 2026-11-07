import { RequireRole } from "@/components/layout/RequireRole";
import { PageHeader } from "@/components/ui/PageHeader";
import { AttendanceView } from "@/features/attendance/AttendanceView";

export default function Page() {
  return (
    <div>
      <PageHeader
        title="Chấm công"
        description="Lễ tân và nhân viên kinh doanh xem giờ vào/ra của mình. Quản trị viên xem báo cáo theo ngày của toàn bộ nhân sự."
      />
      <RequireRole roles={["RECEPTIONIST", "SALES", "SUPER_ADMIN"]}>
        <AttendanceView />
      </RequireRole>
    </div>
  );
}
