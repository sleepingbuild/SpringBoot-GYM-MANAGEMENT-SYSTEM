"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { useToast } from "@/lib/toast";
import { convertLead } from "./api";
import { Dialog } from "./Dialog";
import { leadErrorMessage } from "./errors";
import type { ConvertInput, Lead } from "./types";
import { type ConvertErrors, validateConvert } from "./validation";

/**
 * Chuyển lead thành hội viên. Mật khẩu tạm (nếu hệ thống tự sinh) chỉ nằm trong state của hộp thoại này,
 * hiển thị một lần và mất khi đóng; không lưu vào localStorage, không ghi log.
 */
export function ConvertDialog({ lead, onClose, onConverted }: { lead: Lead; onClose: () => void; onConverted: () => void }) {
  const toast = useToast();
  const [values, setValues] = useState<ConvertInput>({ email: "", password: "" });
  const [errors, setErrors] = useState<ConvertErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState<Lead | null>(null);
  const [copied, setCopied] = useState<"idle" | "ok" | "failed">("idle");

  const set = (k: keyof ConvertInput) => (e: React.ChangeEvent<HTMLInputElement>) => setValues((v) => ({ ...v, [k]: e.target.value }));

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const found = validateConvert(values);
    setErrors(found);
    setServerError(null);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    try {
      const converted = await convertLead(lead.id, values);
      onConverted();
      if (converted.temporaryPassword) {
        setResult(converted);
      } else {
        toast.success("Đã chuyển thành hội viên.");
        onClose();
      }
    } catch (err) {
      setServerError(leadErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function copyPassword() {
    if (!result?.temporaryPassword) return;
    try {
      await navigator.clipboard.writeText(result.temporaryPassword);
      setCopied("ok");
    } catch {
      setCopied("failed");
    }
  }

  if (result?.temporaryPassword) {
    return (
      <Dialog title="Đã tạo tài khoản hội viên" onClose={onClose}>
        <div className="space-y-4">
          <Alert tone="success">
            Đã chuyển <strong>{lead.fullName}</strong> thành hội viên. Đăng nhập bằng email <strong>{values.email.trim()}</strong>.
          </Alert>
          <div>
            <p className="mb-1 text-sm font-medium">Mật khẩu tạm</p>
            <code className="block rounded-control border border-line bg-chalk px-3 py-2 text-base tracking-wider" data-testid="temporary-password">
              {result.temporaryPassword}
            </code>
          </div>
          <Alert tone="warning" title="Chỉ hiển thị một lần">
            Hãy chuyển mật khẩu này cho hội viên qua kênh riêng. Sau khi đóng hộp thoại sẽ không xem lại được.
          </Alert>
          {copied === "failed" && <p className="text-xs text-rubber">Không sao chép được tự động. Hãy bôi đen và sao chép thủ công.</p>}
          {copied === "ok" && <p className="text-xs text-moss">Đã sao chép.</p>}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={copyPassword}>
              Sao chép mật khẩu
            </Button>
            <Button onClick={onClose} data-autofocus>
              Đã lưu, đóng
            </Button>
          </div>
        </div>
      </Dialog>
    );
  }

  return (
    <Dialog title="Chuyển thành hội viên" onClose={onClose}>
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <p className="text-sm">
          Tạo tài khoản hội viên cho <strong>{lead.fullName}</strong> ({lead.phone}). Việc mua gói tập làm ở bước riêng sau khi có tài khoản.
        </p>
        {serverError && <Alert tone="error">{serverError}</Alert>}
        <Field label="Email hội viên" type="email" required autoComplete="off" value={values.email} onChange={set("email")} error={errors.email} />
        <Field
          label="Mật khẩu (không bắt buộc)"
          type="text"
          autoComplete="off"
          value={values.password}
          onChange={set("password")}
          error={errors.password}
          hint="Để trống để hệ thống tự tạo mật khẩu ngẫu nhiên và hiển thị một lần."
        />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Huỷ
          </Button>
          <Button type="submit" loading={saving}>
            Chuyển thành hội viên
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
