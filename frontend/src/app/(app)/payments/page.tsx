import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Thanh toán"
      owner="Agent 2"
      screens={[
        "Xác nhận thanh toán đang chờ",
        "Bán gói hộ hội viên",
      ]}
    />
  );
}
