import { RequireRole } from "@/components/layout/RequireRole";
import { PageHeader } from "@/components/ui/PageHeader";
import { FaceEnroll } from "@/features/face/FaceEnroll";

export default function Page() {
  return (
    <div>
      <PageHeader
        title="Đăng ký khuôn mặt hộ"
        description="Tìm hội viên rồi đăng ký, xem hoặc xóa hồ sơ khuôn mặt của họ bằng ảnh hoặc camera."
      />
      <RequireRole roles={["RECEPTIONIST", "SUPER_ADMIN"]}>
        <FaceEnroll />
      </RequireRole>
    </div>
  );
}
