"use client";

import Link from "next/link";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { useToast } from "@/lib/toast";
import { confirmPayment, listPackages, registerPackage } from "./api";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { SelectField } from "./FormControls";
import { formatVnd } from "./format";
import { MemberPicker } from "./MemberPicker";
import { membershipErrorMessage } from "./messages";
import type { MemberSummary, PackagePurchase } from "./types";
import { useLoad } from "./useLoad";

/** Lễ tân, Admin đăng ký gói cho hội viên tại quầy, rồi xác nhận đã thu tiền ngay hoặc để chờ. */
export function SellForMember() {
  const toast = useToast();
  const packages = useLoad((signal) => listPackages(0, 50, { signal }), []);
  const [member, setMember] = useState<MemberSummary | null>(null);
  const [packageId, setPackageId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [purchase, setPurchase] = useState<PackagePurchase | null>(null);
  const [paid, setPaid] = useState(false);

  if (packages.status === "loading" && !packages.data) return <LoadingBlock text="Đang tải các gói…" />;
  if (packages.status === "error") return <ErrorBlock error={packages.error} onRetry={packages.reload} />;

  const list = packages.data?.content ?? [];
  if (list.length === 0) {
    return <EmptyState title="Chưa có gói nào đang mở bán">Tạo gói ở mục Quản trị gói (cần quyền quản trị viên) rồi quay lại.</EmptyState>;
  }

  const chosen = list.find((p) => p.id === packageId) ?? null;

  function reset() {
    setMember(null);
    setPackageId("");
    setPurchase(null);
    setPaid(false);
    setError(null);
  }

  async function submit() {
    if (!member || !chosen) return;
    setBusy(true);
    setError(null);
    try {
      setPurchase(await registerPackage({ packageId: chosen.id, userId: member.id }));
      toast.success("Đã tạo đăng ký cho hội viên.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  async function collectNow() {
    if (!purchase) return;
    setBusy(true);
    setError(null);
    try {
      await confirmPayment(purchase.paymentTransaction.id);
      setPaid(true);
      toast.success("Đã xác nhận thanh toán, gói đã được kích hoạt.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  if (purchase) {
    const tx = purchase.paymentTransaction;
    return (
      <div className="space-y-3">
        <Alert tone="success" title={paid ? "Đã thu tiền và kích hoạt gói" : "Đã tạo đăng ký, đang chờ thu tiền"}>
          Gói <strong>{purchase.memberPackage.packageName}</strong> cho <strong>{member?.fullName}</strong>, số tiền <strong>{formatVnd(tx.amount)}</strong>, mã giao dịch{" "}
          <span className="font-mono">{tx.transactionCode}</span>.
          {!paid && " Chỉ bấm xác nhận khi đã nhận đủ tiền."}
        </Alert>
        {error != null && (
          <Alert tone="error" title="Chưa xác nhận được">
            {membershipErrorMessage(error)}
          </Alert>
        )}
        <div className="flex flex-wrap gap-2">
          {!paid && (
            <Button loading={busy} onClick={collectNow}>
              Đã thu tiền, kích hoạt gói
            </Button>
          )}
          <Button variant="secondary" disabled={busy} onClick={reset}>
            Bán gói khác
          </Button>
        </div>
        {!paid && (
          <p className="text-sm text-steel">
            Chưa thu tiền? Giao dịch vẫn nằm ở tab Chờ xác nhận để xác nhận sau.{" "}
            <Link href="/packages" className="underline">
              Xem các gói
            </Link>
          </p>
        )}
      </div>
    );
  }

  const pendingExists = error != null && membershipErrorMessage(error).includes("chờ thanh toán");

  return (
    <div className="space-y-4">
      <MemberPicker value={member} onChange={setMember} />
      <SelectField
        label="Gói tập"
        value={packageId}
        onChange={(e) => setPackageId(e.target.value)}
        options={[{ value: "", label: "Chọn gói…" }, ...list.map((p) => ({ value: p.id, label: `${p.name} · ${formatVnd(p.price)}` }))]}
      />
      {error != null && (
        <Alert tone="error" title="Chưa tạo được đăng ký">
          {membershipErrorMessage(error)}
          {pendingExists && " Xác nhận thanh toán của gói đó ở tab Chờ xác nhận trước."}
        </Alert>
      )}
      <Button disabled={!member || !chosen} loading={busy} onClick={submit}>
        Tạo đăng ký
      </Button>
    </div>
  );
}
