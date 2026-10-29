import { describe, expect, it } from "vitest";
import { checkInTypeLabel, formatDateTime, methodLabel } from "./format";

describe("formatDateTime", () => {
  it("định dạng LocalDateTime thành giờ rồi ngày, không đổi múi giờ", () => {
    expect(formatDateTime("2026-10-25T18:05:00")).toBe("18:05 25/10/2026");
  });

  it("bỏ qua phần giây lẻ", () => {
    expect(formatDateTime("2026-10-25T08:05:30.123456")).toBe("08:05 25/10/2026");
  });

  it("trả dấu gạch khi không có giá trị", () => {
    expect(formatDateTime(null)).toBe("—");
    expect(formatDateTime(undefined)).toBe("—");
    expect(formatDateTime("")).toBe("—");
  });

  it("giữ nguyên chuỗi không đúng định dạng", () => {
    expect(formatDateTime("hôm qua")).toBe("hôm qua");
  });
});

describe("checkInTypeLabel", () => {
  it("dịch hai loại đã biết và giữ nguyên loại lạ", () => {
    expect(checkInTypeLabel("GYM_VISIT")).toBe("Tập tự do");
    expect(checkInTypeLabel("PT_SESSION")).toBe("Buổi tập với PT");
    expect(checkInTypeLabel("KHAC")).toBe("KHAC");
  });
});

describe("methodLabel", () => {
  it("dịch FACE và MANUAL, rỗng thành dấu gạch, giữ nguyên giá trị lạ", () => {
    expect(methodLabel("FACE")).toBe("Khuôn mặt");
    expect(methodLabel("MANUAL")).toBe("Thủ công");
    expect(methodLabel(null)).toBe("—");
    expect(methodLabel("QR")).toBe("QR");
  });
});
