"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { formatDateTime, formatVnd } from "@/features/pos/format";
import { Pager } from "@/features/pos/Pager";
import type { PageResponse } from "@/lib/types";
import { listMyCommissions } from "./api";
import { sumCommissions } from "./summary";
import { OUTCOME_LABEL, STATUS_LABEL, type Commission } from "./types";

export function MyCommissions() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Commission> | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setData(await listMyCommissions(page, 20));
    } catch (e) {
      setError(errorMessage(e));
      setData({ content: [], page, size: 20, totalElements: 0, totalPages: 0 });
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  const totals = useMemo(() => sumCommissions(data?.content ?? []), [data]);

  return (
    <div>
      <PageHeader title="Lương theo buổi" description="Mỗi buổi dạy đã chốt được ghi một dòng lương theo mức lương mỗi buổi do quản trị viên cấu hình." />
      {error && (
        <div className="mb-4">
          <Alert tone="error" title="Không tải được lương">
            {error}
          </Alert>
        </div>
      )}
      {data === null ? (
        <div className="flex justify-center py-10 text-steel">
          <Spinner />
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState title="Chưa có dòng lương nào">
          Dòng lương xuất hiện sau khi buổi dạy được chốt. Nếu bạn đã dạy mà chưa thấy, hỏi quản trị viên đã đặt mức lương mỗi buổi cho bạn chưa.
        </EmptyState>
      ) : (
        <>
          <dl className="mb-4 grid gap-3 sm:grid-cols-3">
            <div className="panel p-4">
              <dt className="text-xs text-steel">Chờ chi trả (trang này)</dt>
              <dd className="text-lg font-semibold">{formatVnd(totals.pending)}</dd>
            </div>
            <div className="panel p-4">
              <dt className="text-xs text-steel">Đã chi trả (trang này)</dt>
              <dd className="text-lg font-semibold">{formatVnd(totals.paid)}</dd>
            </div>
            <div className="panel p-4">
              <dt className="text-xs text-steel">Số buổi (trang này)</dt>
              <dd className="text-lg font-semibold">{totals.count}</dd>
            </div>
          </dl>
          <div className="panel overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-line text-left text-steel">
                <tr>
                  <th className="px-3 py-2">Ghi nhận lúc</th>
                  <th className="px-3 py-2">Kết cục buổi</th>
                  <th className="px-3 py-2 text-right">Số tiền</th>
                  <th className="px-3 py-2">Trạng thái</th>
                  <th className="px-3 py-2">Ngày chi trả</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((c) => (
                  <tr key={c.id} className="border-b border-line last:border-0">
                    <td className="px-3 py-2">{formatDateTime(c.createdAt)}</td>
                    <td className="px-3 py-2">{OUTCOME_LABEL[c.outcome] ?? c.outcome}</td>
                    <td className="px-3 py-2 text-right">{formatVnd(c.amount)}</td>
                    <td className="px-3 py-2">{STATUS_LABEL[c.status] ?? c.status}</td>
                    <td className="px-3 py-2">{formatDateTime(c.paidAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </div>
  );
}
