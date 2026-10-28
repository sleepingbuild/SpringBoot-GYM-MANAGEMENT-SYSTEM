import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Gói của tôi"
      owner="Agent 2"
      screens={[
        "Gói đang dùng và số buổi còn lại",
        "Nâng cấp / hạ cấp gói",
        "Lịch sử gói",
        "Lịch sử thanh toán",
      ]}
    />
  );
}
