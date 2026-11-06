"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { errorMessage } from "@/lib/api/errors";
import { currentMonth, formatDateTime, formatVnd, shortId } from "@/features/pos/format";
import { Pager } from "@/features/pos/Pager";
import type { PageResponse } from "@/lib/types";
import { useToast } from "@/lib/toast";
import { markCommissionPaid, searchCommissions } from "./api";
import { trainerLabel } from "./trainers";
import { useTrainers } from "./useTrainers";
import { sumCommissions } from "./summary";
import { OUTCOME_LABEL, STATUS_LABEL, type Commission } from "./types";

export function AdminCommissions() {
  const toast = useToast();
  const [month, setMonth] = useState(currentMonth());
  const [staffId, setStaffId] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Commission> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const { trainers, error: trainersError } = useTrainers();

  const load = useCallback(async () => {
    setError(null);
    setData(null);
    try {
      setData(
        await searchCommissions({
          staffId: staffId === "" ? undefined : staffId,
          month: month || undefined,
          page,
          size: 20,
        }),
      );
    } catch (e) {
      setError(errorMessage(e));
      setData({ content: [], page, size: 20, totalElements: 0, totalPages: 0 });
    }
  }, [staffId, month, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const totals = useMemo(() => sumCommissions(data?.content ?? []), [data]);

  async function markPaid(c: Commission) {
    if (!window.confirm(`Đánh dấu đã chi trả ${formatVnd(c.amount)} cho dòng ${shortId(c.id)}?`)) return;
    setBusyId(c.id);
    try {
      await markCommissionPaid(c.id);
      toast.success("Đã đánh dấu chi trả.");
      await load();
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <Field className="w-48" type="month" label="Tháng ghi nhận" value={month} onChange={(e) => { setMonth(e.target.value); setPage(0); }} hint="Bỏ trống để xem mọi tháng." />
        <div className="w-full sm:w-72">
          <label htmlFor="comm-trainer" className="mb-1 block text-sm font-medium">
            Huấn luyện viên
          </label>
          <select id="comm-trainer" className="input" value={staffId} onChange={(e) => { setStaffId(e.target.value); setPage(0); }}>
            <option value="">Tất cả PT</option>
            {(trainers ?? []).map((t) => (
              <option key={t.id} value={t.id}>
                {t.fullName}
              </option>
            ))}
          </select>
          {trainersError && <p className="mt-1 text-xs text-rubber">Không tải được danh sách PT: {trainersError}</p>}
        </div>
      </div>

      {error && (
        <div className="mb-4">
          <Alert tone="error" title="Không tải được dữ liệu lương">
            {error}
          </Alert>
        </div>
      )}

      {data === null ? (
        <div className="flex justify-center py-10 text-steel">
          <Spinner />
        </div>
      ) : data && data.content.length === 0 ? (
        <EmptyState title="Không có dòng lương phù hợp">Đổi tháng hoặc bỏ lọc PT. Dòng lương chỉ có sau khi PT được đặt mức lương mỗi buổi và buổi dạy được chốt.</EmptyState>
      ) : (
        data && (
          <>
            <p className="mb-2 text-sm text-steel">
              Trang này: {totals.count} dòng · chờ chi trả {formatVnd(totals.pending)} · đã chi trả {formatVnd(totals.paid)} (tổng cả kỳ có thể nhiều hơn nếu còn trang khác)
            </p>
            <div className="panel overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-line text-left text-steel">
                  <tr>
                    <th className="px-3 py-2">Ghi nhận lúc</th>
                    <th className="px-3 py-2">PT</th>
                    <th className="px-3 py-2">Kết cục buổi</th>
                    <th className="px-3 py-2 text-right">Số tiền</th>
                    <th className="px-3 py-2">Trạng thái</th>
                    <th className="px-3 py-2" />
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((c) => (
                    <tr key={c.id} className="border-b border-line last:border-0">
                      <td className="px-3 py-2">{formatDateTime(c.createdAt)}</td>
                      <td className="px-3 py-2">{trainerLabel(trainers ?? [], c.staffId)}</td>
                      <td className="px-3 py-2">{OUTCOME_LABEL[c.outcome] ?? c.outcome}</td>
                      <td className="px-3 py-2 text-right">{formatVnd(c.amount)}</td>
                      <td className="px-3 py-2">
                        {STATUS_LABEL[c.status] ?? c.status}
                        {c.paidAt ? ` · ${formatDateTime(c.paidAt)}` : ""}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-right">
                        {c.status === "PENDING" && (
                          <button type="button" className="underline disabled:opacity-60" disabled={busyId === c.id} onClick={() => markPaid(c)}>
                            Đánh dấu đã trả
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </div>
  );
}
