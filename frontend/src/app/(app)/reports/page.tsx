import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Báo cáo"
      owner="Agent 6"
      screens={[
        "Doanh thu",
        "Tỷ lệ gia hạn",
        "Lưu lượng buổi tập",
        "Gói bán chạy",
        "Lương PT theo tháng",
      ]}
    />
  );
}
