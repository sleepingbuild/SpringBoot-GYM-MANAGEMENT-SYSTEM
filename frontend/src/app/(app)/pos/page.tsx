import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Bán lẻ (POS)"
      owner="Agent 5"
      screens={[
        "Bán sản phẩm tại quầy",
        "Quản lý sản phẩm",
        "Đơn hàng",
      ]}
    />
  );
}
