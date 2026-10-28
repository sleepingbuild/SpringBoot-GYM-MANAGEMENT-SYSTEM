import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Quản trị gói"
      owner="Agent 2"
      screens={[
        "Danh sách gói (kể cả đã ngừng bán)",
        "Tạo, sửa, ngừng bán gói",
      ]}
    />
  );
}
