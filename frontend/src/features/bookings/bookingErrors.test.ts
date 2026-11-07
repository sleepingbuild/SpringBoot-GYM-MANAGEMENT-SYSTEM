import { describe, expect, it } from "vitest";
import { ApiError } from "../../lib/api/errors";
import { BOOKING_ERROR_MESSAGES, bookingErrorMessage } from "./bookingErrors";

const REQUIRED_CODES = [
  "BOOKING_TOO_SOON",
  "BOOKING_WEEKLY_LIMIT_EXCEEDED",
  "BOOKING_SLOT_TAKEN",
  "BOOKING_DOUBLE_BOOKED",
  "BOOKING_OUT_OF_WORKING_HOURS",
  "MEMBERSHIP_EXPIRED",
  "MEMBERSHIP_NO_SESSION",
];

describe("bookingErrorMessage", () => {
  it("co cau tieng Viet cho du 7 ma loi Agent 1 yeu cau", () => {
    for (const code of REQUIRED_CODES) {
      expect(BOOKING_ERROR_MESSAGES[code], code).toBeTruthy();
    }
  });

  it("uu tien cau trong bang hon message tieng Anh cua server", () => {
    const err = new ApiError("conflict", 409, "Slot already taken", "BOOKING_SLOT_TAKEN");
    expect(bookingErrorMessage(err)).toBe(BOOKING_ERROR_MESSAGES.BOOKING_SLOT_TAKEN);
  });

  it("ma khong co trong bang thi dung message cua server", () => {
    const err = new ApiError("validation", 400, "Dữ liệu sai", "SOMETHING_ELSE");
    expect(bookingErrorMessage(err)).toBe("Dữ liệu sai");
  });

  it("loi khong phai ApiError thi dung cau mac dinh", () => {
    expect(bookingErrorMessage(new Error("boom"))).toBe("Đã có lỗi xảy ra. Thử lại sau.");
  });
});
