"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { ErrorBlock, LoadingBlock } from "@/features/membership/Feedback";
import { SelectField } from "@/features/membership/FormControls";
import { membershipErrorMessage } from "@/features/membership/messages";
import { Pagination } from "@/features/membership/Pagination";
import { StatusBadge } from "@/features/membership/StatusBadge";
import { useLoad } from "@/features/membership/useLoad";
import { useAuth } from "@/lib/auth/AuthContext";
import { ROLES, ROLE_LABELS, type Role } from "@/lib/roles";
import { useToast } from "@/lib/toast";
import { createStaff, listUsers, setUserRoles, setUserStatus, updateUser } from "./api";
import { STAFF_ROLES, toggleRole, validateStaffForm, type StaffFormErrors } from "./rules";
import type { AdminUser, StaffFormValues, StatusFilter, UserGroup } from "./types";

type Panel =
  | { kind: "none" }
  | { kind: "create" }
  | { kind: "edit"; user: AdminUser }
  | { kind: "roles"; user: AdminUser }
  | { kind: "lock"; user: AdminUser };

const GROUP_LABELS: Record<UserGroup, string> = { CUSTOMER: "Khách hàng", STAFF: "Nhân viên" };
const STATUS_OPTIONS = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Đang hoạt động" },
  { value: "INACTIVE", label: "Đã khoá" },
];
const ROLE_ORDER: Role[] = [...STAFF_ROLES, "MEMBER"].filter((r): r is Role => (ROLES as readonly string[]).includes(r));
const EMPTY_STAFF: StaffFormValues = { fullName: "", email: "", phone: "", password: "", roles: [] };

