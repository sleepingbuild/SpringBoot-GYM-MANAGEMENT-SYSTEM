import Link from "next/link";

/** Bố cục chung cho đăng nhập/đăng ký: bên trái vạch kẻ làn sàn tập (chỉ màn hình rộng), bên phải là biểu mẫu. */
export function AuthLayout({ title, subtitle, children, footer }: { title: string; subtitle: string; children: React.ReactNode; footer: React.ReactNode }) {
  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
      <aside className="floor-lanes hidden flex-col justify-between p-10 text-ink lg:flex">
        <span className="flex items-center gap-2.5 text-lg font-bold tracking-tight">
          <span className="flex h-9 w-9 items-center justify-center rounded-control bg-gradient-to-b from-accent to-accent-dark text-white">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="M6.5 6.5v11M17.5 6.5v11M3.5 9v6M20.5 9v6M6.5 12h11" />
            </svg>
          </span>
          GMS
        </span>
        <div>
          <p className="max-w-sm text-2xl font-semibold leading-snug">Quản lý hội viên, lịch tập và điểm danh tại một nơi.</p>
          <p className="mt-3 max-w-sm text-sm text-steel">Gym Management System</p>
        </div>
      </aside>
      <main className="flex items-center justify-center px-5 py-10">
        <div className="w-full max-w-sm">
          <Link href="/" className="mb-8 block text-lg font-bold tracking-tight text-ink lg:hidden">
            GMS
          </Link>
          <h1 className="text-xl">{title}</h1>
          <p className="mb-6 mt-1 text-sm text-steel">{subtitle}</p>
          {children}
          <div className="mt-6 text-sm text-steel">{footer}</div>
        </div>
      </main>
    </div>
  );
}
