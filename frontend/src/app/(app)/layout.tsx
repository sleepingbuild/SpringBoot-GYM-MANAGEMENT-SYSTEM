import { AppShell } from "@/components/layout/AppShell";

/** Mọi trang trong nhóm (app) cần đăng nhập và dùng khung có menu. */
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
