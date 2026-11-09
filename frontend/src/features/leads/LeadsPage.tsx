"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { useAuth } from "@/lib/auth/AuthContext";
import { useToast } from "@/lib/toast";
import type { PageResponse } from "@/lib/types";
import { changeLeadStatus, listLeads } from "./api";
import { listSalesUsers, type SalesUser } from "./assign";
import { AssignDialog } from "./AssignDialog";
import { ConvertDialog } from "./ConvertDialog";
import { ConfirmDialog } from "./Dialog";
import { isAbort, leadErrorMessage } from "./errors";
import { formatDateTime } from "./format";
import { LeadFormDialog } from "./LeadFormDialog";
import { STATUS_LABELS, allowedStatusChanges, canAssign, canConvert, canEdit } from "./status";
import { StatusBadge } from "./StatusBadge";
import { LEAD_STATUSES, type Lead, type LeadStatus } from "./types";

const PAGE_SIZE = 10;

const SORTS = [
  { value: "createdAt,desc", label: "Mới nhất" },
  { value: "createdAt,asc", label: "Cũ nhất" },
  { value: "fullName,asc", label: "Tên A đến Z" },
  { value: "status,asc", label: "Theo trạng thái" },
];

type Modal =
  | { kind: "create" }
  | { kind: "edit"; lead: Lead }
  | { kind: "convert"; lead: Lead }
  | { kind: "assign"; lead: Lead }
  | { kind: "lost"; lead: Lead }
  | null;

