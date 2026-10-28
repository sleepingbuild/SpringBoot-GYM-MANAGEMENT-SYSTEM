"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import type { Profile } from "@/lib/types";
import { getMyProfile, updateMyProfile } from "./api";
import { type ProfileFormValues, toRequest } from "./toRequest";

function toForm(p: Profile): ProfileFormValues {
  return {
    dateOfBirth: p.dateOfBirth ?? "",
    weightKg: p.weightKg?.toString() ?? "",
    heightCm: p.heightCm?.toString() ?? "",
    goal: p.goal ?? "",
  };
}

export function ProfileForm() {
  const toast = useToast();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [values, setValues] = useState<ProfileFormValues>({ dateOfBirth: "", weightKg: "", heightCm: "", goal: "" });
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getMyProfile()
      .then((p) => {
        if (cancelled) return;
        setProfile(p);
        setValues(toForm(p));
      })
      .catch((e) => !cancelled && setLoadError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaveError(null);
    setSaving(true);
    try {
      const updated = await updateMyProfile(toRequest(values));
      setProfile(updated);
      setValues(toForm(updated));
      toast.success("Đã lưu hồ sơ.");
    } catch (err) {
      setSaveError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  const set = (k: keyof ProfileFormValues) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setValues((v) => ({ ...v, [k]: e.target.value }));

  if (loadError) return <Alert tone="error">{loadError}</Alert>;
  if (!profile) {
    return (
      <div className="flex justify-center py-16 text-steel" role="status" aria-label="Đang tải hồ sơ">
        <Spinner />
      </div>
    );
  }

  return (
    <div>
      <PageHeader title="Hồ sơ cá nhân" description="Thông tin này giúp huấn luyện viên theo dõi tiến độ tập của bạn." />
      <form onSubmit={onSubmit} className="panel max-w-xl space-y-4 p-5" noValidate>
        {saveError && <Alert tone="error">{saveError}</Alert>}

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <p className="text-xs text-steel">Họ tên</p>
            <p className="text-sm font-medium">{profile.fullName}</p>
          </div>
          <div>
            <p className="text-xs text-steel">Email</p>
            <p className="text-sm font-medium">{profile.email}</p>
          </div>
        </div>

        <Field
          label="Ngày sinh"
          type="date"
          value={values.dateOfBirth}
          onChange={set("dateOfBirth")}
          hint={profile.age !== null ? `Hiện ${profile.age} tuổi. Cần từ 18 tuổi trở lên tính đến ngày đăng ký.` : "Cần từ 18 tuổi trở lên tính đến ngày đăng ký."}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Cân nặng (kg)" inputMode="decimal" value={values.weightKg} onChange={set("weightKg")} />
          <Field label="Chiều cao (cm)" inputMode="decimal" value={values.heightCm} onChange={set("heightCm")} />
        </div>
        <Field label="Mục tiêu" maxLength={255} value={values.goal} onChange={set("goal")} hint="Ví dụ: tăng cơ, giảm mỡ, tăng sức bền." />

        <Button type="submit" loading={saving}>
          Lưu thay đổi
        </Button>
      </form>
    </div>
  );
}
