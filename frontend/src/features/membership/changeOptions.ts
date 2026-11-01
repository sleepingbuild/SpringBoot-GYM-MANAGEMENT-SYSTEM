import type { PackageItem } from "./types";

export type ChangeKind = "upgrade" | "downgrade";

/**
 * Backend: nâng cấp khi giá gói mới >= giá gói đang dùng, hạ cấp khi giá gói mới < giá gói đang dùng.
 * Giao diện chỉ phân loại để gợi ý; backend vẫn là nơi kiểm tra thật.
 */
export function classifyChange(currentPrice: number, candidatePrice: number): ChangeKind {
  return candidatePrice >= currentPrice ? "upgrade" : "downgrade";
}

export interface ChangeOptions {
  upgrades: PackageItem[];
  downgrades: PackageItem[];
}

/** Chia các gói đang bán thành nhóm nâng cấp và nhóm hạ cấp so với gói hiện tại; bỏ chính gói hiện tại. */
export function splitChangeOptions(currentPackageId: string, currentPrice: number, packages: PackageItem[]): ChangeOptions {
  const upgrades: PackageItem[] = [];
  const downgrades: PackageItem[] = [];
  for (const p of packages) {
    if (p.id === currentPackageId || !p.active) continue;
    (classifyChange(currentPrice, p.price) === "upgrade" ? upgrades : downgrades).push(p);
  }
  upgrades.sort((a, b) => a.price - b.price);
  downgrades.sort((a, b) => b.price - a.price);
  return { upgrades, downgrades };
}
