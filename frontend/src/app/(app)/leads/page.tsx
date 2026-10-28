import { ModulePlaceholder } from "@/components/ui/ModulePlaceholder";

export default function Page() {
  return (
    <ModulePlaceholder
      title="Khách tiềm năng"
      owner="Agent 6"
      screens={[
        "Danh sách và tìm kiếm khách tiềm năng",
        "Tạo, sửa, đổi trạng thái",
        "Chuyển thành hội viên",
        "Hội viên sắp hết hạn",
      ]}
    />
  );
}
