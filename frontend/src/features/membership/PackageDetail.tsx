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
import { getPackage, registerPackage } from "./api";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { PACKAGE_TYPE_LABELS, PEAK_TYPE_LABELS, labelOf } from "./labels";
import { formatVnd, packageSummary, weeklyLimitText } from "./format";
import { membershipErrorMessage } from "./messages";
import type { PackageItem, PackagePurchase } from "./types";
import { useLoad } from "./useLoad";

function RegisterPanel({ pkg }: { pkg: PackageItem }) {
  const toast = useToast();
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [result, setResult] = useState<PackagePurchase | null>(null);

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      const purchase = await registerPackage({ packageId: pkg.id });
      setResult(purchase);
      toast.success("Đã gửi đăng ký gói.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  if (result) {
    return (
      <Alert tone="success" title="Đã gửi đăng ký">
        <p>
          Mã giao dịch <strong className="font-mono">{result.paymentTransaction.transactionCode}</strong>, số tiền{" "}
          <strong>{formatVnd(result.paymentTransaction.amount)}</strong>.
        </p>
        <p className="mt-1">Gói sẽ được kích hoạt sau khi lễ tân xác nhận bạn đã thanh toán tại quầy.</p>
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
        <Alert tone="error" title="Chưa đăng ký được">
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
        <Button onClick={() => setConfirming(true)}>Đăng ký gói này</Button>
      ) : (
        <div className="space-y-3">
          <Alert tone="info" title={`Đăng ký ${pkg.name}?`}>
            Bạn sẽ thanh toán {formatVnd(pkg.price)} tại quầy. Gói chỉ bắt đầu tính từ ngày lễ tân xác nhận thanh toán.
          </Alert>
          <div className="flex gap-2">
            <Button loading={busy} onClick={submit}>
              Xác nhận đăng ký
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
          {isMember && <RegisterPanel pkg={p} />}
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
