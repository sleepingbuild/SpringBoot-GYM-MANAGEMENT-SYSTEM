"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { formatVnd, isUuid } from "@/features/pos/format";
import { useToast } from "@/lib/toast";
import { getPayRate, setPayRate } from "./api";

type Loaded = { trainerId: string; amount: number | null };

export function PayRateManager() {
  const toast = useToast();
  const [trainerId, setTrainerId] = useState("");
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [amount, setAmount] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const idError = trainerId.trim() !== "" && !isUuid(trainerId) ? "Mã PT phải là UUID hợp lệ." : null;
  const amountNumber = Number(amount);
  const amountError = amount.trim() !== "" && (!Number.isFinite(amountNumber) || amountNumber <= 0) ? "Số tiền phải lớn hơn 0." : null;

  async function lookup() {
    if (!isUuid(trainerId)) return;
    setLoading(true);
    setError(null);
    setLoaded(null);
    try {
      const rate = await getPayRate(trainerId.trim());
      setLoaded({ trainerId: trainerId.trim(), amount: rate.amountPerSession });
      setAmount(String(rate.amountPerSession));
    } catch (e) {
      if (e instanceof ApiError && e.kind === "notFound") {
        // Chưa cấu hình mức lương cho PT này: cho phép đặt mới.
        setLoaded({ trainerId: trainerId.trim(), amount: null });
        setAmount("");
      } else {
        setError(errorMessage(e));
      }
    } finally {
      setLoading(false);
    }
  }

  async function save() {
    if (!loaded || amount.trim() === "" || amountError) return;
    setSaving(true);
    setError(null);
    try {
      const rate = await setPayRate(loaded.trainerId, amountNumber);
      setLoaded({ trainerId: loaded.trainerId, amount: rate.amountPerSession });
      toast.success("Đã lưu mức lương mỗi buổi.");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="panel max-w-xl space-y-4 p-5">
      <p className="text-sm text-steel">
        Mức lương mỗi buổi áp dụng cho các buổi chốt từ lúc đặt. Đổi mức mới không sửa các dòng lương đã ghi trước đó. PT chưa có mức lương thì buổi dạy chưa được ghi lương.
      </p>
      <div className="flex flex-wrap items-end gap-3">
        <Field className="min-w-0 flex-1" label="Mã PT" value={trainerId} error={idError} onChange={(e) => { setTrainerId(e.target.value); setLoaded(null); }} hint="Dán mã (UUID) của huấn luyện viên." />
        <Button variant="secondary" loading={loading} disabled={!isUuid(trainerId)} onClick={lookup}>
          Xem mức hiện tại
        </Button>
      </div>

      {error && <Alert tone="error">{error}</Alert>}

      {loaded && (
        <div className="space-y-3 border-t border-line pt-4">
          <p className="text-sm">{loaded.amount === null ? "PT này chưa được đặt mức lương mỗi buổi." : `Mức hiện tại: ${formatVnd(loaded.amount)} mỗi buổi.`}</p>
          <Field label="Mức mới (VND mỗi buổi)" inputMode="numeric" value={amount} error={amountError} onChange={(e) => setAmount(e.target.value)} />
          <Button loading={saving} disabled={amount.trim() === "" || Boolean(amountError)} onClick={save}>
            Lưu mức lương
          </Button>
        </div>
      )}
    </div>
  );
}
