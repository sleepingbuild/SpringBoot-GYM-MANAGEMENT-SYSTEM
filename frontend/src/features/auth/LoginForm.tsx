"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { errorMessage } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthContext";
import { AuthLayout } from "./AuthLayout";
import { safeNext } from "./safeNext";

export function LoginForm() {
  const { status, login } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const next = safeNext(params.get("next"));

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (status === "authenticated") router.replace(next);
  }, [status, router, next]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email.trim(), password);
      // Điều hướng do useEffect ở trên đảm nhận khi trạng thái chuyển sang authenticated.
    } catch (err) {
      setError(errorMessage(err));
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Đăng nhập"
      subtitle="Dùng email và mật khẩu của bạn."
      footer={
        <>
          Chưa có tài khoản?{" "}
          <Link href="/register" className="font-medium text-plate underline">
            Tạo tài khoản hội viên
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        {params.get("expired") && <Alert tone="warning">Phiên đăng nhập đã hết hạn. Đăng nhập lại để tiếp tục.</Alert>}
        {error && <Alert tone="error">{error}</Alert>}
        <Field label="Email" type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <Field label="Mật khẩu" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        <Button type="submit" loading={submitting} className="w-full">
          Đăng nhập
        </Button>
      </form>
    </AuthLayout>
  );
}
