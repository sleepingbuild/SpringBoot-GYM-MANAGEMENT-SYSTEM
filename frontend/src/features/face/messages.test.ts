import { describe, expect, it } from "vitest";
import { ApiError } from "../../lib/api/errors";
import { faceErrorMessage } from "./messages";

describe("faceErrorMessage", () => {
  it("dùng chữ riêng cho ba mã lỗi khuôn mặt", () => {
    expect(faceErrorMessage(new ApiError("other", 401, "x", "FACE_NOT_RECOGNIZED"))).toContain("Không nhận diện được");
    expect(faceErrorMessage(new ApiError("notFound", 404, "x", "FACE_PROFILE_NOT_FOUND"))).toContain("đăng ký khuôn mặt");
    expect(faceErrorMessage(new ApiError("validation", 400, "x", "FACE_DESCRIPTOR_INVALID"))).toContain("không hợp lệ");
  });

  it("các mã lỗi khác lấy message của server", () => {
    expect(faceErrorMessage(new ApiError("validation", 400, "Không có gói tập còn hiệu lực", "MEMBERSHIP_EXPIRED"))).toBe(
      "Không có gói tập còn hiệu lực",
    );
  });

  it("lỗi mạng và lỗi không rõ có chữ mặc định", () => {
    expect(faceErrorMessage(new ApiError("network", 0, "x"))).toContain("Không kết nối được máy chủ");
    expect(faceErrorMessage(new Error("boom"))).toBe("Đã có lỗi xảy ra. Thử lại sau.");
  });
});
