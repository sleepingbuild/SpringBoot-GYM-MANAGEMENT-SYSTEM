import { describe, expect, it } from "vitest";
import { describeScan } from "./scanView";
import type { FaceAttendanceResult } from "./types";

function result(over: Partial<FaceAttendanceResult> = {}): FaceAttendanceResult {
  return {
    matchedUserId: "u1",
    matchedUserName: "An",
    action: "CHECK_IN",
    targetType: "GYM_CHECK_IN",
    scanTime: "2026-10-25T18:00:00",
    bookingLinked: false,
    duplicateScan: false,
    ...over,
  };
}

describe("describeScan", () => {
  it("vào phòng tập: thành công, ghi nhận giờ vào", () => {
    const v = describeScan(result());
    expect(v.tone).toBe("success");
    expect(v.title).toBe("An: đã ghi nhận giờ vào");
    expect(v.detail).toBe("Vào phòng tập");
  });

  it("check-out ghi nhận giờ ra", () => {
    expect(describeScan(result({ action: "CHECK_OUT" })).title).toBe("An: đã ghi nhận giờ ra");
  });

  it("buổi PT và chấm công có nhãn riêng, loại lạ giữ nguyên", () => {
    expect(describeScan(result({ targetType: "BOOKING" })).detail).toBe("Buổi tập với PT");
    expect(describeScan(result({ targetType: "STAFF_ATTENDANCE" })).detail).toBe("Chấm công");
    expect(describeScan(result({ targetType: "KHAC" })).detail).toBe("KHAC");
    expect(describeScan(result({ targetType: null })).detail).toBeNull();
  });

  it("quét lại quá sớm là cảnh báo, không phải lỗi", () => {
    const v = describeScan(result({ duplicateScan: true, action: "NONE" }));
    expect(v.tone).toBe("warning");
    expect(v.title).toContain("vừa quét xong");
  });

  it("PT không có buổi nào: hiện message của server", () => {
    const v = describeScan(result({ action: "NONE", message: "Không có buổi nào cần điểm danh", targetType: "BOOKING" }));
    expect(v.tone).toBe("info");
    expect(v.title).toBe("Không có buổi nào cần điểm danh");
    expect(v.detail).toBe("An");
  });

  it("PT quét ra sớm cần nhập lý do", () => {
    const v = describeScan(result({ action: "CHECK_OUT", targetType: "BOOKING", reasonRequired: true, earlyLeave: true }));
    expect(v.tone).toBe("warning");
    expect(v.detail).toContain("nhập lý do");
  });
});
