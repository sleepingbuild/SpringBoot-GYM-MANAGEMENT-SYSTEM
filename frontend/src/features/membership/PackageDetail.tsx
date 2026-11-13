"use client";

import Link from "next/link";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { useAuth } from "@/lib/auth/AuthContext";
import { ApiError } from "@/lib/api/errors";
import { hasAnyRole } from "@/lib/roles";
import { useToast } from "@/lib/toast";
import { getPackage, purchasePackage } from "./api";
import { BranchSelect } from "./BranchSelect";
import { branchIdOrUndefined } from "./branchOptions";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { PACKAGE_TYPE_LABELS, PEAK_TYPE_LABELS, labelOf } from "./labels";
import { formatDate, formatVnd, packageSummary, weeklyLimitText } from "./format";
import { membershipErrorMessage } from "./messages";
import type { PackageItem, PackagePurchase } from "./types";
import { useLoad } from "./useLoad";

/** Hội viên tự mua: thanh toán mô phỏng thành công ngay, gói ACTIVE luôn (POST /member-packages/purchase). */
function PurchasePanel({ pkg }: { pkg: PackageItem }) {
  const toast = useToast();
  const [confirming, setConfirming] = useState(false);
  const [branchId, setBranchId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [result, setResult] = useState<PackagePurchase | null>(null);

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      const purchase = await purchasePackage({ packageId: pkg.id, branchId: branchIdOrUndefined(branchId) });
      setResult(purchase);
      toast.success("Mua gói thành công.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  if (result) {
    const mp = result.memberPackage;
    return (
      <Alert tone="success" title="Mua gói thành công">
        <p>
          Mã giao dịch <strong className="font-mono">{result.paymentTransaction.transactionCode}</strong>, số tiền{" "}
          <strong>{formatVnd(result.paymentTransaction.amount)}</strong>.
        </p>
        <p className="mt-1">
          Gói đã được kích hoạt, hiệu lực từ {formatDate(mp.startDate)}
          {mp.endDate ? ` đến ${formatDate(mp.endDate)}` : ", không thời hạn"}.{" "}
          {mp.remainingSessions == null ? "Không giới hạn buổi." : `Còn ${mp.remainingSessions} buổi.`}
        </p>
        <p className="mt-2">
          <Link href="/my-membership" className="underline">
            Xem gói của tôi
          </Link>
        </p>
      </Alert>
    );
  }

  const pendingExists = error instanceof ApiError && error.is("MEMBERSHIP_PENDING_EXISTS");

  return (
    <div className="space-y-3">
      {error != null && (
        <Alert tone="error" title="Chưa mua được gói">
          {membershipErrorMessage(error)}
          {pendingExists && (
            <>
              {" "}
              <Link href="/my-membership" className="underline">
                Xem gói của tôi
              </Link>
            </>
          )}
        </Alert>
      )}
      {!confirming ? (
        <Button onClick={() => setConfirming(true)}>Mua gói này</Button>
      ) : (
        <div className="space-y-3">
          <Alert tone="info" title={`Mua ${pkg.name}?`}>
            Số tiền {formatVnd(pkg.price)}. Đây là thanh toán mô phỏng: hệ thống ghi nhận thành công ngay và không trừ
            tiền thật. Gói được kích hoạt và tính từ hôm nay.
          </Alert>
          <BranchSelect value={branchId} onChange={setBranchId} />
          <div className="flex gap-2">
            <Button loading={busy} onClick={submit}>
              Xác nhận mua
            </Button>
            <Button variant="secondary" disabled={busy} onClick={() => setConfirming(false)}>
              Để sau
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

export function PackageDetail({ id }: { id: string }) {
  const { user } = useAuth();
  const state = useLoad((signal) => getPackage(id, { signal }), [id]);

  const isMember = !!user && hasAnyRole(user.roles, ["MEMBER"]);
  const isStaff = !!user && hasAnyRole(user.roles, ["RECEPTIONIST", "SUPER_ADMIN"]);

  const back = (
    <Link href="/packages" className="btn-secondary">
      Về danh sách gói
    </Link>
  );

  if (state.status === "loading" && !state.data) return <LoadingBlock text="Đang tải gói tập…" />;

  if (state.status === "error") {
    if (state.error instanceof ApiError && state.error.is("PACKAGE_NOT_FOUND")) {
      return (
        <EmptyState title="Không tìm thấy gói tập" action={back}>
          Gói có thể đã ngừng bán hoặc đường dẫn không đúng.
        </EmptyState>
      );
    }
    return <ErrorBlock error={state.error} onRetry={state.reload} />;
  }

  const p = state.data;
  if (!p) return null;

  return (
    <div>
      <PageHeader title={p.name} description={p.description ?? undefined} actions={back} />
      <div className="grid gap-4 lg:grid-cols-3">
        <dl className="panel grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 p-5 text-sm lg:col-span-2">
          <dt className="text-steel">Giá</dt>
          <dd className="font-semibold">{formatVnd(p.price)}</dd>
          <dt className="text-steel">Quyền lợi</dt>
          <dd>{packageSummary(p)}</dd>
          <dt className="text-steel">Loại gói</dt>
          <dd>{labelOf(PACKAGE_TYPE_LABELS, p.packageType)}</dd>
          <dt className="text-steel">Khung giờ</dt>
          <dd>{labelOf(PEAK_TYPE_LABELS, p.peakType)}</dd>
          <dt className="text-steel">Đặt PT mỗi tuần</dt>
          <dd>{weeklyLimitText(p.maxSessionsPerWeek)}</dd>
        </dl>

        <div className="panel p-5">
          {isMember && <PurchasePanel pkg={p} />}
          {isStaff && (
            <p className="text-sm">
              Để bán gói này cho hội viên, vào mục{" "}
              <Link href="/payments" className="underline">
                Thanh toán
              </Link>
              , chọn Bán gói hộ.
            </p>
          )}
          {!isMember && !isStaff && <p className="text-sm text-steel">Tài khoản này không đăng ký gói trực tiếp.</p>}
        </div>
      </div>
    </div>
  );
}
