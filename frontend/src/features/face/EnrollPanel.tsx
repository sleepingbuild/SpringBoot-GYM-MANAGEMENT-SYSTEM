"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { formatDateTime } from "../attendance/format";
import { deleteFaceProfile, errorTone, getFaceProfile, registerFaceFor } from "./enrollApi";
import type { FaceProfileInfo, MemberSummary } from "./enrollTypes";
import { FaceCameraView } from "./FaceCameraView";
import { extractDescriptorFromImage, imageResultMessage } from "./faceImage";
import { faceErrorMessage } from "./messages";
import { useFaceCamera } from "./useFaceCamera";

type ProfileState =
  | { status: "loading" }
  | { status: "registered"; info: FaceProfileInfo }
  | { status: "none" }
  | { status: "error"; message: string; tone: "warning" | "error" };

type Mode = "image" | "camera";
type Notice = { tone: "success" | "error" | "warning"; text: string };

/** Chụp một khung hình từ camera và trích descriptor. Chỉ được gắn khi chọn chế độ camera. */
function CameraCapture({ onCaptured, disabled }: { onCaptured: (descriptor: number[]) => void; disabled: boolean }) {
  const camera = useFaceCamera(0.25);
  const [busy, setBusy] = useState(false);
  const [hint, setHint] = useState<string | null>(null);

  async function capture() {
    setBusy(true);
    setHint(null);
    try {
      const r = await camera.captureDescriptor();
      if (r.kind === "ok") onCaptured(r.descriptor);
      else if (r.kind === "too-small") setHint("Khuôn mặt quá nhỏ. Đứng gần camera hơn rồi chụp lại.");
      else setHint("Không thấy khuôn mặt. Nhìn thẳng vào camera, đủ sáng, rồi chụp lại.");
    } catch {
      setHint("Không trích được khuôn mặt. Thử lại.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-3">
      <FaceCameraView camera={camera} />
      {hint && <Alert tone="warning">{hint}</Alert>}
      <Button onClick={capture} loading={busy} disabled={disabled || camera.status !== "ready"}>
        Chụp khuôn mặt
      </Button>
    </div>
  );
}

/** Xem, đăng ký (ghi đè) và xóa hồ sơ khuôn mặt của một hội viên. Gắn với key={member.id} để đổi người là đặt lại. */
export function EnrollPanel({ member }: { member: MemberSummary }) {
  const [profile, setProfile] = useState<ProfileState>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);
  const [mode, setMode] = useState<Mode>("image");
  const [pending, setPending] = useState<number[] | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [extracting, setExtracting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [notice, setNotice] = useState<Notice | null>(null);

  useEffect(() => {
    let cancelled = false;
    setProfile({ status: "loading" });
    getFaceProfile(member.id)
      .then((info) => {
        if (!cancelled) setProfile(info ? { status: "registered", info } : { status: "none" });
      })
      .catch((e: unknown) => {
        if (!cancelled) setProfile({ status: "error", message: faceErrorMessage(e), tone: errorTone(e) });
      });
    return () => {
      cancelled = true;
    };
  }, [member.id, reloadKey]);

  useEffect(() => {
    return () => {
      if (preview) URL.revokeObjectURL(preview);
    };
  }, [preview]);

  function switchMode(next: Mode) {
    setMode(next);
    setPending(null);
    setPreview(null);
    setNotice(null);
  }

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const input = e.target;
    const file = input.files?.[0];
    if (!file) return;
    setPending(null);
    setNotice(null);
    setPreview(URL.createObjectURL(file));
    setExtracting(true);
    try {
      const result = await extractDescriptorFromImage(file);
      if (result.kind === "ok") setPending(result.descriptor);
      else setNotice({ tone: "error", text: imageResultMessage(result) });
    } catch {
      setNotice({ tone: "error", text: "Không tải được mô hình nhận diện khuôn mặt. Kiểm tra thư mục public/models." });
    } finally {
      setExtracting(false);
      input.value = "";
    }
  }

  async function save() {
    if (!pending) return;
    if (profile.status === "registered" && !window.confirm(`${member.fullName} đã có hồ sơ khuôn mặt. Ghi đè bằng dữ liệu mới?`)) return;
    setSaving(true);
    setNotice(null);
    try {
      await registerFaceFor(member.id, pending);
      setPending(null);
      setPreview(null);
      setNotice({ tone: "success", text: "Đã lưu hồ sơ khuôn mặt." });
      setReloadKey((k) => k + 1);
    } catch (e) {
      setNotice({ tone: errorTone(e), text: faceErrorMessage(e) });
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!window.confirm(`Xóa hồ sơ khuôn mặt của ${member.fullName}? Người này sẽ không điểm danh bằng khuôn mặt được nữa.`)) return;
    setRemoving(true);
    setNotice(null);
    try {
      await deleteFaceProfile(member.id);
      setNotice({ tone: "success", text: "Đã xóa hồ sơ khuôn mặt." });
      setReloadKey((k) => k + 1);
    } catch (e) {
      setNotice({ tone: errorTone(e), text: faceErrorMessage(e) });
    } finally {
      setRemoving(false);
    }
  }

  const busy = extracting || saving || removing;

  return (
    <div className="space-y-4">
      <div className="panel px-4 py-3">
        <p className="font-semibold">{member.fullName}</p>
        <p className="text-sm text-steel">{[member.phone, member.email].filter(Boolean).join(" · ") || member.id}</p>
      </div>

      {profile.status === "loading" && (
        <div className="flex items-center gap-2 text-sm text-steel" role="status">
          <Spinner className="h-4 w-4" />
          Đang kiểm tra hồ sơ khuôn mặt...
        </div>
      )}
      {profile.status === "error" && (
        <div className="space-y-3">
          <Alert tone={profile.tone} title="Không xem được hồ sơ khuôn mặt">
            {profile.message}
          </Alert>
          <Button variant="secondary" onClick={() => setReloadKey((k) => k + 1)}>
            Thử lại
          </Button>
        </div>
      )}
      {profile.status === "none" && <Alert tone="info">Hội viên này chưa đăng ký khuôn mặt.</Alert>}
      {profile.status === "registered" && (
        <Alert tone="success" title="Đã có hồ sơ khuôn mặt">
          Đăng ký lần đầu lúc {formatDateTime(profile.info.createdAt)}. Đăng ký lại sẽ ghi đè hồ sơ cũ.
        </Alert>
      )}

      {notice && <Alert tone={notice.tone}>{notice.text}</Alert>}

      {(profile.status === "none" || profile.status === "registered") && (
        <div className="panel space-y-4 px-4 py-4">
          <div className="flex flex-wrap gap-2">
            <Button variant={mode === "image" ? "primary" : "secondary"} onClick={() => switchMode("image")} disabled={busy}>
              Chọn ảnh
            </Button>
            <Button variant={mode === "camera" ? "primary" : "secondary"} onClick={() => switchMode("camera")} disabled={busy}>
              Dùng camera
            </Button>
          </div>

          {mode === "image" && (
            <div className="space-y-3">
              <label htmlFor="face-image" className="block text-sm font-semibold">
                Ảnh chụp thẳng mặt (chỉ một người)
              </label>
              <input id="face-image" type="file" accept="image/*" onChange={onFile} disabled={busy} className="block text-sm" />
              <p className="text-xs text-steel">Ảnh chỉ được xử lý trong trình duyệt, không gửi lên máy chủ.</p>
              {extracting && (
                <div className="flex items-center gap-2 text-sm text-steel" role="status">
                  <Spinner className="h-4 w-4" />
                  Đang trích khuôn mặt...
                </div>
              )}
              {preview && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={preview} alt="Ảnh đã chọn" className="max-h-56 rounded-control" />
              )}
            </div>
          )}

          {mode === "camera" && <CameraCapture onCaptured={setPending} disabled={busy} />}

          {pending && (
            <Alert tone="info" title="Đã trích xong khuôn mặt">
              Kiểm tra đúng người rồi bấm lưu.
            </Alert>
          )}

          <div className="flex flex-wrap gap-2">
            <Button onClick={save} loading={saving} disabled={!pending || busy}>
              {profile.status === "registered" ? "Ghi đè hồ sơ" : "Lưu hồ sơ"}
            </Button>
            {profile.status === "registered" && (
              <Button variant="danger" onClick={remove} loading={removing} disabled={busy}>
                Xóa hồ sơ
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
