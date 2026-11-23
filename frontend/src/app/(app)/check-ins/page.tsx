import { RequireRole } from "@/components/layout/RequireRole";
import { PageHeader } from "@/components/ui/PageHeader";
import { CheckInHistory } from "@/features/attendance/CheckInHistory";

export default function Page() {
  return (
    <div>
      <PageHeader
        title="Lịch sử check-in"
        description="Các lượt vào phòng tập và buổi tập với PT của bạn, mới nhất trước."
      />
      <RequireRole roles={["MEMBER"]}>
        <CheckInHistory />
      </RequireRole>
    </div>
  );
}
