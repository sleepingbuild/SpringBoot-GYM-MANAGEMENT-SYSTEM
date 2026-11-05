import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/errors";
import { membershipErrorMessage } from "./messages";

describe("membershipErrorMessage", () => {
  it("backend chỉ gửi tên mã làm message thì dùng bảng tiếng Việt", () => {
    const err = new ApiError("notFound", 404, "PACKAGE_NOT_FOUND", "PACKAGE_NOT_FOUND");
    expect(membershipErrorMessage(err)).toBe("Không tìm thấy gói tập, hoặc gói đã ngừng bán.");
  });

  it("backend có lời giải thích riêng thì giữ nguyên", () => {
    const err = new ApiError("notFound", 404, "Bạn hiện không có gói tập nào đang hoạt động", "MEMBERSHIP_NOT_FOUND");
    expect(membershipErrorMessage(err)).toBe("Bạn hiện không có gói tập nào đang hoạt động");
  });

  it("đăng ký khi đang có gói chờ thanh toán", () => {
    const err = new ApiError("conflict", 409, "MEMBERSHIP_PENDING_EXISTS", "MEMBERSHIP_PENDING_EXISTS");
    expect(membershipErrorMessage(err)).toContain("chờ thanh toán");
  });

  it("mã hoàn tiền mới có lời giải thích", () => {
    const err = new ApiError("conflict", 409, "PAYMENT_NOT_REFUNDABLE", "PAYMENT_NOT_REFUNDABLE");
    expect(membershipErrorMessage(err)).toBe("Giao dịch này không thể hoàn tiền.");
  });

  it("mã lạ và không có message thì không lộ tên mã ra cho người dùng", () => {
    const err = new ApiError("other", 418, "SOMETHING_NEW", "SOMETHING_NEW");
    expect(membershipErrorMessage(err)).toBe("Đã có lỗi xảy ra. Thử lại sau.");
    expect(membershipErrorMessage(err, "Không tải được")).toBe("Không tải được");
  });

  it("lỗi mạng và hết phiên dùng thông báo chung của khung", () => {
    expect(membershipErrorMessage(new ApiError("network", 0, "x"))).toContain("Không kết nối được máy chủ");
    expect(membershipErrorMessage(new ApiError("session", 401, "x", "AUTH_TOKEN_EXPIRED"))).toContain("hết hạn");
  });

  it("lỗi không phải ApiError thì dùng dự phòng", () => {
    expect(membershipErrorMessage(new Error("boom"))).toBe("Đã có lỗi xảy ra. Thử lại sau.");
  });
});
