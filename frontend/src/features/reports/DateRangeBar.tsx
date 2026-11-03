"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { rangeError } from "./dateRange";
import type { DateRange } from "./types";

/** Chọn khoảng ngày. Chỉ gọi onApply khi hợp lệ; extra là các ô riêng của từng báo cáo. */
export function DateRangeBar({
  applied,
  onApply,
  children,
}: {
  applied: DateRange;
  onApply: (range: DateRange) => void;
  children?: React.ReactNode;
}) {
  const [draft, setDraft] = useState<DateRange>(applied);
  const [error, setError] = useState<string | null>(null);

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const problem = rangeError(draft);
    setError(problem);
    if (!problem) onApply(draft);
  }

  return (
    <form onSubmit={submit} className="panel mb-4 flex flex-wrap items-start gap-3 p-4" noValidate>
      <Field label="Từ ngày" type="date" value={draft.from} onChange={(e) => setDraft((d) => ({ ...d, from: e.target.value }))} />
      <Field label="Đến ngày" type="date" value={draft.to} onChange={(e) => setDraft((d) => ({ ...d, to: e.target.value }))} />
      {children}
      <div className="self-end">
        <Button type="submit">Xem báo cáo</Button>
      </div>
      {error && (
        <p role="alert" className="w-full text-xs text-rubber">
          {error}
        </p>
      )}
    </form>
  );
}
