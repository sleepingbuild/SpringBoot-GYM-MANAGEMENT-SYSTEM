import type { Branch } from "./types";

/** Các lựa chọn của ô chọn chi nhánh: dòng đầu là "không chọn" (giá trị rỗng), sau đó là từng chi nhánh theo thứ tự backend trả về. */
export function branchOptions(branches: Branch[], emptyLabel: string): { value: string; label: string }[] {
  return [{ value: "", label: emptyLabel }, ...branches.map((b) => ({ value: b.id, label: b.name }))];
}

/** Giá trị gửi lên backend: rỗng nghĩa là bỏ trống `branchId` (bán gói: không gắn chi nhánh; đổi gói: giữ chi nhánh cũ). */
export function branchIdOrUndefined(selected: string): string | undefined {
  return selected === "" ? undefined : selected;
}
