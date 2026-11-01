"use client";

import Link from "next/link";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/lib/toast";
import { downgradePackage, listPackages, upgradePackage } from "./api";
import { splitChangeOptions, type ChangeKind } from "./changeOptions";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { formatDate, formatVnd, packageSummary } from "./format";
import { membershipErrorMessage } from "./messages";
import type { MemberPackage, PackageItem, PackagePurchase } from "./types";
import { useLoad } from "./useLoad";

interface Choice {
  kind: ChangeKind;
  pkg: PackageItem;
}

function OptionList({ title, hint, kind, items, selected, onSelect }: { title: string; hint: string; kind: ChangeKind; items: PackageItem[]; selected: Choice | null; onSelect: (c: Choice) => void }) {
  if (items.length === 0) return null;
  return (
    <fieldset className="space-y-2">
      <legend className="font-semibold">{title}</legend>
      <p className="text-xs text-steel">{hint}</p>
      {items.map((p) => (
        <label key={p.id} className="panel flex cursor-pointer items-start gap-3 p-3 text-sm">
          <input type="radio" name="change-plan" className="mt-1" checked={selected?.pkg.id === p.id} onChange={() => onSelect({ kind, pkg: p })} />
          <span>
            <span className="font-medium">{p.name}</span> · {formatVnd(p.price)}
            <span className="block text-xs text-steel">{packageSummary(p)}</span>
          </span>
        </label>
      ))}
    </fieldset>
  );
}

/** Nâng cấp hoặc hạ cấp từ gói ACTIVE hiện tại. Backend là nơi kiểm tra điều kiện thật; ở đây chỉ gợi ý và giải thích hệ quả. */
export function ChangePlan({ current, onDone, onCancel }: { current: MemberPackage; onDone: () => void; onCancel: () => void }) {
  const toast = useToast();
  const packages = useLoad((signal) => listPackages(0, 50, { signal }), []);
  const [choice, setChoice] = useState<Choice | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [result, setResult] = useState<{ kind: ChangeKind; purchase: PackagePurchase } | null>(null);

  if (packages.status === "loading" && !packages.data) return <LoadingBlock text="Đang tải các gói có thể đổi…" />;
  if (packages.status === "error") return <ErrorBlock error={packages.error} onRetry={packages.reload} />;

  const currentPkg = packages.data?.content.find((p) => p.id === current.packageId);
  if (!currentPkg) {
    return (
      <Alert tone="warning" title="Chưa thể tự đổi gói">
        Gói hiện tại đã ngừng bán nên không so được giá. Liên hệ lễ tân để được đổi gói.
      </Alert>
    );
  }

  const { upgrades, downgrades } = splitChangeOptions(current.packageId, currentPkg.price, packages.data?.content ?? []);

  async function submit() {
    if (!choice) return;
    setBusy(true);
    setError(null);
    try {
      const input = { newPackageId: choice.pkg.id };
      const purchase = choice.kind === "upgrade" ? await upgradePackage(current.id, input) : await downgradePackage(current.id, input);
      setResult({ kind: choice.kind, purchase });
      toast.success(choice.kind === "upgrade" ? "Đã tạo gói mới, chờ thanh toán." : "Đã đặt lịch đổi gói.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  if (result) {
    const { kind, purchase } = result;
    return (
      <div className="space-y-3">
        <Alert tone="success" title={kind === "upgrade" ? "Đã tạo gói nâng cấp" : "Đã đặt lịch hạ cấp"}>
          {kind === "upgrade" ? (
            <>
              Gói cũ đã được huỷ. Gói mới <strong>{purchase.memberPackage.packageName}</strong> sẽ kích hoạt sau khi lễ tân xác nhận bạn đã thanh toán{" "}
              <strong>{formatVnd(purchase.paymentTransaction.amount)}</strong> (mã <span className="font-mono">{purchase.paymentTransaction.transactionCode}</span>).
            </>
          ) : (
            <>
              Gói hiện tại được giữ đến hết hạn. Gói <strong>{purchase.memberPackage.packageName}</strong> sẽ bắt đầu từ ngày {formatDate(purchase.memberPackage.startDate)}.
            </>
          )}
        </Alert>
        <Button onClick={onDone}>Xong</Button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <p className="text-sm">
        Gói hiện tại: <strong>{currentPkg.name}</strong> · {formatVnd(currentPkg.price)}
      </p>

      {upgrades.length === 0 && downgrades.length === 0 && <Alert tone="info">Hiện chưa có gói khác để đổi. Xem thêm ở mục <Link href="/packages" className="underline">Gói tập</Link>.</Alert>}

      <OptionList title="Nâng cấp" hint="Giá gói mới từ bằng đến cao hơn gói hiện tại." kind="upgrade" items={upgrades} selected={choice} onSelect={setChoice} />
      <OptionList title="Hạ cấp" hint="Giá gói mới thấp hơn gói hiện tại." kind="downgrade" items={downgrades} selected={choice} onSelect={setChoice} />

      {choice && (
        <Alert tone="warning" title={choice.kind === "upgrade" ? `Nâng cấp lên ${choice.pkg.name}?` : `Hạ cấp xuống ${choice.pkg.name}?`}>
          {choice.kind === "upgrade"
            ? `Gói hiện tại bị huỷ NGAY khi bạn xác nhận. Gói mới (${formatVnd(choice.pkg.price)}) chỉ dùng được sau khi thanh toán và được lễ tân xác nhận.`
            : `Gói hiện tại vẫn dùng đến hết hạn. Gói mới (${formatVnd(choice.pkg.price)}) bắt đầu ngay sau đó và được tính tiền ngay bây giờ.`}
        </Alert>
      )}

      {error != null && (
        <Alert tone="error" title="Chưa đổi được gói">
          {membershipErrorMessage(error)}
        </Alert>
      )}

      <div className="flex gap-2">
        <Button disabled={!choice} loading={busy} variant={choice?.kind === "upgrade" ? "danger" : "primary"} onClick={submit}>
          Xác nhận đổi gói
        </Button>
        <Button variant="secondary" disabled={busy} onClick={onCancel}>
          Không đổi
        </Button>
      </div>
    </div>
  );
}
