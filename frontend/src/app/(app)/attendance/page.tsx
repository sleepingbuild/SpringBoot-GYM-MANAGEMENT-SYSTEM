import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Chấm công"
      owner="Agent 4"
      screens={[
        "Bảng chấm công của tôi",
        "Báo cáo chấm công theo ngày (quản trị viên)",
      ]}
    />
  );
}
