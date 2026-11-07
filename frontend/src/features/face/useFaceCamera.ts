"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { CameraSetupError, cameraErrorMessage } from "./cameraErrors";
import { loadFaceModels } from "./faceModels";
import type { FaceApi } from "./faceModels";

export type CameraStatus = "loading-models" | "starting-camera" | "ready" | "error";

export type CaptureResult =
  | { kind: "ok"; descriptor: number[] }
  | { kind: "no-face" }
  | { kind: "too-small" };

export interface FaceCamera {
  videoRef: React.RefObject<HTMLVideoElement>;
  status: CameraStatus;
  errorMessage: string | null;
  /** Chụp một khung hình và trích descriptor 128 số. */
  captureDescriptor: () => Promise<CaptureResult>;
  /** Bật lại camera sau lỗi. */
  restart: () => void;
}

/**
 * Mở camera trước, tải mô hình nhận diện và trích descriptor 128 chiều ngay trong trình duyệt.
 * @param minFaceRatio chiều rộng tối thiểu của khuôn mặt so với khung hình (0 đến 1); nhỏ hơn thì báo "too-small".
 */
export function useFaceCamera(minFaceRatio = 0.2): FaceCamera {
  const videoRef = useRef<HTMLVideoElement>(null);
  const faceapiRef = useRef<FaceApi | null>(null);
  const [status, setStatus] = useState<CameraStatus>("loading-models");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let stream: MediaStream | null = null;
    setStatus("loading-models");
    setErrorMessage(null);

    const start = async () => {
      try {
        try {
          faceapiRef.current = await loadFaceModels();
        } catch {
          throw new CameraSetupError("models", "Không tải được mô hình nhận diện khuôn mặt. Kiểm tra thư mục public/models.");
        }
        if (cancelled) return;

        setStatus("starting-camera");
        if (!navigator.mediaDevices?.getUserMedia) {
          throw new CameraSetupError("insecure", "Trình duyệt chỉ cho dùng camera ở localhost hoặc HTTPS.");
        }
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: "user", width: { ideal: 640 }, height: { ideal: 480 } },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }

        const video = videoRef.current;
        if (!video) return;
        video.srcObject = stream;
        await video.play();
        if (!cancelled) setStatus("ready");
      } catch (e) {
        if (!cancelled) {
          setErrorMessage(cameraErrorMessage(e));
          setStatus("error");
        }
      }
    };
    void start();

    return () => {
      cancelled = true;
      stream?.getTracks().forEach((t) => t.stop());
    };
  }, [attempt]);

  const captureDescriptor = useCallback(async (): Promise<CaptureResult> => {
    const faceapi = faceapiRef.current;
    const video = videoRef.current;
    if (!faceapi || !video || video.readyState < 2 || video.videoWidth === 0) return { kind: "no-face" };

    const result = await faceapi
      .detectSingleFace(video, new faceapi.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.5 }))
      .withFaceLandmarks()
      .withFaceDescriptor();
    if (!result) return { kind: "no-face" };
    if (result.detection.box.width / video.videoWidth < minFaceRatio) return { kind: "too-small" };
    return { kind: "ok", descriptor: Array.from(result.descriptor) };
  }, [minFaceRatio]);

  const restart = useCallback(() => setAttempt((n) => n + 1), []);

  return { videoRef, status, errorMessage, captureDescriptor, restart };
}
