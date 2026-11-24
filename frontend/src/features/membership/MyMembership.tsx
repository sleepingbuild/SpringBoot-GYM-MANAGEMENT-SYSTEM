"use client";

import Link from "next/link";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { useToast } from "@/lib/toast";
import { getMyMembership, listMyMemberships, listMyPayments, listPackages, purchasePackage } from "./api";
import { ChangePlan } from "./ChangePlan";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { formatDate, formatVnd } from "./format";
import { MEMBERSHIP_STATUS_LABELS, labelOf, membershipTone, type Tone } from "./labels";
import { membershipErrorMessage } from "./messages";
import { canRenew, daysChip, matchesTab, todayInVietnam, type DaysChip, type MembershipTab } from "./membershipView";
import { Pagination } from "./Pagination";
import { PaymentTable } from "./Tables";
import { Tabs } from "./Tabs";
import type { MemberPackage } from "./types";
import { useLoad } from "./useLoad";

type TabId = MembershipTab | "payments";

const CHIP_TONE: Record<DaysChip["tone"], Tone> = { good: "success", warn: "warning", bad: "danger" };

function RefreshIcon() {
  return (
    <svg
      className="h-4 w-4"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M21 12a9 9 0 1 1-3-6.7" />
      <path d="M21 3v6h-6" />
    </svg>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs text-steel">{label}</dt>
      <dd className="mt-0.5 font-semibold">{children}</dd>
    </div>
  );
}

