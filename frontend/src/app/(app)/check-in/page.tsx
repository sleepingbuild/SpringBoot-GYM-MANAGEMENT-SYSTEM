import { RequireRole } from "@/components/layout/RequireRole";
import { PageHeader } from "@/components/ui/PageHeader";
import { SelfCheckIn } from "@/features/face/SelfCheckIn";

export default function Page() {
  return (
    <div>
      <PageHeader
        title="Tự điểm danh"
        description="Quét khuôn mặt để ghi nhận giờ vào hoặc giờ ra. Cần đăng ký khuôn mặt trước."
      />
      <RequireRole roles={["MEMBER", "TRAINER", "RECEPTIONIST", "SALES"]}>
        <SelfCheckIn />
      </RequireRole>
    </div>
  );
}
