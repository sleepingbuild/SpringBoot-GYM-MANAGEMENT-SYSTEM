"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { createPackage, updatePackage } from "./api";
import { SelectField, TextAreaField } from "./FormControls";
import { PACKAGE_TYPE_LABELS, PEAK_TYPE_LABELS } from "./labels";
import { membershipErrorMessage } from "./messages";
import {
  EMPTY_PACKAGE_FORM,
  type PackageFormErrors,
  type PackageFormValues,
  toFormValues,
  toPackageRequest,
  validatePackageForm,
} from "./packageFormLogic";
import type { PackageItem, PackageType, PeakType } from "./types";

const TYPE_OPTIONS = (Object.keys(PACKAGE_TYPE_LABELS) as PackageType[]).map((v) => ({ value: v, label: PACKAGE_TYPE_LABELS[v] }));
const PEAK_OPTIONS = (Object.keys(PEAK_TYPE_LABELS) as PeakType[]).map((v) => ({ value: v, label: PEAK_TYPE_LABELS[v] }));

/** Form tạo hoặc sửa gói. `editing` rỗng là tạo mới. Kiểm tra ở trình duyệt chỉ để báo sớm; backend vẫn kiểm tra lại. */
export function PackageForm({ editing, onSaved, onCancel }: { editing: PackageItem | null; onSaved: (saved: PackageItem) => void; onCancel: () => void }) {
  const [values, setValues] = useState<PackageFormValues>(editing ? toFormValues(editing) : EMPTY_PACKAGE_FORM);
  const [errors, setErrors] = useState<PackageFormErrors>({});
  const [busy, setBusy] = useState(false);
  const [submitError, setSubmitError] = useState<unknown>(null);

  function set<K extends keyof PackageFormValues>(key: K, value: PackageFormValues[K]) {
    setValues((v) => ({ ...v, [key]: value }));
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const found = validatePackageForm(values);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setBusy(true);
    setSubmitError(null);
    try {
      const body = toPackageRequest(values);
      const saved = editing ? await updatePackage(editing.id, body) : await createPackage(body);
      onSaved(saved);
    } catch (err) {
      setSubmitError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="panel space-y-4 p-5">
      <h2 className="text-lg">{editing ? `Sửa gói: ${editing.name}` : "Tạo gói mới"}</h2>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field className="sm:col-span-2" label="Tên gói" required value={values.name} error={errors.name} maxLength={150} onChange={(e) => set("name", e.target.value)} />
        <TextAreaField className="sm:col-span-2" label="Mô tả" value={values.description} error={errors.description} maxLength={500} onChange={(e) => set("description", e.target.value)} />
        <Field label="Giá (đồng)" required type="number" inputMode="numeric" min={0} value={values.price} error={errors.price} onChange={(e) => set("price", e.target.value)} />
        <SelectField label="Loại gói" value={values.packageType} options={TYPE_OPTIONS} onChange={(e) => set("packageType", e.target.value as PackageType)} />
        <Field
          label="Số ngày sử dụng"
          type="number"
          inputMode="numeric"
          min={1}
          value={values.durationDays}
          error={errors.durationDays}
          hint={values.packageType === "TIME_BASED" ? "Bắt buộc với gói theo thời hạn." : "Để trống nếu không giới hạn thời gian."}
          onChange={(e) => set("durationDays", e.target.value)}
        />
        <Field
          label="Số buổi"
          type="number"
          inputMode="numeric"
          min={1}
          value={values.sessionCount}
          error={errors.sessionCount}
          hint={values.packageType === "TIME_BASED" ? "Để trống nếu không giới hạn buổi." : "Bắt buộc với gói theo buổi và gói có huấn luyện viên."}
          onChange={(e) => set("sessionCount", e.target.value)}
        />
        <SelectField label="Khung giờ" value={values.peakType} options={PEAK_OPTIONS} onChange={(e) => set("peakType", e.target.value as PeakType)} />
        <Field
          label="Số buổi PT tối đa mỗi tuần"
          type="number"
          inputMode="numeric"
          min={0}
          value={values.maxSessionsPerWeek}
          error={errors.maxSessionsPerWeek}
          hint="Để trống: không giới hạn. Nhập 0: không cho đặt PT."
          onChange={(e) => set("maxSessionsPerWeek", e.target.value)}
        />
      </div>

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={values.active} onChange={(e) => set("active", e.target.checked)} />
        Đang mở bán
      </label>

      {submitError != null && (
        <Alert tone="error" title="Chưa lưu được gói">
          {membershipErrorMessage(submitError)}
        </Alert>
      )}

      <div className="flex gap-2">
        <Button type="submit" loading={busy}>
          {editing ? "Lưu thay đổi" : "Tạo gói"}
        </Button>
        <Button variant="secondary" disabled={busy} onClick={onCancel}>
          Huỷ
        </Button>
      </div>
    </form>
  );
}
