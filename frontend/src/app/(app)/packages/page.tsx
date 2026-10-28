import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Gói tập"
      owner="Agent 2"
      screens={[
        "Danh sách gói tập",
        "Chi tiết gói",
        "Đăng ký gói (hội viên)",
      ]}
    />
  );
}
