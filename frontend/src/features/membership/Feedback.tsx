"use client";

import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { membershipErrorMessage } from "./messages";

export function LoadingBlock({ text = "Đang tải…" }: { text?: string }) {
  return (
    <div className="flex items-center gap-2 py-8 text-sm text-steel" role="status">
      <Spinner />
      <span>{text}</span>
    </div>
  );
}

export function ErrorBlock({ error, onRetry, title = "Không tải được dữ liệu" }: { error: unknown; onRetry?: () => void; title?: string }) {
  return (
    <div className="space-y-3">
      <Alert tone="error" title={title}>
        {membershipErrorMessage(error)}
      </Alert>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Thử lại
        </Button>
      )}
    </div>
  );
}
