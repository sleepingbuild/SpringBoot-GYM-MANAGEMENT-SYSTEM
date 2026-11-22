import { Badge } from "@/components/ui/Badge";
import type { Tone } from "./labels";

/** Giữ nguyên chữ ký cũ để các màn hình trong module không phải sửa; phần hiển thị dùng Badge dùng chung. */
export function StatusBadge({ tone, children }: { tone: Tone; children: React.ReactNode }) {
  return <Badge tone={tone}>{children}</Badge>;
}
