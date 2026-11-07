"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { errorMessage } from "@/lib/api/errors";
import { submitEarlyLeaveReason } from "./api";

/** PT nhập lý do khi quét ra trước giờ kết thúc buổi (có lý do thì buổi vẫn tính là dạy đủ). */
export function EarlyLeaveForm({ bookingId, onDone }: { bookingId: string; onDone: () => void }) {
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = reason.trim();
    if (trimmed.length < 5) {
      setError("Lý do cần từ 5 đến 255 ký tự.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await submitEarlyLeaveReason(bookingId, trimmed);
      onDone();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <label htmlFor="early-leave-reason" className="block text-sm font-semibold">
        Lý do rời buổi sớm
      </label>
      <textarea
        id="early-leave-reason"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        maxLength={255}
        rows={3}
        className="w-full rounded-control border px-3 py-2 text-sm"
      />
      {error && <Alert tone="error">{error}</Alert>}
      <Button type="submit" loading={submitting}>
        Gửi lý do
      </Button>
    </form>
  );
}
