import { loadFaceModels } from "./faceModels";

export const MAX_IMAGE_BYTES = 10 * 1024 * 1024;

export type ImageResult =
  | { kind: "ok"; descriptor: number[] }
  | { kind: "no-face" }
  | { kind: "many-faces" }
  | { kind: "unreadable" }
  | { kind: "too-large" }
  | { kind: "not-image" };

export function imageResultMessage(result: Exclude<ImageResult, { kind: "ok" }>): string {
  switch (result.kind) {
    case "not-image":
      return "Tệp đã chọn không phải ảnh.";
    case "too-large":
      return "Ảnh lớn hơn 10 MB. Chọn ảnh nhỏ hơn.";
    case "unreadable":
      return "Không đọc được ảnh này. Thử ảnh JPG hoặc PNG.";
    case "many-faces":
      return "Ảnh có nhiều hơn một khuôn mặt. Chọn ảnh chỉ có một người.";
    case "no-face":
      return "Không thấy khuôn mặt trong ảnh. Chọn ảnh chụp thẳng mặt, đủ sáng.";
  }
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("image-load"));
    img.src = url;
  });
}

/**
 * Trích descriptor 128 số từ một ảnh ngay trong trình duyệt. Ảnh không được gửi lên máy chủ.
 * Ảnh có nhiều hơn một khuôn mặt bị từ chối để không đăng ký nhầm người.
 * Ném lỗi nếu không tải được mô hình; người gọi tự bắt.
 */
export async function extractDescriptorFromImage(file: File): Promise<ImageResult> {
  if (!file.type.startsWith("image/")) return { kind: "not-image" };
  if (file.size > MAX_IMAGE_BYTES) return { kind: "too-large" };

  const url = URL.createObjectURL(file);
  try {
    let img: HTMLImageElement;
    try {
      img = await loadImage(url);
    } catch {
      return { kind: "unreadable" };
    }
    const faceapi = await loadFaceModels();
    const faces = await faceapi
      .detectAllFaces(img, new faceapi.TinyFaceDetectorOptions({ inputSize: 512, scoreThreshold: 0.5 }))
      .withFaceLandmarks()
      .withFaceDescriptors();
    if (faces.length > 1) return { kind: "many-faces" };
    const first = faces[0];
    if (!first) return { kind: "no-face" };
    return { kind: "ok", descriptor: Array.from(first.descriptor) };
  } finally {
    URL.revokeObjectURL(url);
  }
}
