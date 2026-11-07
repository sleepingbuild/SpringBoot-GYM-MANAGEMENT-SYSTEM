import { describe, expect, it } from "vitest";
import { CameraSetupError, cameraErrorMessage } from "./cameraErrors";

describe("cameraErrorMessage", () => {
  it("giữ nguyên thông báo của lỗi do hook tạo ra", () => {
    expect(cameraErrorMessage(new CameraSetupError("insecure", "Chỉ chạy ở HTTPS"))).toBe("Chỉ chạy ở HTTPS");
  });

  it("dịch các lỗi quen thuộc của trình duyệt", () => {
    expect(cameraErrorMessage({ name: "NotAllowedError" })).toContain("chưa cho phép");
    expect(cameraErrorMessage({ name: "NotFoundError" })).toContain("Không tìm thấy camera");
    expect(cameraErrorMessage({ name: "NotReadableError" })).toContain("ứng dụng khác");
  });

  it("lỗi lạ có thông báo mặc định", () => {
    expect(cameraErrorMessage(new Error("?"))).toContain("Không bật được camera");
    expect(cameraErrorMessage(undefined)).toContain("Không bật được camera");
  });
});
