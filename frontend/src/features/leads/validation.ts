import type { ConvertInput, LeadInput } from "./types";

const VN_PHONE = /^0\d{9,10}$/;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Cùng quy tắc với backend: bỏ khoảng trắng, dấu chấm, gạch ngang, ngoặc; +84 hoặc 84 đầu số thành 0. */
export function normalizePhone(raw: string): string {
  let p = raw.replace(/[\s.\-()]/g, "");
  if (p.startsWith("+84")) p = `0${p.slice(3)}`;
  else if (p.startsWith("84") && p.length >= 11) p = `0${p.slice(2)}`;
  return p;
}

export function phoneError(raw: string): string | null {
  if (!raw.trim()) return "Nhập số điện thoại.";
  return VN_PHONE.test(normalizePhone(raw)) ? null : "Số điện thoại không hợp lệ (dạng 0xxxxxxxxx hoặc +84xxxxxxxxx).";
}

export type LeadFormErrors = Partial<Record<keyof LeadInput, string>>;

export function validateLead(v: LeadInput): LeadFormErrors {
  const errors: LeadFormErrors = {};
  if (!v.fullName.trim()) errors.fullName = "Nhập họ tên.";
  else if (v.fullName.trim().length > 150) errors.fullName = "Họ tên tối đa 150 ký tự.";
  const phone = phoneError(v.phone);
  if (phone) errors.phone = phone;
  if (v.source.trim().length > 50) errors.source = "Nguồn tối đa 50 ký tự.";
  if (v.notes.trim().length > 500) errors.notes = "Ghi chú tối đa 500 ký tự.";
  return errors;
}

export type ConvertErrors = Partial<Record<keyof ConvertInput, string>>;

export function validateConvert(v: ConvertInput): ConvertErrors {
  const errors: ConvertErrors = {};
  const email = v.email.trim();
  if (!email) errors.email = "Nhập email của hội viên.";
  else if (!EMAIL.test(email)) errors.email = "Email không hợp lệ.";
  else if (email.length > 150) errors.email = "Email tối đa 150 ký tự.";
  if (v.password !== "" && v.password.length < 8) errors.password = "Mật khẩu tối thiểu 8 ký tự, hoặc để trống để hệ thống tự tạo.";
  return errors;
}
