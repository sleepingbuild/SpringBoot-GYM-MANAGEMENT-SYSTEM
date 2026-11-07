"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { monthRangeError } from "./monthRange";
import type { MonthRange } from "./types";

/** Chọn khoảng tháng. Chỉ gọi onApply khi hợp lệ. */
export function MonthRangeBar({ applied, onApply }: { applied: MonthRange; onApply: (range: MonthRange) => void }) {
  const [draft, setDraft] = useState<MonthRange>(applied);
  const [error, setError] = useState<string | null>(null);

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const problem = monthRangeError(draft);
    setError(problem);
    if (!problem) onApply(draft);
  }

  return (
    <form onSubmit={submit} className="panel mb-4 flex flex-wrap items-start gap-3 p-4" noValidate>
      <Field label="Từ tháng" type="month" placeholder="yyyy-MM" value={draft.from} onChange={(e) => setDraft((d) => ({ ...d, from: e.target.value }))} />
      <Field label="Đến tháng" type="month" placeholder="yyyy-MM" value={draft.to} onChange={(e) => setDraft((d) => ({ ...d, to: e.target.value }))} />
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