/** Quản trị tài khoản (Super Admin): khách hàng, nhân viên, gán vai trò, khoá/mở. Không tự đổi vai trò hay tự khoá mình. */
export function AdminUsersWorkspace() {
  const toast = useToast();
  const { user: me } = useAuth();
  const [group, setGroup] = useState<UserGroup>("CUSTOMER");
  const [search, setSearch] = useState("");
  const [appliedQ, setAppliedQ] = useState("");
  const [status, setStatus] = useState<StatusFilter>("");
  const [page, setPage] = useState(0);
  const [panel, setPanel] = useState<Panel>({ kind: "none" });
  const state = useLoad((signal) => listUsers(group, appliedQ, status, page, signal), [group, appliedQ, status, page]);

  function changeGroup(next: UserGroup) {
    setGroup(next);
    setPage(0);
    setPanel({ kind: "none" });
  }

  function done(message: string) {
    toast.success(message);
    setPanel({ kind: "none" });
    state.reload();
  }

  const isSelf = (u: AdminUser) => !!me && u.email.toLowerCase() === me.email.toLowerCase();

  return (
    <div>
      <PageHeader
        title="Người dùng"
        description="Quản lý khách hàng và nhân viên: sửa thông tin, gán vai trò, khoá hoặc mở tài khoản. Quyền có hiệu lực ngay ở lần thao tác kế tiếp của người đó."
        actions={group === "STAFF" ? <Button onClick={() => setPanel({ kind: "create" })}>Thêm nhân viên</Button> : undefined}
      />

      <div role="tablist" aria-label="Nhóm tài khoản" className="mb-4 flex gap-2">
        {(Object.keys(GROUP_LABELS) as UserGroup[]).map((g) => (
          <Button key={g} role="tab" aria-selected={group === g} variant={group === g ? "primary" : "secondary"} onClick={() => changeGroup(g)}>
            {GROUP_LABELS[g]}
          </Button>
        ))}
      </div>

      <form
        className="mb-4 grid gap-3 sm:grid-cols-[1fr_auto_auto] sm:items-end"
        onSubmit={(e) => {
          e.preventDefault();
          setAppliedQ(search);
          setPage(0);
        }}
      >
        <Field label="Tìm theo tên, email hoặc số điện thoại" value={search} onChange={(e) => setSearch(e.target.value)} maxLength={100} />
        <SelectField
          label="Trạng thái"
          value={status}
          options={STATUS_OPTIONS}
          onChange={(e) => {
            setStatus(e.target.value as StatusFilter);
            setPage(0);
          }}
        />
        <Button type="submit">Tìm</Button>
      </form>

      {panel.kind === "create" && <StaffCreatePanel onCancel={() => setPanel({ kind: "none" })} onDone={() => done("Đã tạo tài khoản nhân viên.")} />}
      {panel.kind === "edit" && <EditPanel user={panel.user} onCancel={() => setPanel({ kind: "none" })} onDone={() => done("Đã lưu thông tin.")} />}
      {panel.kind === "roles" && <RolesPanel user={panel.user} onCancel={() => setPanel({ kind: "none" })} onDone={() => done("Đã cập nhật vai trò.")} />}
      {panel.kind === "lock" && (
        <LockPanel user={panel.user} onCancel={() => setPanel({ kind: "none" })} onDone={() => done(panel.user.status === "ACTIVE" ? "Đã khoá tài khoản." : "Đã mở tài khoản.")} />
      )}

      {state.status === "loading" && !state.data && <LoadingBlock text="Đang tải danh sách…" />}
      {state.status === "error" && <ErrorBlock error={state.error} onRetry={state.reload} />}

      {state.data && state.data.content.length === 0 && <EmptyState title="Không có tài khoản nào">Thử đổi từ khoá hoặc bộ lọc trạng thái.</EmptyState>}

      {state.data && state.data.content.length > 0 && (
        <>
          <div className="panel overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  {["Họ tên", "Email", "Điện thoại", "Vai trò", "Trạng thái", "Thao tác"].map((h) => (
                    <th key={h} className="border-b px-3 py-2 font-medium text-steel">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {state.data.content.map((u) => {
                  const self = isSelf(u);
                  return (
                    <tr key={u.id}>
                      <td className="border-b px-3 py-2 align-top font-medium">
                        {u.fullName}
                        {self && <span className="block text-xs text-steel">Tài khoản của bạn</span>}
                      </td>
                      <td className="border-b px-3 py-2 align-top">{u.email}</td>
                      <td className="border-b px-3 py-2 align-top">{u.phone ?? "—"}</td>
                      <td className="border-b px-3 py-2 align-top">{u.roles.map((r) => ROLE_LABELS[r] ?? r).join(", ")}</td>
                      <td className="border-b px-3 py-2 align-top">
                        <StatusBadge tone={u.status === "ACTIVE" ? "success" : "neutral"}>{u.status === "ACTIVE" ? "Hoạt động" : "Đã khoá"}</StatusBadge>
                      </td>
                      <td className="border-b px-3 py-2 align-top">
                        <div className="flex flex-wrap gap-2">
                          <Button variant="secondary" onClick={() => setPanel({ kind: "edit", user: u })}>
                            Sửa
                          </Button>
                          <Button variant="secondary" disabled={self} onClick={() => setPanel({ kind: "roles", user: u })}>
                            Vai trò
                          </Button>
                          <Button variant="secondary" disabled={self} onClick={() => setPanel({ kind: "lock", user: u })}>
                            {u.status === "ACTIVE" ? "Khoá" : "Mở"}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <Pagination page={state.data.page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}

function PanelShell({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="panel mb-4 space-y-3 p-4" aria-label={title}>
      <h2 className="text-base font-semibold">{title}</h2>
      {children}
    </section>
  );
}

function RoleChecks({ selected, onToggle, only }: { selected: Role[]; onToggle: (r: Role) => void; only?: readonly Role[] }) {
  const options = only ? ROLE_ORDER.filter((r) => only.includes(r)) : ROLE_ORDER;
  return (
    <fieldset className="flex flex-wrap gap-4">
      <legend className="mb-1 text-sm font-medium">Vai trò</legend>
      {options.map((r) => (
        <label key={r} className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={selected.includes(r)} onChange={() => onToggle(r)} />
          {ROLE_LABELS[r]}
        </label>
      ))}
    </fieldset>
  );
}

function StaffCreatePanel({ onCancel, onDone }: { onCancel: () => void; onDone: () => void }) {
  const toast = useToast();
  const [values, setValues] = useState<StaffFormValues>(EMPTY_STAFF);
  const [errors, setErrors] = useState<StaffFormErrors>({});
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const found = validateStaffForm(values);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setBusy(true);
    try {
      await createStaff(values);
      onDone();
    } catch (err) {
      toast.error(membershipErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const set = (key: keyof StaffFormValues) => (e: React.ChangeEvent<HTMLInputElement>) => setValues((v) => ({ ...v, [key]: e.target.value }));

  return (
    <PanelShell title="Thêm nhân viên">
      <form onSubmit={submit} className="space-y-3" noValidate>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Họ tên" required value={values.fullName} onChange={set("fullName")} error={errors.fullName} />
          <Field label="Email" type="email" required value={values.email} onChange={set("email")} error={errors.email} />
          <Field label="Số điện thoại" value={values.phone} onChange={set("phone")} error={errors.phone} />
          <Field label="Mật khẩu" type="password" required autoComplete="new-password" value={values.password} onChange={set("password")} error={errors.password} hint="Từ 8 đến 72 ký tự." />
        </div>
        <RoleChecks selected={values.roles} only={STAFF_ROLES} onToggle={(r) => setValues((v) => ({ ...v, roles: toggleRole(v.roles, r) }))} />
        {errors.roles && <p className="text-xs text-rubber">{errors.roles}</p>}
        <div className="flex gap-2">
          <Button type="submit" loading={busy}>
            Tạo tài khoản
          </Button>
          <Button variant="secondary" disabled={busy} onClick={onCancel}>
            Huỷ
          </Button>
        </div>
      </form>
    </PanelShell>
  );
}

function EditPanel({ user, onCancel, onDone }: { user: AdminUser; onCancel: () => void; onDone: () => void }) {
  const toast = useToast();
  const [fullName, setFullName] = useState(user.fullName);
  const [phone, setPhone] = useState(user.phone ?? "");
  const [busy, setBusy] = useState(false);
  const nameError = fullName.trim() ? null : "Nhập họ tên.";

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (nameError) return;
    setBusy(true);
    try {
      await updateUser(user.id, fullName, phone);
      onDone();
    } catch (err) {
      toast.error(membershipErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <PanelShell title={`Sửa thông tin: ${user.email}`}>
      <form onSubmit={submit} className="space-y-3" noValidate>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Họ tên" required value={fullName} onChange={(e) => setFullName(e.target.value)} error={nameError} />
          <Field label="Số điện thoại" value={phone} maxLength={20} onChange={(e) => setPhone(e.target.value)} />
        </div>
        <div className="flex gap-2">
          <Button type="submit" loading={busy}>
            Lưu
          </Button>
          <Button variant="secondary" disabled={busy} onClick={onCancel}>
            Huỷ
          </Button>
        </div>
      </form>
    </PanelShell>
  );
}

function RolesPanel({ user, onCancel, onDone }: { user: AdminUser; onCancel: () => void; onDone: () => void }) {
  const toast = useToast();
  const [roles, setRoles] = useState<Role[]>(user.roles);
  const [busy, setBusy] = useState(false);

  async function save() {
    setBusy(true);
    try {
      await setUserRoles(user.id, roles);
      onDone();
    } catch (err) {
      toast.error(membershipErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <PanelShell title={`Gán vai trò: ${user.fullName}`}>
      <RoleChecks selected={roles} onToggle={(r) => setRoles((prev) => toggleRole(prev, r))} />
      <p className="text-xs text-steel">Hội viên không kết hợp với vai trò nhân viên. Người này sẽ có quyền mới ngay ở thao tác kế tiếp.</p>
      <div className="flex gap-2">
        <Button loading={busy} disabled={roles.length === 0} onClick={save}>
          Lưu vai trò
        </Button>
        <Button variant="secondary" disabled={busy} onClick={onCancel}>
          Huỷ
        </Button>
      </div>
    </PanelShell>
  );
}

function LockPanel({ user, onCancel, onDone }: { user: AdminUser; onCancel: () => void; onDone: () => void }) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const locking = user.status === "ACTIVE";

  async function confirm() {
    setBusy(true);
    try {
      await setUserStatus(user.id, locking ? "INACTIVE" : "ACTIVE");
      onDone();
    } catch (err) {
      toast.error(membershipErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <PanelShell title={locking ? `Khoá tài khoản ${user.email}?` : `Mở lại tài khoản ${user.email}?`}>
      <p className="text-sm">
        {locking ? "Người này sẽ không đăng nhập được, và phiên đang mở cũng bị từ chối ngay. Dữ liệu được giữ nguyên." : "Người này sẽ đăng nhập lại được bình thường."}
      </p>
      <div className="flex gap-2">
        <Button variant={locking ? "danger" : "primary"} loading={busy} onClick={confirm}>
          {locking ? "Khoá" : "Mở"}
        </Button>
        <Button variant="secondary" disabled={busy} onClick={onCancel}>
          Huỷ
        </Button>
      </div>
    </PanelShell>
  );
}