export function LeadsPage() {
  const toast = useToast();
  const { user } = useAuth();
  const isAdmin = !!user?.roles.includes("SUPER_ADMIN");
  const [status, setStatus] = useState<LeadStatus | "">("");
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState(SORTS[0].value);
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);

  const [data, setData] = useState<PageResponse<Lead> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modal, setModal] = useState<Modal>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  // Chỉ Super Admin cần danh sách Sales (để giao lead và hiện tên người phụ trách).
  const [salesUsers, setSalesUsers] = useState<SalesUser[] | null>(null);
  useEffect(() => {
    if (!isAdmin) return;
    const ctrl = new AbortController();
    listSalesUsers(ctrl.signal)
      .then(setSalesUsers)
      .catch((e) => {
        if (!isAbort(e)) toast.error(leadErrorMessage(e));
      });
    return () => ctrl.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

  function assigneeLabel(lead: Lead): string {
    if (!lead.assignedSalesId) return "Chưa giao";
    const found = salesUsers?.find((s) => s.id === lead.assignedSalesId);
    return found ? found.fullName : salesUsers ? "Nhân viên không còn hoạt động" : "…";
  }

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);
  const closeModal = useCallback(() => setModal(null), []);

  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    setError(null);
    listLeads({ status, keyword, page, size: PAGE_SIZE, sort }, ctrl.signal)
      .then((result) => {
        if (result.content.length === 0 && result.totalPages > 0 && page >= result.totalPages) {
          setPage(result.totalPages - 1); // trang hiện tại đã hết dòng (vừa xử lý xong dòng cuối)
          return;
        }
        setData(result);
        setLoading(false);
      })
      .catch((e) => {
        if (isAbort(e)) return;
        setError(leadErrorMessage(e));
        setLoading(false);
      });
    return () => ctrl.abort();
  }, [status, keyword, page, sort, reloadKey]);

  async function applyStatus(lead: Lead, next: "CONTACTED" | "LOST") {
    setBusyId(lead.id);
    try {
      await changeLeadStatus(lead.id, next);
      toast.success(next === "LOST" ? "Đã đánh dấu lead đã mất." : "Đã chuyển sang Đã liên hệ.");
      setModal(null);
      reload();
    } catch (err) {
      toast.error(leadErrorMessage(err));
      setModal(null);
      reload(); // lead có thể đã đổi ở nơi khác: tải lại để thấy trạng thái thật
    } finally {
      setBusyId(null);
    }
  }

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    setKeyword(keywordInput.trim());
  }

  function clearFilters() {
    setStatus("");
    setKeywordInput("");
    setKeyword("");
    setPage(0);
  }

  const filtered = status !== "" || keyword !== "";
  const leads = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Khách tiềm năng"
        description="Theo dõi khách quan tâm, ghi nhận lần liên hệ và chuyển thành hội viên. Sales chỉ thấy khách được giao cho mình hoặc chưa có người phụ trách."
        actions={<Button onClick={() => setModal({ kind: "create" })}>Thêm khách</Button>}
      />

      <form onSubmit={onSearch} className="panel mb-4 flex flex-wrap items-end gap-3 p-4">
        <div>
          <label htmlFor="lead-status" className="mb-1 block text-sm font-medium">
            Trạng thái
          </label>
          <select
            id="lead-status"
            className="input"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as LeadStatus | "");
              setPage(0);
            }}
          >
            <option value="">Tất cả</option>
            {LEAD_STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
        <div className="min-w-[12rem] flex-1">
          <label htmlFor="lead-keyword" className="mb-1 block text-sm font-medium">
            Tìm theo tên hoặc số điện thoại
          </label>
          <input id="lead-keyword" className="input" value={keywordInput} onChange={(e) => setKeywordInput(e.target.value)} placeholder="Ví dụ: Nguyễn hoặc 0912" />
        </div>
        <div>
          <label htmlFor="lead-sort" className="mb-1 block text-sm font-medium">
            Sắp xếp
          </label>
          <select
            id="lead-sort"
            className="input"
            value={sort}
            onChange={(e) => {
              setSort(e.target.value);
              setPage(0);
            }}
          >
            {SORTS.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </div>
        <Button type="submit" variant="secondary">
          Tìm
        </Button>
      </form>

      {error && (
        <div className="mb-4">
          <Alert tone="error" title="Không tải được danh sách">
            {error}{" "}
            <button type="button" className="underline" onClick={reload}>
              Thử lại
            </button>
          </Alert>
        </div>
      )}

      {loading && !data && (
        <div className="flex justify-center py-16 text-steel" role="status" aria-label="Đang tải danh sách khách">
          <Spinner />
        </div>
      )}

      {!loading && !error && leads.length === 0 && (
        <EmptyState
          title={filtered ? "Không có khách nào khớp bộ lọc" : "Chưa có khách tiềm năng nào"}
          action={
            filtered ? (
              <Button variant="secondary" onClick={clearFilters}>
                Xoá bộ lọc
              </Button>
            ) : (
              <Button onClick={() => setModal({ kind: "create" })}>Thêm khách đầu tiên</Button>
            )
          }
        >
          {filtered ? "Thử bỏ bớt điều kiện lọc hoặc đổi từ khoá." : "Thêm người quan tâm đến phòng tập để bắt đầu theo dõi và chăm sóc."}
        </EmptyState>
      )}

      {leads.length > 0 && (
        <div className={`panel overflow-x-auto ${loading ? "opacity-60" : ""}`} aria-busy={loading}>
          <table className="w-full min-w-[44rem] text-left text-sm">
            <thead className="border-b border-line text-xs text-steel">
              <tr>
                <th className="px-4 py-3 font-medium">Khách</th>
                <th className="px-4 py-3 font-medium">Nguồn</th>
                <th className="px-4 py-3 font-medium">Trạng thái</th>
                {isAdmin && <th className="px-4 py-3 font-medium">Phụ trách</th>}
                <th className="px-4 py-3 font-medium">Tạo lúc</th>
                <th className="px-4 py-3 font-medium">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {leads.map((lead) => (
                <tr key={lead.id} className="border-b border-line align-top last:border-0">
                  <td className="px-4 py-3">
                    <p className="font-medium">{lead.fullName}</p>
                    <p className="text-steel">{lead.phone}</p>
                    {lead.notes && <p className="mt-1 max-w-xs truncate text-xs text-steel" title={lead.notes}>{lead.notes}</p>}
                  </td>
                  <td className="px-4 py-3">{lead.source ?? ""}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={lead.status} />
                  </td>
                  {isAdmin && <td className="px-4 py-3">{assigneeLabel(lead)}</td>}
                  <td className="whitespace-nowrap px-4 py-3">{formatDateTime(lead.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      {allowedStatusChanges(lead.status).includes("CONTACTED") && (
                        <Button variant="secondary" disabled={busyId === lead.id} onClick={() => applyStatus(lead, "CONTACTED")}>
                          Đã liên hệ
                        </Button>
                      )}
                      {canConvert(lead.status) && (
                        <Button disabled={busyId === lead.id} onClick={() => setModal({ kind: "convert", lead })}>
                          Chuyển hội viên
                        </Button>
                      )}
                      {isAdmin && canAssign(lead.status) && (
                        <Button variant="secondary" disabled={busyId === lead.id || salesUsers === null} onClick={() => setModal({ kind: "assign", lead })}>
                          Giao cho
                        </Button>
                      )}
                      {canEdit(lead.status) && (
                        <Button variant="secondary" disabled={busyId === lead.id} onClick={() => setModal({ kind: "edit", lead })}>
                          Sửa
                        </Button>
                      )}
                      {allowedStatusChanges(lead.status).includes("LOST") && (
                        <Button variant="danger" disabled={busyId === lead.id} onClick={() => setModal({ kind: "lost", lead })}>
                          Đánh dấu mất
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
          <p className="text-steel">
            Trang {data.page + 1} / {data.totalPages} ({data.totalElements} khách)
          </p>
          <div className="flex gap-2">
            <Button variant="secondary" disabled={page === 0 || loading} onClick={() => setPage((p) => Math.max(0, p - 1))}>
              Trang trước
            </Button>
            <Button variant="secondary" disabled={page + 1 >= data.totalPages || loading} onClick={() => setPage((p) => p + 1)}>
              Trang sau
            </Button>
          </div>
        </div>
      )}

      {modal?.kind === "create" && <LeadFormDialog lead={null} onClose={closeModal} onSaved={reload} />}
      {modal?.kind === "edit" && <LeadFormDialog lead={modal.lead} onClose={closeModal} onSaved={reload} />}
      {modal?.kind === "convert" && <ConvertDialog lead={modal.lead} onClose={closeModal} onConverted={reload} />}
      {modal?.kind === "assign" && salesUsers && <AssignDialog lead={modal.lead} salesUsers={salesUsers} onClose={closeModal} onAssigned={reload} />}
      {modal?.kind === "lost" && (
        <ConfirmDialog
          title="Đánh dấu khách đã mất?"
          message={`${modal.lead.fullName} sẽ chuyển sang "Đã mất" và không thể mở lại. Chỉ làm khi chắc chắn khách không còn quan tâm.`}
          confirmLabel="Đánh dấu mất"
          danger
          busy={busyId === modal.lead.id}
          onConfirm={() => applyStatus(modal.lead, "LOST")}
          onClose={closeModal}
        />
      )}
    </div>
  );
}
