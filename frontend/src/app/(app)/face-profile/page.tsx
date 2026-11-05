import { RequireRole } from "@/components/layout/RequireRole";
import { PageHeader } from "@/components/ui/PageHeader";
import { FaceProfileManager } from "@/features/face/FaceProfileManager";

export default function Page() {
  return (
    <div>
      <PageHeader
        title="Đăng ký khuôn mặt"
        description="Đăng ký khuôn mặt một lần để điểm danh nhanh bằng camera."
      />
      <RequireRole roles={["MEMBER", "TRAINER", "RECEPTIONIST", "SALES"]}>
        <FaceProfileManager />
      </RequireRole>
    </div>
  );
}
