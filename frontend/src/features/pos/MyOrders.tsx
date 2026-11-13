"use client";

import { Fragment, useCallback, useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { errorMessage } from "@/lib/api/errors";
import type { PageResponse } from "@/lib/types";
import { listMyOrders } from "./api";
import { formatDateTime, formatVnd, shortId } from "./format";
import { Pager } from "./Pager";
import { ORDER_STATUS_LABEL, type PosOrder } from "./types";

/** Lịch sử đơn của chính hội viên. Không có nút huỷ: huỷ và hoàn tiền do lễ tân/admin xử lý. */
export function MyOrders() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<PosOrder> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    setData(null);
    try {
      setData(await listMyOrders(page, 20));
    } catch (e) {
      setError(errorMessage(e));
      setData({ content: [], page, size: 20, totalElements: 0, totalPages: 0 });
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div>
      <div className="mb-4">
        <Button variant="secondary" onClick={() => void load()}>
          Tải lại
        </Button>
      </div>

      {error && (
        <div className="mb-4">
          <Alert tone="error" title="Không tải được đơn hàng">
            {error}
          </Alert>
        </div>
      )}

      {data === null ? (
        <div className="flex justify-center py-10 text-steel">
          <Spinner />
        </div>
      ) : data.content.length === 0 ? (
        <EmptyState title="Bạn chưa có đơn hàng nào">Mua hàng ở thẻ Mua hàng, đơn sẽ hiện ở đây.</EmptyState>
      ) : (
        <div className="panel overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-line text-left text-steel">
              <tr>
                <th className="px-3 py-2">Mã</th>
                <th className="px-3 py-2">Thời gian</th>
                <th className="px-3 py-2 text-right">Tổng</th>
                <th className="px-3 py-2">Trạng thái</th>
                <th className="px-3 py-2" />
              </tr>
            </thead>
            <tbody>
              {data.content.map((o) => (
                <Fragment key={o.id}>
                  <tr className="border-b border-line">
                    <td className="px-3 py-2 font-mono">{shortId(o.id)}</td>
                    <td className="px-3 py-2">{formatDateTime(o.createdAt)}</td>
                    <td className="px-3 py-2 text-right">{formatVnd(o.totalAmount)}</td>
                    <td className="px-3 py-2">{ORDER_STATUS_LABEL[o.status] ?? o.status}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right">
                      <button type="button" className="underline" aria-expanded={openId === o.id} onClick={() => setOpenId(openId === o.id ? null : o.id)}>
                        {openId === o.id ? "Ẩn" : "Chi tiết"}
                      </button>
                    </td>
                  </tr>
                  {openId === o.id && (
                    <tr className="border-b border-line bg-chalk">
                      <td colSpan={5} className="px-3 py-3">
                        <ul className="space-y-1">
                          {o.items.map((i) => (
                            <li key={i.productId} className="flex justify-between gap-3">
                              <span>
                                {i.productName} × {i.quantity} ({formatVnd(i.unitPrice)})
                              </span>
                              <span>{formatVnd(i.subtotal)}</span>
                            </li>
                          ))}
                        </ul>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </div>
  );
}
