"use client";

import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { NAVIGATION } from "@/config/navigation";
import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";

export default function DashboardPage() {
  const { user } = useAuth();
  if (!user) return null;

  const items = NAVIGATION.flatMap((g) => g.items.map((i) => ({ ...i, group: g.title }))).filter(
    (i) => i.href !== "/dashboard" && hasAnyRole(user.roles, i.roles),
  );

  return (
    <div>
      <PageHeader title={`Xin chào, ${user.fullName}`} description="Các phân hệ dành cho tài khoản của bạn và tình trạng giao diện hiện tại." />
      <div className="panel overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-line bg-chalk text-xs uppercase tracking-wide text-steel">
            <tr>
              <th className="px-4 py-2 font-semibold">Phân hệ</th>
              <th className="hidden px-4 py-2 font-semibold sm:table-cell">Nhóm</th>
              <th className="hidden px-4 py-2 font-semibold sm:table-cell">Phụ trách</th>
              <th className="px-4 py-2 font-semibold">Giao diện</th>
            </tr>
          </thead>
          <tbody>
            {items.map((i) => (
              <tr key={i.href} className="border-b border-line last:border-0">
                <td className="px-4 py-3">
                  <Link href={i.href} className="font-medium text-plate underline-offset-2 hover:underline">
                    {i.label}
                  </Link>
                </td>
                <td className="hidden px-4 py-3 text-steel sm:table-cell">{i.group}</td>
                <td className="hidden px-4 py-3 text-steel sm:table-cell">{i.owner}</td>
                <td className="px-4 py-3">
                  {i.status === "ready" ? (
                    <span className="rounded bg-moss-tint px-2 py-0.5 text-xs font-medium text-moss">Đã có</span>
                  ) : (
                    <span className="rounded bg-chalk px-2 py-0.5 text-xs text-steel">Sắp có</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
