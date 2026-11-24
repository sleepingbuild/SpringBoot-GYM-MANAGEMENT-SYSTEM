import { Alert } from "@/components/ui/Alert";
import type { SessionSummary } from "./sessionRules";

/** Dong tom tat so buoi con lai o dau man hinh dat lich cua hoi vien. */
export function SessionBanner({ summary }: { summary: SessionSummary }) {
  return (
    <div className="mb-6 max-w-md">
      <Alert tone={summary.blocked ? "error" : "info"}>{summary.text}</Alert>
    </div>
  );
}
