"use client";

import { useId, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { useToast } from "@/lib/toast";
import { createLead, updateLead } from "./api";
import { Dialog } from "./Dialog";
import { leadErrorMessage } from "./errors";
import type { Lead, LeadInput } from "./types";
import { type LeadFormErrors, validateLead } from "./validation";

function initial(lead: Lead | null): LeadInput {
  return {
    fullName: lead?.fullName ?? "",
    phone: lead?.phone ?? "",
    source: lead?.source ?? "",
    notes: lead?.notes ?? "",
  };
}

/** Dùng cho cả tạo mới (lead = null) và sửa. */
export function LeadFormDialog({ lead, onClose, onSaved }: { lead: Lead | null; onClose: () => void; onSaved: () => void }) {
  const toast = useToast();
  const notesId = useId();
  const [values, setValues] = useState<LeadInput>(() => initial(lead));
  const [errors, setErrors] = useState<LeadFormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const set = (k: keyof LeadInput) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setValues((v) => ({ ...v, [k]: e.target.value }));

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const found = validateLead(values);
    setErrors(found);
    setServerError(null);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    try {
      if (lead) {
        await updateLead(lead.id, values);
        toast.success("Đã lưu thay đổi.");
      } else {
        const created = await createLead(values);
        toast.success("Đã thêm khách tiềm năng.");
        if (created.duplicatePhone) {
          toast.info("Lưu ý: số điện thoại này đã có ở một lead khác.");
        }
      }
      onSaved();
      onClose();
    } catch (err) {
      setServerError(leadErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog title={lead ? "Sửa thông tin khách" : "Thêm khách tiềm năng"} onClose={onClose}>
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        {serverError && <Alert tone="error">{serverError}</Alert>}
        <Field label="Họ tên" required maxLength={150} value={values.fullName} onChange={set("fullName")} error={errors.fullName} />
        <Field
          label="Số điện thoại"
          required
          inputMode="tel"
          value={values.phone}
          onChange={set("phone")}
          error={errors.phone}
          hint="Dạng 0912345678 hoặc +84912345678."
        />
        <Field label="Nguồn" maxLength={50} value={values.source} onChange={set("source")} error={errors.source} hint="Ví dụ: Facebook, Zalo, giới thiệu, đến trực tiếp." />
        <div>
          <label htmlFor={notesId} className="mb-1 block text-sm font-medium text-ink">
            Ghi chú
          </label>
          <textarea
            id={notesId}
            rows={3}
            maxLength={500}
            value={values.notes}
            onChange={set("notes")}
            aria-invalid={errors.notes ? true : undefined}
            className={`input ${errors.notes ? "input-error" : ""}`}
          />
          {errors.notes && <p className="mt-1 text-xs text-rubber">{errors.notes}</p>}
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Huỷ
          </Button>
          <Button type="submit" loading={saving}>
            {lead ? "Lưu thay đổi" : "Thêm khách"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
