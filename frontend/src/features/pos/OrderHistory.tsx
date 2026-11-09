"use client";

import { Fragment, useCallback, useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { errorMessage } from "@/lib/api/errors";
import type { PageResponse } from "@/lib/types";
import { useToast } from "@/lib/toast";
import { cancelOrder, listOrders } from "./api";
import { formatDateTime, formatVnd, shortId, todayIso } from "./format";
import { Pager } from "./Pager";
import { ORDER_STATUS_LABEL, PAYMENT_LABEL, type PaymentMethod, type PosOrder } from "./types";

export function OrderHistory() {
  const toast = useToast();
  const [from, setFrom] = useState(todayIso());
  const [to, setTo] = useState(todayIso());
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<PosOrder> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const rangeError = from && to && from > to ? "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc." : null;

  const load = useCallback(async () => {
    if (rangeError) return;
    setError(null);
    setData(null);
    try {
      setData(await listOrders({ from: from || undefined, to: to || undefined, page, size: 20 }));
    } catch (e) {
      setError(errorMessage(e));
      setData({ content: [], page, size: 20, totalElements: 0, totalPages: 0 });
    }
  }, [from, to, page, rangeError]);

  useEffect(() => {
    void load();
  }, [load]);

  async function cancel(order: PosOrder) {
    if (!window.confirm(`Huỷ đơn ${shortId(order.id)} (${formatVnd(order.totalAmount)})? Giao dịch thanh toán sẽ được hoàn tiền và tồn kho được hoàn lại.`)) return;
    setBusyId(order.id);
    try {
      await cancelOrder(order.id);
      toast.success("Đã huỷ đơn, hoàn tiền và hoàn lại tồn kho.");
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
        <Field className="w-44" type="date" label="Từ ngày" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} />
        <Field className="w-44" type="date" label="Đến ngày" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} error={rangeError} />
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

      {data === null && !rangeError ? (
        <div className="flex justify-center py-10 text-steel">
          <Spinner />
        </div>
      ) : data && data.content.length === 0 ? (
        <EmptyState title="Không có đơn trong khoảng này">Đổi khoảng ngày hoặc tạo đơn ở thẻ Bán hàng.</EmptyState>
      ) : (
        data && (
          <div className="panel overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-line text-left text-steel">
                <tr>
                  <th className="px-3 py-2">Mã</th>
                  <th className="px-3 py-2">Thời gian</th>
                  <th className="px-3 py-2">Thanh toán</th>
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
                      <td className="px-3 py-2">{PAYMENT_LABEL[o.paymentMethod as PaymentMethod] ?? o.paymentMethod}</td>
                      <td className="px-3 py-2 text-right">{formatVnd(o.totalAmount)}</td>
                      <td className="px-3 py-2">{ORDER_STATUS_LABEL[o.status] ?? o.status}</td>
                      <td className="space-x-3 whitespace-nowrap px-3 py-2 text-right">
                        <button type="button" className="underline" aria-expanded={openId === o.id} onClick={() => setOpenId(openId === o.id ? null : o.id)}>
                          {openId === o.id ? "Ẩn" : "Chi tiết"}
                        </button>
                        {o.status === "COMPLETED" && (
                          <button type="button" className="text-rubber underline disabled:opacity-60" disabled={busyId === o.id} onClick={() => cancel(o)}>
                            Huỷ đơn
                          </button>
                        )}
                      </td>
                    </tr>
                    {openId === o.id && (
                      <tr className="border-b border-line bg-chalk">
                        <td colSpan={6} className="px-3 py-3">
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
                          {o.memberId && <p className="mt-2 text-xs text-steel">Hội viên: {o.memberId}</p>}
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
      {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
    </div>
  );
}
