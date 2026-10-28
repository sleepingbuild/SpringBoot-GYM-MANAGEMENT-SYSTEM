import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Đăng ký khuôn mặt"
      owner="Agent 4"
      screens={[
        "Đăng ký khuôn mặt của tôi",
        "Đăng ký hộ (lễ tân, quản trị viên)",
      ]}
    />
  );
}
