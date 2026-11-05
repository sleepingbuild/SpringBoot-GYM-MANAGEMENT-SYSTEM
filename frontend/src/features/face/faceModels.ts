export type FaceApi = typeof import("@vladmandic/face-api");

/** Thư mục chứa file mô hình, nằm trong frontend/public/models/. */
const MODEL_URL = "/models";

let loading: Promise<FaceApi> | null = null;

/**
 * Tải thư viện và ba mô hình (phát hiện khuôn mặt, 68 điểm mốc, nhận diện) đúng một lần cho cả trang.
 * Dùng import động để thư viện (nặng, cần trình duyệt) không chạy khi dựng phía máy chủ.
 */
export function loadFaceModels(): Promise<FaceApi> {
  if (!loading) {
    loading = (async () => {
      const faceapi = await import("@vladmandic/face-api");
      // Phần khai báo kiểu của thư viện không liệt kê setBackend/ready dù chúng có thật lúc chạy.
      const tf = faceapi.tf as unknown as { setBackend(name: string): Promise<boolean>; ready(): Promise<void> };
      try {
        await tf.setBackend("webgl");
      } catch {
        // Không có WebGL: để thư viện tự chọn backend khác.
      }
      await tf.ready();
      await Promise.all([
        faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL),
        faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL),
        faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL),
      ]);
      return faceapi;
    })();
    loading.catch(() => {
      loading = null; // cho phép thử lại lần sau
    });
  }
  return loading;
}
