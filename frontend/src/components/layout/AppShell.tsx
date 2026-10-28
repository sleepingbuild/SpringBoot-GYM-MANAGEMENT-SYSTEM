"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { NAVIGATION } from "@/config/navigation";
import { useAuth } from "@/lib/auth/AuthContext";
import { ROLE_LABELS, hasAnyRole } from "@/lib/roles";
import { Button, Spinner } from "../ui/Button";

/** Khung cho mọi trang cần đăng nhập: kiểm tra phiên, menu theo vai trò, nút đăng xuất. */
export function AppShell({ children }: { children: React.ReactNode }) {
  const { status, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [status, router, pathname]);

  // Đóng ngăn kéo menu khi chuyển trang (màn hình nhỏ).
  useEffect(() => setOpen(false), [pathname]);

  if (status !== "authenticated" || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center text-steel" role="status" aria-label="Đang tải">
        <Spinner />
      </div>
    );
  }

  const groups = NAVIGATION.map((g) => ({
    ...g,
    items: g.items.filter((i) => hasAnyRole(user.roles, i.roles)),
  })).filter((g) => g.items.length > 0);

  const nav = (
    <nav aria-label="Điều hướng chính" className="flex-1 overflow-y-auto px-3 pb-4">
      {groups.map((g) => (
        <div key={g.title} className="mt-5 first:mt-2">
          <p className="px-3 pb-1 text-xs font-semibold uppercase tracking-wide text-steel">{g.title}</p>
          <ul>
            {g.items.map((item) => {
              const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    className={`flex items-center justify-between rounded-control px-3 py-2 text-sm ${
                      active ? "bg-plate-tint font-semibold text-plate-dark" : "text-ink hover:bg-chalk"
                    }`}
                  >
                    <span>{item.label}</span>
                    {item.status === "planned" && <span className="rounded bg-chalk px-1.5 py-0.5 text-xs text-steel">Sắp có</span>}
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </nav>
  );

  const identity = (
    <div className="border-t border-line p-4">
      <p className="truncate text-sm font-semibold">{user.fullName}</p>
      <p className="truncate text-xs text-steel">{user.roles.map((r) => ROLE_LABELS[r] ?? r).join(", ")}</p>
      <Button variant="secondary" className="mt-3 w-full" onClick={() => void logout()}>
        Đăng xuất
      </Button>
    </div>
  );

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[16rem_1fr]">
      {/* Màn hình lớn: cột menu cố định */}
      <aside className="sticky top-0 hidden h-screen flex-col border-r border-line bg-paper lg:flex">
        <div className="px-6 py-5 text-lg font-bold tracking-tight text-plate">GMS</div>
        {nav}
        {identity}
      </aside>

      {/* Màn hình nhỏ: thanh trên + ngăn kéo */}
      <header className="flex items-center justify-between border-b border-line bg-paper px-4 py-3 lg:hidden">
        <span className="text-lg font-bold tracking-tight text-plate">GMS</span>
        <Button variant="secondary" aria-expanded={open} aria-controls="mobile-menu" onClick={() => setOpen((v) => !v)}>
          {open ? "Đóng" : "Menu"}
        </Button>
      </header>
      {open && (
        <div id="mobile-menu" className="fixed inset-x-0 bottom-0 top-[3.6rem] z-40 flex flex-col bg-paper lg:hidden">
          {nav}
          {identity}
        </div>
      )}

      <main className="min-w-0 px-4 py-6 sm:px-8 sm:py-8">
        <div className="mx-auto max-w-5xl">{children}</div>
      </main>
    </div>
  );
}
