"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { useToast } from "@/lib/toast";
import { deactivatePackage, listAllPackages } from "./api";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { PACKAGE_TYPE_LABELS, PEAK_TYPE_LABELS, labelOf } from "./labels";
import { formatVnd, packageSummary } from "./format";
import { membershipErrorMessage } from "./messages";
import { PackageForm } from "./PackageForm";
import { Pagination } from "./Pagination";
import { StatusBadge } from "./StatusBadge";
import type { PackageItem } from "./types";
import { useLoad } from "./useLoad";

type Mode = { kind: "list" } | { kind: "create" } | { kind: "edit"; item: PackageItem };

/** Quản trị gói (Super Admin): xem cả gói đã ngừng bán, tạo, sửa và ngừng bán. Ngừng bán không xoá dữ liệu. */
export function AdminPackages() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [mode, setMode] = useState<Mode>({ kind: "list" });
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const state = useLoad((signal) => listAllPackages(page, 20, { signal }), [page]);

  async function deactivate(p: PackageItem) {
    setBusyId(p.id);
    try {
      await deactivatePackage(p.id);
      toast.success(`Đã ngừng bán ${p.name}.`);
      setConfirmId(null);
      state.reload();
    } catch (e) {
      toast.error(membershipErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  if (mode.kind !== "list") {
    return (
      <div>
        <PageHeader title="Quản trị gói" />
        <PackageForm
          editing={mode.kind === "edit" ? mode.item : null}
          onCancel={() => setMode({ kind: "list" })}
          onSaved={(saved) => {
            toast.success(mode.kind === "edit" ? `Đã lưu ${saved.name}.` : `Đã tạo ${saved.name}.`);
            setMode({ kind: "list" });
            state.reload();
          }}
        />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Quản trị gói"
        description="Danh sách đầy đủ, kể cả gói đã ngừng bán. Ngừng bán chỉ ẩn gói khỏi danh sách mua, hội viên đang dùng gói đó không bị ảnh hưởng."
        actions={<Button onClick={() => setMode({ kind: "create" })}>Tạo gói mới</Button>}
      />

      {state.status === "loading" && !state.data && <LoadingBlock text="Đang tải danh sách gói…" />}
      {state.status === "error" && <ErrorBlock error={state.error} onRetry={state.reload} />}

      {state.data && state.data.content.length === 0 && (
        <EmptyState title="Chưa có gói nào" action={<Button onClick={() => setMode({ kind: "create" })}>Tạo gói đầu tiên</Button>}>
          Tạo gói để hội viên có thể đăng ký.
        </EmptyState>
      )}

      {state.data && state.data.content.length > 0 && (
        <>
          <div className="panel overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  <th className="border-b px-3 py-2 font-medium text-steel">Gói</th>
                  <th className="border-b px-3 py-2 font-medium text-steel">Loại</th>
                  <th className="border-b px-3 py-2 font-medium text-steel">Giá</th>
                  <th className="border-b px-3 py-2 font-medium text-steel">Quyền lợi</th>
                  <th className="border-b px-3 py-2 font-medium text-steel">Trạng thái</th>
                  <th className="border-b px-3 py-2 font-medium text-steel">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {state.data.content.map((p) => (
                  <tr key={p.id}>
                    <td className="border-b px-3 py-2 align-top font-medium">{p.name}</td>
                    <td className="border-b px-3 py-2 align-top">
                      {labelOf(PACKAGE_TYPE_LABELS, p.packageType)}
                      <span className="block text-xs text-steel">{labelOf(PEAK_TYPE_LABELS, p.peakType)}</span>
                    </td>
                    <td className="whitespace-nowrap border-b px-3 py-2 align-top">{formatVnd(p.price)}</td>
                    <td className="border-b px-3 py-2 align-top">{packageSummary(p)}</td>
                    <td className="border-b px-3 py-2 align-top">
                      <StatusBadge tone={p.active ? "success" : "neutral"}>{p.active ? "Đang bán" : "Ngừng bán"}</StatusBadge>
                    </td>
                    <td className="border-b px-3 py-2 align-top">
                      {confirmId === p.id ? (
                        <div className="space-y-2">
                          <p className="text-xs">Ngừng bán {p.name}?</p>
                          <div className="flex gap-2">
                            <Button variant="danger" loading={busyId === p.id} onClick={() => deactivate(p)}>
                              Ngừng bán
                            </Button>
                            <Button variant="secondary" disabled={busyId === p.id} onClick={() => setConfirmId(null)}>
                              Huỷ
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="flex flex-wrap gap-2">
                          <Button variant="secondary" onClick={() => setMode({ kind: "edit", item: p })}>
                            Sửa
                          </Button>
                          {p.active && (
                            <Button variant="secondary" onClick={() => setConfirmId(p.id)}>
                              Ngừng bán
                            </Button>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={state.data.page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
