import { describe, expect, it } from "vitest";
import { bookingStatusTone } from "./statusTone";

describe("bookingStatusTone", () => {
  it("cho PT xac nhan: vang; da xac nhan: xanh duong; hoan thanh: xanh la", () => {
    expect(bookingStatusTone("PENDING")).toBe("warning");
    expect(bookingStatusTone("CONFIRMED")).toBe("info");
    expect(bookingStatusTone("COMPLETED")).toBe("success");
  });

  it("hoc vien vang: do; huy muon: vang", () => {
    expect(bookingStatusTone("NO_SHOW")).toBe("danger");
    expect(bookingStatusTone("LATE_CANCELLED")).toBe("warning");
  });

  it("huy thuong, PT vang va trang thai la: trung tinh", () => {
    expect(bookingStatusTone("CANCELLED")).toBe("neutral");
    expect(bookingStatusTone("PT_NO_SHOW")).toBe("neutral");
    expect(bookingStatusTone("SOMETHING_ELSE")).toBe("neutral");
  });
});
