"use client";

import { listBranches } from "./api";
import { branchOptions } from "./branchOptions";
import { SelectField } from "./FormControls";
import { useLoad } from "./useLoad";

/**
 * Ô chọn chi nhánh (H4). Để trống vẫn hợp lệ: backend không bắt buộc `branchId`.
 * Không có chi nhánh nào thì ẩn ô; không tải được danh sách thì khoá ô và báo, không chặn thao tác chính.
 */
export function BranchSelect({
  value,
  onChange,
  emptyLabel = "Không chọn chi nhánh",
  hint,
}: {
  value: string;
  onChange: (branchId: string) => void;
  /** Nhãn của dòng "không chọn" (mặc định: bán gói, đăng ký gói). Đổi gói dùng "Giữ chi nhánh hiện tại". */
  emptyLabel?: string;
  hint?: string;
}) {
  const branches = useLoad((signal) => listBranches({ signal }), []);
  if (branches.status === "ready" && (branches.data ?? []).length === 0) return null;

  const failed = branches.status === "error";
  return (
    <SelectField
      label="Chi nhánh"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      options={branchOptions(branches.data ?? [], emptyLabel)}
      disabled={failed || (branches.status === "loading" && !branches.data)}
      hint={failed ? "Không tải được danh sách chi nhánh. Để trống vẫn thực hiện được." : hint}
    />
  );
}
