"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Spinner } from "@/components/ui/Button";
import { useAuth } from "@/lib/auth/AuthContext";
import { homePathFor } from "@/lib/home";

/** Trang gốc chỉ điều hướng: đã đăng nhập vào trang đầu theo vai trò, chưa thì tới đăng nhập. */
export default function Home() {
  const { status, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "authenticated" && user) router.replace(homePathFor(user.roles));
    if (status === "unauthenticated") router.replace("/login");
  }, [status, user, router]);

  return (
    <div className="flex min-h-screen items-center justify-center text-steel" role="status" aria-label="Đang tải">
      <Spinner />
    </div>
  );
}
