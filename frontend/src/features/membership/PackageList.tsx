"use client";

import Link from "next/link";
import { useState } from "react";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { listPackages } from "./api";
import { ErrorBlock, LoadingBlock } from "./Feedback";
import { PACKAGE_TYPE_LABELS, PEAK_TYPE_LABELS, labelOf } from "./labels";
import { formatVnd, packageSummary } from "./format";
import { Pagination } from "./Pagination";
import { StatusBadge } from "./StatusBadge";
import type { PackageItem } from "./types";
import { useLoad } from "./useLoad";

const PAGE_SIZE = 12;

function PackageCard({ p }: { p: PackageItem }) {
  return (
    <li className="panel flex flex-col p-5">
      <div className="mb-2 flex flex-wrap gap-2">
        <StatusBadge tone="neutral">{labelOf(PACKAGE_TYPE_LABELS, p.packageType)}</StatusBadge>
        <StatusBadge tone="neutral">{labelOf(PEAK_TYPE_LABELS, p.peakType)}</StatusBadge>
      </div>
      <h2 className="text-lg">{p.name}</h2>
      {p.description && <p className="mt-1 text-sm text-steel">{p.description}</p>}
      <p className="mt-3 text-2xl font-semibold">{formatVnd(p.price)}</p>
      <p className="mt-1 text-sm">{packageSummary(p)}</p>
      <div className="mt-auto pt-4">
        <Link href={`/packages/${p.id}`} className="btn-secondary" aria-label={`Xem chi tiết ${p.name}`}>
          Xem chi tiết
        </Link>
      </div>
    </li>
  );
}

export function PackageList() {
  const [page, setPage] = useState(0);
  const state = useLoad((signal) => listPackages(page, PAGE_SIZE, { signal }), [page]);

  return (
    <div>
      <PageHeader title="Gói tập" description="Các gói đang mở bán. Chọn một gói để xem chi tiết và đăng ký." />

      {state.status === "loading" && !state.data && <LoadingBlock text="Đang tải danh sách gói…" />}
      {state.status === "error" && <ErrorBlock error={state.error} onRetry={state.reload} />}

      {state.data && state.data.content.length === 0 && (
        <EmptyState title="Chưa có gói tập nào đang mở bán">Liên hệ quản trị viên hoặc lễ tân để được tư vấn gói phù hợp.</EmptyState>
      )}

      {state.data && state.data.content.length > 0 && (
        <>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {state.data.content.map((p) => (
              <PackageCard key={p.id} p={p} />
            ))}
          </ul>
          <Pagination page={state.data.page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
