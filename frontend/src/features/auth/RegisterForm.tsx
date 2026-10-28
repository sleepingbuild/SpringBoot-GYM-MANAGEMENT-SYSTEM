"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthContext";
import { AuthLayout } from "./AuthLayout";

interface FieldErrors {
  fullName?: string;
  email?: string;
  phone?: string;
  password?: string;
  confirm?: string;
}

export function RegisterForm() {
  const { status, register } = useAuth();
  const router = useRouter();

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (status === "authenticated") router.replace("/dashboard");
  }, [status, router]);

  function validate(): FieldErrors {
    const e: FieldErrors = {};
    if (!fullName.trim()) e.fullName = "Nhập họ tên.";
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) e.email = "Email chưa đúng định dạng.";
    if (password.length < 8) e.password = "Mật khẩu tối thiểu 8 ký tự.";
    if (confirm !== password) e.confirm = "Mật khẩu nhập lại chưa khớp.";
    return e;
  }

  async function onSubmit(ev: React.FormEvent) {
    ev.preventDefault();
    setError(null);
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      await register({ fullName: fullName.trim(), email: email.trim(), phone, password });
    } catch (err) {
      // Lỗi trùng hiện ngay dưới ô liên quan; lỗi khác hiện ở đầu biểu mẫu.
      if (err instanceof ApiError && err.is("AUTH_EMAIL_ALREADY_EXISTS")) {
        setErrors({ email: errorMessage(err) });
      } else if (err instanceof ApiError && err.is("AUTH_PHONE_ALREADY_EXISTS")) {
        setErrors({ phone: errorMessage(err) });
      } else {
        setError(errorMessage(err));
      }
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Tạo tài khoản hội viên"
      subtitle="Tài khoản dành cho hội viên. Nhân viên do quản trị viên cấp."
      footer={
        <>
          Đã có tài khoản?{" "}
          <Link href="/login" className="font-medium text-plate underline">
            Đăng nhập
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        {error && <Alert tone="error">{error}</Alert>}
        <Field label="Họ tên" autoComplete="name" required value={fullName} error={errors.fullName} onChange={(e) => setFullName(e.target.value)} />
        <Field label="Email" type="email" autoComplete="email" required value={email} error={errors.email} onChange={(e) => setEmail(e.target.value)} />
        <Field label="Số điện thoại" type="tel" autoComplete="tel" value={phone} error={errors.phone} hint="Không bắt buộc." onChange={(e) => setPhone(e.target.value)} />
        <Field label="Mật khẩu" type="password" autoComplete="new-password" required value={password} error={errors.password} hint="Tối thiểu 8 ký tự." onChange={(e) => setPassword(e.target.value)} />
        <Field label="Nhập lại mật khẩu" type="password" autoComplete="new-password" required value={confirm} error={errors.confirm} onChange={(e) => setConfirm(e.target.value)} />
        <Button type="submit" loading={submitting} className="w-full">
          Tạo tài khoản
        </Button>
      </form>
    </AuthLayout>
  );
}
