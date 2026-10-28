"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Spinner } from "@/components/ui/Button";
import { useAuth } from "@/lib/auth/AuthContext";

/** Trang gốc chỉ điều hướng: đã đăng nhập vào tổng quan, chưa thì tới đăng nhập. */
export default function Home() {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "authenticated") router.replace("/dashboard");
    if (status === "unauthenticated") router.replace("/login");
  }, [status, router]);

  return (
    <div className="flex min-h-screen items-center justify-center text-steel" role="status" aria-label="Đang tải">
      <Spinner />
    </div>
  );
}
