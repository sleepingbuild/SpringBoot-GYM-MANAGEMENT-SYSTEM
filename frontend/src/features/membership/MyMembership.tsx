"use client";

import Link from "next/link";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { getMyMembership, listMyMemberships, listMyPayments } from "./api";
import { ChangePlan } from "./ChangePlan";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { formatDate } from "./format";
import { MEMBERSHIP_STATUS_LABELS, labelOf, membershipTone } from "./labels";
import { Pagination } from "./Pagination";
import { StatusBadge } from "./StatusBadge";
import { MembershipTable, PaymentTable } from "./Tables";
import { Tabs } from "./Tabs";
import { useLoad } from "./useLoad";

type TabId = "current" | "history" | "payments";

function CurrentTab() {
  const current = useLoad((signal) => getMyMembership({ signal }), []);
  // Gói đang chờ thanh toán chưa phải ACTIVE nên /me không trả về; lấy vài dòng lịch sử để báo cho hội viên biết.
  const recent = useLoad((signal) => listMyMemberships(0, 10, { signal }), []);
  const [changing, setChanging] = useState(false);

  if (current.status === "loading" && current.data === undefined) return <LoadingBlock text="Đang tải gói của bạn…" />;
  if (current.status === "error") return <ErrorBlock error={current.error} onRetry={current.reload} />;

  const mp = current.data ?? null;
  const pending = recent.data?.content.filter((m) => m.status === "PENDING") ?? [];
  const scheduled = recent.data?.content.filter((m) => m.status === "SCHEDULED") ?? [];

  return (
    <div className="space-y-4">
      {pending.map((m) => (
        <Alert key={m.id} tone="warning" title="Có gói đang chờ thanh toán">
          Gói <strong>{m.packageName}</strong> sẽ được kích hoạt sau khi lễ tân xác nhận bạn đã thanh toán tại quầy.
        </Alert>
      ))}
      {scheduled.map((m) => (
        <Alert key={m.id} tone="info" title="Có gói sắp bắt đầu">
          Gói <strong>{m.packageName}</strong> bắt đầu từ ngày {formatDate(m.startDate)}.
        </Alert>
      ))}

      {mp === null ? (
        <EmptyState
          title="Bạn chưa có gói tập nào đang dùng"
          action={
            <Link href="/packages" className="btn-primary">
              Xem các gói tập
            </Link>
          }
        >
          Nếu bạn vừa đăng ký, gói sẽ xuất hiện ở đây sau khi lễ tân xác nhận thanh toán.
        </EmptyState>
      ) : (
        <div className="panel p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg">{mp.packageName}</h2>
            <StatusBadge tone={membershipTone(mp.status)}>{labelOf(MEMBERSHIP_STATUS_LABELS, mp.status)}</StatusBadge>
          </div>
          <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
            <dt className="text-steel">Bắt đầu</dt>
            <dd>{formatDate(mp.startDate)}</dd>
            <dt className="text-steel">Hết hạn</dt>
            <dd>{mp.endDate ? formatDate(mp.endDate) : "Không thời hạn"}</dd>
            <dt className="text-steel">Số buổi còn lại</dt>
            <dd>{mp.remainingSessions == null ? "Không giới hạn buổi" : `${mp.remainingSessions} buổi`}</dd>
          </dl>

          <div className="mt-5 border-t pt-4">
            {changing ? (
              <ChangePlan
                current={mp}
                onCancel={() => setChanging(false)}
                onDone={() => {
                  setChanging(false);
                  current.reload();
                  recent.reload();
                }}
              />
            ) : (
              <Button variant="secondary" onClick={() => setChanging(true)}>
                Nâng cấp hoặc hạ cấp gói
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function HistoryTab() {
  const [page, setPage] = useState(0);
  const state = useLoad((signal) => listMyMemberships(page, 10, { signal }), [page]);

  if (state.status === "loading" && !state.data) return <LoadingBlock />;
  if (state.status === "error") return <ErrorBlock error={state.error} onRetry={state.reload} />;
  if (!state.data || state.data.content.length === 0) {
    return <EmptyState title="Chưa có lịch sử gói">Các gói bạn đã đăng ký sẽ hiện ở đây.</EmptyState>;
  }
  return (
    <>
      <MembershipTable rows={state.data.content} />
      <Pagination page={state.data.page} totalPages={state.data.totalPages} onChange={setPage} />
    </>
  );
}

function PaymentsTab() {
  const [page, setPage] = useState(0);
  const state = useLoad((signal) => listMyPayments(page, 10, { signal }), [page]);

  if (state.status === "loading" && !state.data) return <LoadingBlock />;
  if (state.status === "error") return <ErrorBlock error={state.error} onRetry={state.reload} />;
  if (!state.data || state.data.content.length === 0) {
    return <EmptyState title="Chưa có giao dịch thanh toán">Các giao dịch của bạn sẽ hiện ở đây.</EmptyState>;
  }
  return (
    <>
      <PaymentTable rows={state.data.content} />
      <Pagination page={state.data.page} totalPages={state.data.totalPages} onChange={setPage} />
    </>
  );
}

export function MyMembership() {
  const [tab, setTab] = useState<TabId>("current");
  return (
    <div>
      <PageHeader title="Gói của tôi" description="Gói đang dùng, số buổi còn lại, lịch sử gói và thanh toán." />
      <Tabs
        label="Gói của tôi"
        value={tab}
        onChange={setTab}
        tabs={[
          { id: "current", label: "Gói hiện tại" },
          { id: "history", label: "Lịch sử gói" },
          { id: "payments", label: "Lịch sử thanh toán" },
        ]}
      />
      {tab === "current" && <CurrentTab />}
      {tab === "history" && <HistoryTab />}
      {tab === "payments" && <PaymentsTab />}
    </div>
  );
}
