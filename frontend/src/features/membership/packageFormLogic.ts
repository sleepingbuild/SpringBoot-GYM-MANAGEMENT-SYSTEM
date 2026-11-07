import type { PackageItem, PackageRequestBody, PackageType, PeakType } from "./types";

/** Giá trị trên form quản trị gói. Ô số giữ dạng chuỗi để người dùng gõ dở vẫn hiển thị đúng. */
export interface PackageFormValues {
  name: string;
  description: string;
  price: string;
  durationDays: string;
  sessionCount: string;
  maxSessionsPerWeek: string;
  packageType: PackageType;
  peakType: PeakType;
  active: boolean;
}

export type PackageFormField = "name" | "description" | "price" | "durationDays" | "sessionCount" | "maxSessionsPerWeek";
export type PackageFormErrors = Partial<Record<PackageFormField, string>>;

export const EMPTY_PACKAGE_FORM: PackageFormValues = {
  name: "",
  description: "",
  price: "",
  durationDays: "",
  sessionCount: "",
  maxSessionsPerWeek: "",
  packageType: "TIME_BASED",
  peakType: "FULL_TIME",
  active: true,
};

export function toFormValues(p: PackageItem): PackageFormValues {
  return {
    name: p.name,
    description: p.description ?? "",
    price: String(p.price),
    durationDays: p.durationDays == null ? "" : String(p.durationDays),
    sessionCount: p.sessionCount == null ? "" : String(p.sessionCount),
    maxSessionsPerWeek: p.maxSessionsPerWeek == null ? "" : String(p.maxSessionsPerWeek),
    packageType: p.packageType,
    peakType: p.peakType,
    active: p.active,
  };
}

/** Chuỗi rỗng -> null; còn lại -> số (NaN nếu không phải số). */
function parseOptionalNumber(raw: string): number | null {
  const t = raw.trim();
  if (t === "") return null;
  return Number(t);
}

function isPositiveInt(n: number): boolean {
  return Number.isInteger(n) && n > 0;
}

const MAX_PRICE = 9_999_999_999;

export function validatePackageForm(v: PackageFormValues): PackageFormErrors {
  const errors: PackageFormErrors = {};

  const name = v.name.trim();
  if (name === "") errors.name = "Nhập tên gói.";
  else if (name.length > 150) errors.name = "Tên gói tối đa 150 ký tự.";

  if (v.description.length > 500) errors.description = "Mô tả tối đa 500 ký tự.";

  const price = parseOptionalNumber(v.price);
  if (price === null) errors.price = "Nhập giá gói.";
  else if (!Number.isFinite(price) || price <= 0) errors.price = "Giá gói phải lớn hơn 0.";
  else if (price > MAX_PRICE) errors.price = "Giá gói quá lớn.";

  const duration = parseOptionalNumber(v.durationDays);
  if (duration !== null && !isPositiveInt(duration)) errors.durationDays = "Số ngày phải là số nguyên dương.";

  const sessions = parseOptionalNumber(v.sessionCount);
  if (sessions !== null && !isPositiveInt(sessions)) errors.sessionCount = "Số buổi phải là số nguyên dương.";

  const weekly = parseOptionalNumber(v.maxSessionsPerWeek);
  if (weekly !== null && !(Number.isInteger(weekly) && weekly >= 0)) {
    errors.maxSessionsPerWeek = "Nhập số nguyên từ 0 trở lên, hoặc để trống nếu không giới hạn.";
  }

  // Quy tắc theo loại gói: gói theo thời hạn cần số ngày; gói theo buổi và gói PT cần số buổi.
  if (v.packageType === "TIME_BASED" && duration === null && !errors.durationDays) {
    errors.durationDays = "Gói theo thời hạn cần số ngày sử dụng.";
  }
  if ((v.packageType === "SESSION_BASED" || v.packageType === "PT_1ON1") && sessions === null && !errors.sessionCount) {
    errors.sessionCount = "Gói theo buổi hoặc có huấn luyện viên cần số buổi.";
  }

  return errors;
}

/** Chỉ gọi khi validatePackageForm không có lỗi. */
export function toPackageRequest(v: PackageFormValues): PackageRequestBody {
  const description = v.description.trim();
  return {
    name: v.name.trim(),
    description: description === "" ? null : description,
    price: Number(v.price.trim()),
    durationDays: parseOptionalNumber(v.durationDays),
    sessionCount: parseOptionalNumber(v.sessionCount),
    packageType: v.packageType,
    peakType: v.peakType,
    maxSessionsPerWeek: parseOptionalNumber(v.maxSessionsPerWeek),
    active: v.active,
  };
}
