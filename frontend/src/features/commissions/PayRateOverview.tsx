"use client";

import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/errors";
import { formatVnd } from "@/features/pos/format";
import { getPayRate } from "./api";
import { type RateRow, toRateRows } from "./rateRows";
import type { Trainer } from "./types";

/** Bảng mức lương hiện hành của mọi PT (đọc từng PT bằng GET /trainer-pay-rates/{id}, không có API danh sách). */
export function PayRateOverview({
  trainers,
  version,
  selectedId,
  onSelect,
}: {
  /** null = danh sách PT đang tải. */
  trainers: Trainer[] | null;
  /** Tăng lên sau mỗi lần lưu để tải lại bảng. */
  version: number;
  selectedId: string;
  onSelect: (trainerId: string) => void;
}) {
  const [rows, setRows] = useState<RateRow[] | null>(null);

  useEffect(() => {
    let alive = true;
    setRows(null);
    if (trainers === null) return;
    Promise.all(
      trainers.map((t) =>
        getPayRate(t.id).then(
          (r) => ({ ok: true as const, amount: r.amountPerSession }),
          (e: unknown) => (e instanceof ApiError && e.kind === "notFound" ? { ok: true as const, amount: null } : { ok: false as const }),
        ),
      ),
    ).then((results) => {
      if (alive) setRows(toRateRows(trainers, results));
    });
    return () => {
      alive = false;
    };
  }, [trainers, version]);

  return (
    <div className="panel p-5">
      <h2 className="text-base">Mức lương hiện hành của các PT</h2>
      {rows === null ? (
        <div className="mt-4 flex items-center gap-2 text-sm text-steel">
          <Spinner /> Đang tải…
        </div>
      ) : rows.length === 0 ? (
        <p className="mt-3 text-sm text-steel">Chưa có huấn luyện viên nào đang hoạt động.</p>
      ) : (
        <table className="mt-3 w-full text-sm">
          <thead className="border-b border-line text-left text-steel">
            <tr>
              <th className="px-2 py-2">Huấn luyện viên</th>
              <th className="px-2 py-2 text-right">Mỗi buổi</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.trainerId} className={`border-b border-line last:border-0 hover:bg-raised ${r.trainerId === selectedId ? "bg-plate-tint" : ""}`}>
                <td className="px-2 py-2">
                  <button type="button" className="underline" aria-pressed={r.trainerId === selectedId} onClick={() => onSelect(r.trainerId)}>
                    {r.name}
                  </button>
                </td>
                <td className="px-2 py-2 text-right">
                  {r.failed ? <Badge tone="danger">Không tải được</Badge> : r.amount === null ? <Badge tone="warning">Chưa đặt</Badge> : formatVnd(r.amount)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
