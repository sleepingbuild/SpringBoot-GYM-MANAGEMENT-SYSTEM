"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { formatVnd } from "@/features/pos/format";
import { useToast } from "@/lib/toast";
import { getPayRate, setPayRate } from "./api";
import { trainerLabel } from "./trainers";
import { useTrainers } from "./useTrainers";

/** amount = null nghĩa là PT này chưa được đặt mức lương mỗi buổi. */
type Loaded = { trainerId: string; amount: number | null };

export function PayRateManager() {
  const toast = useToast();
  const { trainers, error: trainersError } = useTrainers();
  const [trainerId, setTrainerId] = useState("");
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [amount, setAmount] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const amountNumber = Number(amount);
  const amountError = amount.trim() !== "" && (!Number.isFinite(amountNumber) || amountNumber <= 0) ? "Số tiền phải lớn hơn 0." : null;

  // Chọn PT là tự tải mức hiện tại. `alive` chặn kết quả của lần chọn cũ ghi đè lần chọn mới.
  useEffect(() => {
    setLoaded(null);
    setError(null);
    setAmount("");
    if (trainerId === "") return;
    let alive = true;
    setLoading(true);
    getPayRate(trainerId)
      .then((rate) => {
        if (!alive) return;
        setLoaded({ trainerId, amount: rate.amountPerSession });
        setAmount(String(rate.amountPerSession));
      })
      .catch((e: unknown) => {
        if (!alive) return;
        if (e instanceof ApiError && e.kind === "notFound") {
          setLoaded({ trainerId, amount: null }); // chưa cấu hình: cho phép đặt mới
        } else {
          setError(errorMessage(e));
        }
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [trainerId]);

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

      {trainersError && (
        <Alert tone="error" title="Không tải được danh sách PT">
          {trainersError}
        </Alert>
      )}

      <div>
        <label htmlFor="rate-trainer" className="mb-1 block text-sm font-medium">
          Huấn luyện viên
        </label>
        <select id="rate-trainer" className="input" value={trainerId} disabled={trainers === null} onChange={(e) => setTrainerId(e.target.value)}>
          <option value="">{trainers === null ? "Đang tải…" : "Chọn huấn luyện viên"}</option>
          {(trainers ?? []).map((t) => (
            <option key={t.id} value={t.id}>
              {t.fullName}
            </option>
          ))}
        </select>
      </div>

      {loading && (
        <div className="flex items-center gap-2 text-sm text-steel">
          <Spinner /> Đang tải mức lương hiện tại…
        </div>
      )}

      {error && <Alert tone="error">{error}</Alert>}

      {loaded && !loading && (
        <div className="space-y-3 border-t border-line pt-4">
          <p className="text-sm">
            {loaded.amount === null
              ? `${trainerLabel(trainers ?? [], loaded.trainerId)} chưa được đặt mức lương mỗi buổi.`
              : `Mức hiện tại của ${trainerLabel(trainers ?? [], loaded.trainerId)}: ${formatVnd(loaded.amount)} mỗi buổi.`}
          </p>
          <Field label="Mức mới (VND mỗi buổi)" inputMode="numeric" value={amount} error={amountError} onChange={(e) => setAmount(e.target.value)} />
          <Button loading={saving} disabled={amount.trim() === "" || Boolean(amountError)} onClick={save}>
            Lưu mức lương
          </Button>
        </div>
      )}
    </div>
  );
}
