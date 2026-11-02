import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import type { ReportState } from "./useReport";

/** Xử lý chung ba trạng thái của một báo cáo: đang tải, lỗi (có nút thử lại), có dữ liệu. */
export function ReportBody<T>({
  state,
  onRetry,
  label,
  children,
}: {
  state: ReportState<T>;
  onRetry: () => void;
  label: string;
  children: (data: T) => React.ReactNode;
}) {
  if (state.error) {
    return (
      <Alert tone="error" title={`Không tải được ${label}`}>
        {state.error}{" "}
        <button type="button" className="underline" onClick={onRetry}>
          Thử lại
        </button>
      </Alert>
    );
  }
  if (!state.data) {
    return (
      <div className="flex justify-center py-16 text-steel" role="status" aria-label={`Đang tải ${label}`}>
        <Spinner />
      </div>
    );
  }
  return (
    <div className={state.loading ? "opacity-60" : ""} aria-busy={state.loading}>
      {children(state.data)}
    </div>
  );
}
