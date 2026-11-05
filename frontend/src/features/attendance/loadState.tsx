"use client";

import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { ApiError, errorMessage } from "@/lib/api/errors";

export type LoadState<T> =
  | { status: "loading" }
  | { status: "ready"; data: T }
  | { status: "forbidden" }
  | { status: "error"; message: string };

/** Đổi lỗi từ apiFetch thành trạng thái hiển thị. Trả null khi yêu cầu bị huỷ (không cần cập nhật giao diện). */
export function stateFromError(e: unknown): LoadState<never> | null {
  if (e instanceof DOMException && e.name === "AbortError") return null;
  if (e instanceof ApiError && e.kind === "forbidden") return { status: "forbidden" };
  return { status: "error", message: errorMessage(e) };
}

interface GateProps<T> {
  state: LoadState<T>;
  onRetry: () => void;
  loadingText: string;
  forbiddenText: string;
  errorTitle: string;
  children: (data: T) => React.ReactNode;
}

/** Vẽ ba trạng thái đang tải, không có quyền, lỗi API; trạng thái sẵn sàng giao cho `children`. */
export function LoadGate<T>({ state, onRetry, loadingText, forbiddenText, errorTitle, children }: GateProps<T>) {
  if (state.status === "loading") {
    return (
      <div className="flex items-center gap-2 py-10 text-sm text-steel" role="status">
        <Spinner />
        {loadingText}
      </div>
    );
  }
  if (state.status === "forbidden") {
    return (
      <Alert tone="warning" title="Bạn không có quyền xem trang này">
        {forbiddenText}
      </Alert>
    );
  }
  if (state.status === "error") {
    return (
      <div className="space-y-3">
        <Alert tone="error" title={errorTitle}>
          {state.message}
        </Alert>
        <Button variant="secondary" onClick={onRetry}>
          Thử lại
        </Button>
      </div>
    );
  }
  return <>{children(state.data)}</>;
}
