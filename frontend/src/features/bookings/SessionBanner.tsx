import { Alert } from "@/components/ui/Alert";
import type { SessionSummary } from "./sessionRules";

/** Dong tom tat so buoi con lai o dau man hinh dat lich cua hoi vien. */
export function SessionBanner({ summary }: { summary: SessionSummary }) {
  if (summary.blocked) {
    return (
      <div className="mb-6 max-w-md">
        <Alert tone="error">{summary.text}</Alert>
      </div>
    );
  }
  return <p className="panel mb-6 max-w-md p-4 text-sm">{summary.text}</p>;
}
