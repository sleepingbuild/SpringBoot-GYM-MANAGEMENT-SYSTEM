import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Lịch dạy"
      owner="Agent 3"
      screens={[
        "Khai lịch làm việc",
        "Các buổi dạy sắp tới",
        "Hoàn thành buổi, nộp lý do rời buổi sớm",
      ]}
    />
  );
}
