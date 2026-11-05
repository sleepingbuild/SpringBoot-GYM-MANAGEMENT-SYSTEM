import { RequireRole } from "@/components/layout/RequireRole";
import { PageHeader } from "@/components/ui/PageHeader";
import { KioskScanner } from "@/features/face/KioskScanner";

export default function Page() {
  return (
    <div>
      <PageHeader
        title="Kiosk điểm danh"
        description="Đặt camera hướng về cửa. Hệ thống tự nhận diện hội viên, huấn luyện viên và nhân viên."
      />
      <RequireRole roles={["RECEPTIONIST", "SUPER_ADMIN"]}>
        <KioskScanner />
      </RequireRole>
    </div>
  );
}
