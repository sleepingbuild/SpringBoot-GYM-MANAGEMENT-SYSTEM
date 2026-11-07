import type { MemberSummary } from "./enrollTypes";

export interface ParsedMemberPage {
  members: MemberSummary[];
  totalElements: number;
}

function toMember(raw: unknown): MemberSummary | null {
  if (typeof raw !== "object" || raw === null) return null;
  const r = raw as Record<string, unknown>;
  if (typeof r.id !== "string" || r.id === "") return null;
  if (typeof r.fullName !== "string" || r.fullName.trim() === "") return null;
  return {
    id: r.id,
    fullName: r.fullName,
    phone: typeof r.phone === "string" && r.phone !== "" ? r.phone : null,
    email: typeof r.email === "string" && r.email !== "" ? r.email : null,
  };
}

/**
 * Chuẩn hóa response của GET /api/v1/users: PageResponse {content, totalElements, ...}.
 * Cũng nhận mảng thuần cho chắc; phần tử sai dạng bị bỏ.
 */
export function parseMemberPage(raw: unknown): ParsedMemberPage {
  let list: unknown[] = [];
  let total: number | null = null;
  if (Array.isArray(raw)) {
    list = raw;
  } else if (typeof raw === "object" && raw !== null) {
    const o = raw as Record<string, unknown>;
    if (Array.isArray(o.content)) list = o.content;
    if (typeof o.totalElements === "number") total = o.totalElements;
  }
  const members = list.map(toMember).filter((m): m is MemberSummary => m !== null);
  return { members, totalElements: total !== null && total >= members.length ? total : members.length };
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isUuid(value: string): boolean {
  return UUID_PATTERN.test(value.trim());
}
