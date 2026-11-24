"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { NAVIGATION, type NavItem } from "@/config/navigation";
import { useAuth } from "@/lib/auth/AuthContext";
import { ROLE_LABELS, hasAnyRole } from "@/lib/roles";
import { Badge } from "../ui/Badge";
import { Button, Spinner } from "../ui/Button";

const isActive = (pathname: string, href: string) => pathname === href || pathname.startsWith(`${href}/`);

function Chevron({ open }: { open: boolean }) {
  return (
    <svg viewBox="0 0 20 20" className={`h-4 w-4 transition-transform ${open ? "rotate-180" : ""}`} fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M5 8l5 5 5-5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function Logo() {
  return (
    <Link href="/" className="flex items-center gap-2.5" aria-label="GMS, về trang chủ">
      <span className="flex h-9 w-9 items-center justify-center rounded-control bg-gradient-to-b from-accent to-accent-dark text-white">
        <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="M6.5 6.5v11M17.5 6.5v11M3.5 9v6M20.5 9v6M6.5 12h11" />
        </svg>
      </span>
      <span className="text-lg font-bold tracking-tight"></span>
    </Link>
  );
}

/**
 * Khung cho mọi trang cần đăng nhập: kiểm tra phiên, thanh menu trên cùng theo vai trò, menu tài khoản.
 * Nhóm có từ 2 mục trở lên hiện thành danh sách xổ xuống; nhóm chỉ có 1 mục (sau khi lọc theo vai trò) hiện thẳng như liên kết.
 * Màn hình nhỏ: nút Menu mở bảng toàn chiều rộng liệt kê đủ các nhóm.
 */
export function AppShell({ children }: { children: React.ReactNode }) {
  const { status, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  /** Khóa của danh sách xổ xuống đang mở: tên nhóm, hoặc "account" cho menu tài khoản. */
  const [openKey, setOpenKey] = useState<string | null>(null);
  const headerRef = useRef<HTMLElement>(null);

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [status, router, pathname]);

  // Đóng mọi menu khi chuyển trang.
  useEffect(() => {
    setMobileOpen(false);
    setOpenKey(null);
  }, [pathname]);

  // Bấm ra ngoài hoặc nhấn Escape thì đóng danh sách xổ xuống.
  useEffect(() => {
    if (!openKey) return;
    const onDown = (e: MouseEvent) => {
      if (headerRef.current && !headerRef.current.contains(e.target as Node)) setOpenKey(null);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpenKey(null);
    };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [openKey]);

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

  const roleText = user.roles.map((r) => ROLE_LABELS[r] ?? r).join(", ");
  const toggle = (key: string) => setOpenKey((cur) => (cur === key ? null : key));

  const topLink = (active: boolean) =>
    `inline-flex h-16 items-center gap-1 border-b-2 px-3 text-sm font-medium transition-colors ${
      active ? "border-accent text-ink" : "border-transparent text-steel hover:text-ink"
    }`;

  const dropdownLink = (item: NavItem) => (
    <li key={item.href}>
      <Link
        href={item.href}
        aria-current={isActive(pathname, item.href) ? "page" : undefined}
        className={`flex items-center justify-between gap-3 rounded-control px-3 py-2 text-sm ${
          isActive(pathname, item.href) ? "bg-plate-tint font-semibold text-plate" : "text-ink hover:bg-raised"
        }`}
      >
        <span>{item.label}</span>
        {item.status === "planned" && <Badge>Sắp có</Badge>}
      </Link>
    </li>
  );

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-50 focus:rounded-control focus:bg-paper focus:px-3 focus:py-2">
        Bỏ qua menu, tới nội dung
      </a>

      <header ref={headerRef} className="sticky top-0 z-40 border-b border-line bg-chalk/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4 sm:px-6 2xl:max-w-[90rem]">
          <Logo />

          {/* Màn hình lớn: menu ngang */}
          <nav aria-label="Điều hướng chính" className="ml-2 hidden h-full flex-1 items-center gap-1 lg:flex">
            {groups.map((g) => {
              const groupActive = g.items.some((i) => isActive(pathname, i.href));
              if (g.items.length === 1) {
                const only = g.items[0];
                return (
                  <Link key={g.title} href={only.href} aria-current={groupActive ? "page" : undefined} className={topLink(groupActive)}>
                    {only.label}
                  </Link>
                );
              }
              const open = openKey === g.title;
              return (
                <div key={g.title} className="relative h-full">
                  <button type="button" aria-expanded={open} aria-haspopup="true" onClick={() => toggle(g.title)} className={topLink(groupActive)}>
                    {g.title}
                    <Chevron open={open} />
                  </button>
                  {open && <ul className="absolute left-0 top-full mt-1 min-w-[13rem] rounded-panel border border-line bg-paper p-1 shadow-lg shadow-black/40">{g.items.map(dropdownLink)}</ul>}
                </div>
              );
            })}
          </nav>

          <div className="ml-auto flex items-center gap-2 lg:ml-0">
            {/* Menu tài khoản */}
            <div className="relative">
              <button
                type="button"
                aria-expanded={openKey === "account"}
                aria-haspopup="true"
                onClick={() => toggle("account")}
                className="flex items-center gap-2 rounded-control px-2 py-1.5 text-sm text-steel hover:bg-raised hover:text-ink"
              >
                <svg viewBox="0 0 24 24" className="h-5 w-5 shrink-0" fill="currentColor" aria-hidden="true">
                  <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 4a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7zm0 14.2a8 8 0 0 1-6-2.7c.1-2 4-3.1 6-3.1s5.9 1.1 6 3.1a8 8 0 0 1-6 2.7z" />
                </svg>
                <span className="hidden max-w-[14rem] truncate md:inline">{user.email}</span>
                <Chevron open={openKey === "account"} />
              </button>
              {openKey === "account" && (
                <div className="absolute right-0 top-full mt-1 w-64 rounded-panel border border-line bg-paper p-3 shadow-lg shadow-black/40">
                  <p className="truncate text-sm font-semibold">{user.fullName}</p>
                  <p className="truncate text-xs text-steel">{user.email}</p>
                  <p className="mt-1 text-xs text-steel">{roleText}</p>
                  <Button variant="secondary" className="mt-3 w-full" onClick={() => void logout()}>
                    Đăng xuất
                  </Button>
                </div>
              )}
            </div>

            {/* Màn hình nhỏ: nút mở menu */}
            <Button variant="secondary" className="lg:hidden" aria-expanded={mobileOpen} aria-controls="mobile-menu" onClick={() => setMobileOpen((v) => !v)}>
              {mobileOpen ? "Đóng" : "Menu"}
            </Button>
          </div>
        </div>
      </header>

      {mobileOpen && (
        <div id="mobile-menu" className="fixed inset-x-0 bottom-0 top-16 z-30 overflow-y-auto bg-chalk px-3 pb-6 lg:hidden">
          <nav aria-label="Điều hướng chính (điện thoại)">
            {groups.map((g) => (
              <div key={g.title} className="mt-5 first:mt-3">
                <p className="px-3 pb-1 text-xs font-semibold uppercase tracking-wide text-steel">{g.title}</p>
                <ul>{g.items.map(dropdownLink)}</ul>
              </div>
            ))}
          </nav>
        </div>
      )}

      <main id="main" className="mx-auto w-full max-w-7xl min-w-0 flex-1 px-4 py-8 sm:px-6 2xl:max-w-[90rem]">
        {children}
      </main>

      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-x-6 gap-y-1 px-4 py-4 text-xs text-steel sm:px-6 2xl:max-w-[90rem]">
          <span>GMS · Hệ thống quản lý phòng tập</span>
          <span>Đăng nhập với vai trò: {roleText}</span>
        </div>
      </footer>
    </div>
  );
}
