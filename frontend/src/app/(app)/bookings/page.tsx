import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Đặt lịch PT"
      owner="Agent 3"
      screens={[
        "Chọn huấn luyện viên và xem khung giờ trống",
        "Đặt, xác nhận, huỷ buổi tập",
        "Danh sách buổi của tôi",
      ]}
    />
  );
}
