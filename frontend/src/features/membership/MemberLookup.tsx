"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { getMemberCurrentMembership, listMemberMemberships } from "./api";
import { ChangePlan } from "./ChangePlan";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { formatDate } from "./format";
import { MEMBERSHIP_STATUS_LABELS, labelOf, membershipTone } from "./labels";
import { MemberPicker } from "./MemberPicker";
import { Pagination } from "./Pagination";
import { StatusBadge } from "./StatusBadge";
import { MembershipTable } from "./Tables";
import type { MemberSummary } from "./types";
import { useLoad } from "./useLoad";

/** Lễ tân, Admin tra cứu gói của một hội viên và đổi gói hộ (H2). */
export function MemberLookup() {
  const [member, setMember] = useState<MemberSummary | null>(null);
  return (
    <div className="space-y-4">
      <MemberPicker value={member} onChange={setMember} />
      {member && <MemberDetail key={member.id} member={member} />}
    </div>
  );
}

function MemberDetail({ member }: { member: MemberSummary }) {
  const [page, setPage] = useState(0);
  const [changing, setChanging] = useState(false);
  const current = useLoad((signal) => getMemberCurrentMembership(member.id, { signal }), [member.id]);
  const history = useLoad((signal) => listMemberMemberships(member.id, page, 10, { signal }), [member.id, page]);

  function refresh() {
    current.reload();
    history.reload();
  }

  const active = current.data ?? null;
  const rows = history.data?.content ?? [];
  const hasPending = rows.some((m) => m.status === "PENDING");

  return (
    <div className="space-y-6">
      <section className="space-y-3" aria-labelledby="member-current-title">
        <h2 id="member-current-title" className="font-semibold">
          Gói đang dùng
        </h2>

        {current.status === "loading" && !current.data && <LoadingBlock text="Đang tải gói của hội viên…" />}
        {current.status === "error" && <ErrorBlock error={current.error} onRetry={current.reload} />}

        {current.status === "ready" && !active && (
          <Alert tone="info" title="Hội viên chưa có gói đang dùng">
            Bán gói mới ở tab Bán gói hộ hội viên.
          </Alert>
        )}

        {active && !changing && (
          <div className="panel space-y-2 p-4 text-sm">
            <p className="flex flex-wrap items-center gap-2">
              <strong>{active.packageName}</strong>
              <StatusBadge tone={membershipTone(active.status)}>{labelOf(MEMBERSHIP_STATUS_LABELS, active.status)}</StatusBadge>
            </p>
            <p className="text-steel">
              {formatDate(active.startDate)} – {active.endDate ? formatDate(active.endDate) : "không thời hạn"} · Số buổi còn lại:{" "}
              {active.remainingSessions == null ? "Không giới hạn" : active.remainingSessions}
            </p>
            <Button variant="secondary" onClick={() => setChanging(true)}>
              Đổi gói hộ hội viên
            </Button>
          </div>
        )}

        {active && changing && (
          <div className="panel p-4">
            <ChangePlan
              forStaff
              current={active}
              onCancel={() => setChanging(false)}
              onDone={() => {
                setChanging(false);
                refresh();
              }}
            />
          </div>
        )}

        {hasPending && (
          <Alert tone="warning" title="Có gói chờ thanh toán">
            Hội viên có gói đang chờ thu tiền. Xác nhận ở tab Chờ xác nhận sau khi đã nhận đủ tiền.
          </Alert>
        )}
      </section>

      <section className="space-y-3" aria-labelledby="member-history-title">
        <h2 id="member-history-title" className="font-semibold">
          Lịch sử gói
        </h2>
        {history.status === "loading" && !history.data && <LoadingBlock text="Đang tải lịch sử gói…" />}
        {history.status === "error" && <ErrorBlock error={history.error} onRetry={history.reload} />}
        {history.data && rows.length === 0 && <EmptyState title="Hội viên chưa từng đăng ký gói nào">Bán gói đầu tiên ở tab Bán gói hộ hội viên.</EmptyState>}
        {rows.length > 0 && <MembershipTable rows={rows} />}
        {history.data && <Pagination page={history.data.page} totalPages={history.data.totalPages} onChange={setPage} />}
      </section>
    </div>
  );
}
