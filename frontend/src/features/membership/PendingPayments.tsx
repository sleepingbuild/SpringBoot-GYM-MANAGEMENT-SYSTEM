"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { useToast } from "@/lib/toast";
import { confirmPayment, listPayments } from "./api";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { SelectField } from "./FormControls";
import { formatVnd } from "./format";
import { membershipErrorMessage } from "./messages";
import { Pagination } from "./Pagination";
import { PaymentTable, buyerName } from "./Tables";
import type { PaymentTransaction } from "./types";
import { useLoad } from "./useLoad";

const STATUS_OPTIONS = [
  { value: "PENDING", label: "Chờ xác nhận" },
  { value: "SUCCESS", label: "Đã thanh toán" },
  { value: "REFUNDED", label: "Đã hoàn tiền" },
  { value: "FAILED", label: "Thất bại" },
  { value: "", label: "Tất cả trạng thái" },
];

/** Danh sách giao dịch cho Lễ tân, Admin; mặc định hiện giao dịch chờ xác nhận để xác nhận đã thu tiền tại quầy. */
export function PendingPayments() {
  const toast = useToast();
  const [status, setStatus] = useState("PENDING");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const state = useLoad(
    (signal) => listPayments({ status: status || undefined, from: from || undefined, to: to || undefined }, page, 20, { signal }),
    [status, from, to, page],
  );

  // Lọc thêm ở trình duyệt để màn hình đúng cả khi backend chưa hỗ trợ lọc theo trạng thái.
  const rows = (state.data?.content ?? []).filter((p) => !status || p.status === status);

  async function confirm(p: PaymentTransaction) {
    setBusyId(p.id);
    try {
      await confirmPayment(p.id);
      toast.success(`Đã xác nhận thanh toán ${p.transactionCode}.`);
      setConfirmId(null);
      state.reload();
    } catch (e) {
      toast.error(membershipErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  function renderAction(p: PaymentTransaction) {
    if (p.status !== "PENDING") return <span className="text-steel">—</span>;
    if (confirmId !== p.id) {
      return (
        <Button variant="secondary" onClick={() => setConfirmId(p.id)}>
          Xác nhận đã thu
        </Button>
      );
    }
    return (
      <div className="space-y-2">
        <p className="text-xs">
          Đã thu đủ <strong>{formatVnd(p.amount)}</strong> của <strong>{buyerName(p)}</strong>?
        </p>
        <div className="flex gap-2">
          <Button loading={busyId === p.id} onClick={() => confirm(p)}>
            Đã thu
          </Button>
          <Button variant="secondary" disabled={busyId === p.id} onClick={() => setConfirmId(null)}>
            Huỷ
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="panel grid gap-3 p-4 sm:grid-cols-3">
        <SelectField
          label="Trạng thái"
          value={status}
          options={STATUS_OPTIONS}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        />
        <Field
          label="Từ ngày"
          type="date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setPage(0);
          }}
        />
        <Field
          label="Đến ngày"
          type="date"
          value={to}
          hint="Không chọn ngày thì dùng mặc định của hệ thống."
          onChange={(e) => {
            setTo(e.target.value);
            setPage(0);
          }}
        />
      </div>

      {state.status === "loading" && !state.data && <LoadingBlock text="Đang tải giao dịch…" />}
      {state.status === "error" && <ErrorBlock error={state.error} onRetry={state.reload} />}

      {state.data && rows.length === 0 && (
        <EmptyState title={status === "PENDING" ? "Không có giao dịch nào chờ xác nhận" : "Không có giao dịch phù hợp"}>
          Thử đổi bộ lọc, hoặc chờ hội viên đăng ký gói rồi quay lại.
        </EmptyState>
      )}

      {rows.length > 0 && (
        <>
          <Alert tone="info">Chỉ bấm xác nhận sau khi đã nhận đủ tiền của hội viên. Xác nhận xong gói sẽ được kích hoạt ngay.</Alert>
          <PaymentTable rows={rows} showBuyer renderAction={renderAction} />
        </>
      )}

      {state.data && <Pagination page={state.data.page} totalPages={state.data.totalPages} onChange={setPage} />}
    </div>
  );
}