/** Gia hạn một gói đã hết hạn: mua lại đúng gói đó (POST /member-packages/purchase), thanh toán mô phỏng. */
function RenewPanel({ mp, price, onDone }: { mp: MemberPackage; price: number | null; onDone: () => void }) {
  const toast = useToast();
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      await purchasePackage({ packageId: mp.packageId, branchId: mp.branchId ?? undefined });
      toast.success("Gia hạn thành công.");
      onDone();
    } catch (e) {
      setError(e);
      setBusy(false);
    }
  }

  return (
    <div className="space-y-3">
      {error != null && (
        <Alert tone="error" title="Chưa gia hạn được">
          {membershipErrorMessage(error)}
        </Alert>
      )}
      {!confirming ? (
        <Button className="w-full" onClick={() => setConfirming(true)}>
          <RefreshIcon />
          Gia hạn
        </Button>
      ) : (
        <div className="space-y-3">
          <Alert tone="info" title={`Gia hạn ${mp.packageName}?`}>
            {price != null && `Số tiền ${formatVnd(price)}. `}Đây là thanh toán mô phỏng: hệ thống ghi nhận thành công ngay và không
            trừ tiền thật. Gói mới được kích hoạt và tính từ hôm nay.
          </Alert>
          <div className="flex gap-2">
            <Button loading={busy} onClick={submit}>
              Xác nhận gia hạn
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

interface CardProps {
  mp: MemberPackage;
  /** Dữ liệu từ GET /member-packages/me của đúng gói này (có số buổi giữ chỗ); null nếu không phải gói đang dùng. */
  live: MemberPackage | null;
  price: number | null;
  today: string;
  renewable: boolean;
  onChanged: () => void;
}

function PackageCard({ mp, live, price, today, renewable, onChanged }: CardProps) {
  const [changing, setChanging] = useState(false);
  const chip = daysChip(mp, today);
  const held = live?.heldSessions;
  const bookable = live?.bookableSessions;

  return (
    <article className={changing ? "panel p-5 col-span-full" : "panel p-5"}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg">{mp.packageName}</h2>
          {price != null && <p className="mt-1 text-sm text-steel">{formatVnd(price)}</p>}
        </div>
        <Badge tone={membershipTone(mp.status)}>{labelOf(MEMBERSHIP_STATUS_LABELS, mp.status)}</Badge>
      </div>

      <dl className="mt-5 grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
        <Field label="Ngày bắt đầu">{formatDate(mp.startDate)}</Field>
        <Field label="Ngày kết thúc">{mp.endDate ? formatDate(mp.endDate) : "Không thời hạn"}</Field>
        {mp.status === "ACTIVE" && (
          <>
            <Field label="Số buổi còn lại">
              {mp.remainingSessions == null ? "Không giới hạn buổi" : `${mp.remainingSessions} buổi`}
            </Field>
            {held != null && bookable != null && (
              <>
                <Field label="Đang giữ chỗ">{held} buổi</Field>
                <Field label="Còn đặt được">{bookable} buổi</Field>
              </>
            )}
          </>
        )}
      </dl>
      {mp.status === "ACTIVE" && held != null && (
        <p className="mt-2 text-xs text-steel">Đang giữ chỗ gồm các buổi bạn đã đặt, đang chờ hoặc đã được xác nhận.</p>
      )}

      {chip && (
        <div className="mt-4 flex items-center gap-2 text-sm">
          <span className="text-steel">{chip.label}:</span>
          <Badge tone={CHIP_TONE[chip.tone]}>{chip.text}</Badge>
        </div>
      )}

      {mp.status === "SCHEDULED" && (
        <>
          <p className="mt-4 text-sm">
            <span className="text-steel">Kích hoạt từ:</span> <strong>{formatDate(mp.startDate)}</strong>
          </p>
          <div className="mt-3">
            <Alert tone="info">
              Sẽ tự động kích hoạt từ ngày <strong>{formatDate(mp.startDate)}</strong>
            </Alert>
          </div>
        </>
      )}

      {mp.status === "PENDING" && (
        <div className="mt-4">
          <Alert tone="warning" title="Đang chờ thanh toán">
            Gói sẽ được kích hoạt sau khi lễ tân xác nhận bạn đã thanh toán tại quầy.
          </Alert>
        </div>
      )}

      {mp.status === "ACTIVE" && (
        <div className="mt-5 border-t pt-4">
          {changing ? (
            <ChangePlan
              current={mp}
              onCancel={() => setChanging(false)}
              onDone={() => {
                setChanging(false);
                onChanged();
              }}
            />
          ) : (
            <Button variant="secondary" className="w-full" onClick={() => setChanging(true)}>
              Nâng cấp hoặc hạ cấp gói
            </Button>
          )}
        </div>
      )}

      {renewable && (
        <div className="mt-5">
          <RenewPanel mp={mp} price={price} onDone={onChanged} />
        </div>
      )}
    </article>
  );
}

const EMPTY_TEXT: Record<MembershipTab, { title: string; hint: string }> = {
  all: { title: "Bạn chưa có gói tập nào", hint: "Chọn một gói ở mục Gói tập để bắt đầu." },
  active: { title: "Bạn chưa có gói nào đang hoạt động", hint: "Gói chờ thanh toán hoặc sắp bắt đầu xem ở tab Tất cả." },
  history: { title: "Chưa có gói đã kết thúc", hint: "Các gói đã hết hạn hoặc đã huỷ sẽ hiện ở đây." },
};

function PackagesTab({ tab }: { tab: MembershipTab }) {
  // Lấy nhiều gói một lần (một hội viên có rất ít gói) để lọc tab ở trình duyệt cho đúng.
  const list = useLoad((signal) => listMyMemberships(0, 50, { signal }), []);
  // Chỉ GET /member-packages/me mới có số buổi giữ chỗ và số buổi còn đặt được.
  const me = useLoad((signal) => getMyMembership({ signal }), []);
  // Giá lấy từ danh sách gói đang bán; gói đã ngừng bán không có giá thì ẩn dòng giá, không báo lỗi.
  const packages = useLoad((signal) => listPackages(0, 50, { signal }), []);

  if (list.status === "loading" && !list.data) return <LoadingBlock text="Đang tải gói của bạn…" />;
  if (list.status === "error") return <ErrorBlock error={list.error} onRetry={list.reload} />;

  const all = list.data?.content ?? [];
  const shown = all.filter((m) => matchesTab(m.status, tab));
  const prices = new Map<string, number>((packages.data?.content ?? []).map((p) => [p.id, p.price]));
  const today = todayInVietnam();
  const reload = () => {
    list.reload();
    me.reload();
  };

  if (shown.length === 0) {
    return (
      <EmptyState
        icon="💪" title={EMPTY_TEXT[tab].title}
        action={
          <Link href="/packages" className="btn-primary">
            Xem các gói tập
          </Link>
        }
      >
        {EMPTY_TEXT[tab].hint}
      </EmptyState>
    );
  }

  return (
    <>
      <div className="card-grid">
        {shown.map((m) => (
          <PackageCard
            key={m.id}
            mp={m}
            live={me.data && me.data.id === m.id ? me.data : null}
            price={prices.get(m.packageId) ?? null}
            today={today}
            renewable={canRenew(m, all)}
            onChanged={reload}
          />
        ))}
      </div>
      {(list.data?.totalPages ?? 1) > 1 && <p className="mt-3 text-xs text-steel">Chỉ hiển thị 50 gói gần nhất.</p>}
    </>
  );
}

function PaymentsTab() {
  const [page, setPage] = useState(0);
  const state = useLoad((signal) => listMyPayments(page, 10, { signal }), [page]);

  if (state.status === "loading" && !state.data) return <LoadingBlock />;
  if (state.status === "error") return <ErrorBlock error={state.error} onRetry={state.reload} />;
  if (!state.data || state.data.content.length === 0) {
    return <EmptyState icon="💳" title="Chưa có giao dịch thanh toán">Các giao dịch của bạn sẽ hiện ở đây.</EmptyState>;
  }
  return (
    <>
      <PaymentTable rows={state.data.content} />
      <Pagination page={state.data.page} totalPages={state.data.totalPages} onChange={setPage} />
    </>
  );
}

export function MyMembership() {
  const [tab, setTab] = useState<TabId>("all");
  return (
    <div>
      <PageHeader icon="💪" title="Gói tập của tôi" description="Quản lý gói tập, số buổi còn lại và thanh toán của bạn." />
      <Tabs
        label="Gói của tôi"
        value={tab}
        onChange={setTab}
        tabs={[
          { id: "all", label: "Tất cả" },
          { id: "active", label: "Đang hoạt động" },
          { id: "history", label: "Lịch sử" },
          { id: "payments", label: "Thanh toán" },
        ]}
      />
      {tab === "payments" ? <PaymentsTab /> : <PackagesTab tab={tab} />}
    </div>
  );
}
