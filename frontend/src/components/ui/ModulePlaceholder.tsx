import { PageHeader } from "./PageHeader";

interface Props {
  title: string;
  /** Agent phụ trách module này. */
  owner: string;
  /** Các màn hình dự kiến (theo kế hoạch frontend của chủ module). */
  screens: string[];
}

/**
 * Trang giữ chỗ do Agent 1 tạo để menu và phân quyền chạy được ngay.
 * Chủ module THAY toàn bộ nội dung file page.tsx này khi làm màn hình thật,
 * rồi đổi `status` của mục tương ứng trong src/config/navigation.ts thành "ready".
 */
export function ModulePlaceholder({ title, owner, screens }: Props) {
  return (
    <div>
      <PageHeader title={title} description={`Module của ${owner}. Chưa có giao diện; các màn hình dự kiến bên dưới.`} />
      <div className="panel p-5">
        <ul className="list-disc space-y-1 pl-5 text-sm">
          {screens.map((s) => (
            <li key={s}>{s}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}
