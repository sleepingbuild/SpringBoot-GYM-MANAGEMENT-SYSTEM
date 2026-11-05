import { describe, expect, it } from "vitest";
import { branchIdOrUndefined, branchOptions } from "./branchOptions";

describe("branchOptions", () => {
  it("luôn có dòng không chọn ở đầu với giá trị rỗng", () => {
    expect(branchOptions([], "Không chọn chi nhánh")).toEqual([{ value: "", label: "Không chọn chi nhánh" }]);
  });

  it("giữ nguyên thứ tự chi nhánh backend trả về", () => {
    const options = branchOptions(
      [
        { id: "b2", name: "Chi nhánh Hà Nội" },
        { id: "b1", name: "Chi nhánh Đà Nẵng" },
      ],
      "Giữ chi nhánh hiện tại",
    );
    expect(options.map((o) => o.value)).toEqual(["", "b2", "b1"]);
    expect(options[1].label).toBe("Chi nhánh Hà Nội");
  });
});

describe("branchIdOrUndefined", () => {
  it("rỗng thì bỏ trống", () => {
    expect(branchIdOrUndefined("")).toBeUndefined();
  });

  it("có giá trị thì gửi nguyên", () => {
    expect(branchIdOrUndefined("b1")).toBe("b1");
  });
});
