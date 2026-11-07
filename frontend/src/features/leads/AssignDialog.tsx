"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/lib/toast";
import { assignLead, type SalesUser } from "./assign";
import { Dialog } from "./Dialog";
import { leadErrorMessage } from "./errors";
import type { Lead } from "./types";

/** Super Admin giao lead cho một Sales, hoặc bỏ giao để mọi Sales đều thấy. */
export function AssignDialog({
  lead,
  salesUsers,
  onClose,
  onAssigned,
}: {
  lead: Lead;
  salesUsers: SalesUser[];
  onClose: () => void;
  onAssigned: () => void;
}) {
  const toast = useToast();
  const [salesId, setSalesId] = useState(lead.assignedSalesId ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await assignLead(lead.id, salesId || null);
      toast.success(salesId ? "Đã giao khách cho nhân viên." : "Đã bỏ giao, mọi nhân viên kinh doanh đều thấy khách này.");
      onAssigned();
      onClose();
    } catch (err) {
      setError(leadErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog title={`Giao khách: ${lead.fullName}`} onClose={onClose} widthClass="max-w-md">
      <form onSubmit={onSubmit} noValidate>
        {error && (
          <div className="mb-3">
            <Alert tone="error" title="Không giao được khách">
              {error}
            </Alert>
          </div>
        )}
        <label htmlFor="assign-sales" className="mb-1 block text-sm font-medium">
          Nhân viên phụ trách
        </label>
        <select id="assign-sales" className="input w-full" value={salesId} onChange={(e) => setSalesId(e.target.value)} data-autofocus>
          <option value="">Chưa giao (mọi nhân viên đều thấy)</option>
          {salesUsers.map((s) => (
            <option key={s.id} value={s.id}>
              {s.fullName}
              {s.phone ? ` (${s.phone})` : ""}
            </option>
          ))}
        </select>
        {salesUsers.length === 0 && <p className="mt-2 text-xs text-steel">Chưa có tài khoản nhân viên kinh doanh nào đang hoạt động.</p>}
        <div className="mt-5 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose} disabled={saving}>
            Huỷ
          </Button>
          <Button type="submit" loading={saving}>
            Lưu
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
