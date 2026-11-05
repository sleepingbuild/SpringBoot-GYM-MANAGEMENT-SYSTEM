import { describe, expect, it } from "vitest";
import {
  attendanceStatusLabel,
  attendanceStatusTone,
  checkInTypeLabel,
  formatDate,
  formatDateTime,
  methodLabel,
  toIsoDate,
} from "./format";

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

describe("formatDate", () => {
  it("đổi yyyy-MM-dd sang dd/MM/yyyy", () => {
    expect(formatDate("2026-10-25")).toBe("25/10/2026");
  });

  it("trả dấu gạch khi rỗng và giữ nguyên chuỗi lạ", () => {
    expect(formatDate(null)).toBe("—");
    expect(formatDate("25/10")).toBe("25/10");
  });
});

describe("toIsoDate", () => {
  it("ra đúng yyyy-MM-dd theo ngày của máy, có đệm số 0", () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe("2026-01-05");
    expect(toIsoDate(new Date(2026, 9, 25))).toBe("2026-10-25");
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

describe("attendanceStatus", () => {
  it("dịch đủ năm trạng thái và giữ nguyên trạng thái lạ", () => {
    expect(attendanceStatusLabel("ON_TIME")).toBe("Đúng giờ");
    expect(attendanceStatusLabel("LATE")).toBe("Đi muộn");
    expect(attendanceStatusLabel("LEFT_EARLY")).toBe("Về sớm");
    expect(attendanceStatusLabel("LATE_AND_LEFT_EARLY")).toBe("Đi muộn và về sớm");
    expect(attendanceStatusLabel("ABSENT")).toBe("Vắng");
    expect(attendanceStatusLabel("KHAC")).toBe("KHAC");
  });

  it("gán màu theo mức độ", () => {
    expect(attendanceStatusTone("ON_TIME")).toBe("success");
    expect(attendanceStatusTone("LATE")).toBe("warning");
    expect(attendanceStatusTone("LEFT_EARLY")).toBe("warning");
    expect(attendanceStatusTone("LATE_AND_LEFT_EARLY")).toBe("warning");
    expect(attendanceStatusTone("ABSENT")).toBe("error");
    expect(attendanceStatusTone("KHAC")).toBe("neutral");
  });
});
