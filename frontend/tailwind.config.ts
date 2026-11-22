import type { Config } from "tailwindcss";

/**
 * Giao diện tối (theo ảnh mẫu IronFit). Giữ NGUYÊN TÊN token cũ để không phải sửa hàng trăm file,
 * chỉ đổi giá trị. Tên token là vai trò, không còn là mô tả màu thật:
 *   chalk = nền trang, paper = nền thẻ/panel, ink = chữ chính, steel = chữ phụ, line = viền,
 *   plate = xanh dương (đang chọn, liên kết, focus), accent = cam (MỘT nút hành động chính mỗi màn hình),
 *   rubber = nguy hiểm/hết hạn, moss = thành công/đang hoạt động, amber = cảnh báo/chờ.
 * Hai màu nhấn có phân vai: xanh dương cho trạng thái chọn và liên kết, cam cho hành động chính.
 * Chữ trên nền tối đều đạt tương phản tối thiểu 4,5:1 (đã tính trên chalk và paper).
 * Cam của nút chính được làm đậm hơn ảnh mẫu (#D14707 thay vì #FF6A00) vì chữ trắng trên cam sáng chỉ đạt 2,9:1.
 * Tint là màu đặc (không dùng độ trong suốt) để dùng được trên cả chalk lẫn paper.
 */
const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        chalk: "#0B0F19",
        paper: "#151B2C",
        raised: "#1B2336",
        ink: "#F4F5F7",
        steel: "#9AA3B5",
        line: { DEFAULT: "#262E42", strong: "#6B7794" },
        plate: {
          DEFAULT: "#5B9BFF",
          dark: "#2563EB",
          tint: "#14264A",
        },
        accent: { DEFAULT: "#D14707", dark: "#B53A05" },
        rubber: { DEFAULT: "#FF7B72", tint: "#2A1618" },
        moss: { DEFAULT: "#3FD07F", tint: "#12291D" },
        amber: { DEFAULT: "#F5B83D", tint: "#2B2210" },
      },
      // Viền không chỉ màu (border, divide-y, border-b...) mặc định theo màu `line`, nếu không sẽ ra viền xám sáng trên nền tối.
      borderColor: { DEFAULT: "#262E42" },
      fontFamily: {
        sans: ['"Be Vietnam Pro"', "system-ui", "-apple-system", "Segoe UI", "sans-serif"],
      },
      fontSize: {
        // thang chữ: nhỏ, thân, tiêu đề phụ, tiêu đề trang
        xs: ["0.8125rem", { lineHeight: "1.25rem" }],
        sm: ["0.9375rem", { lineHeight: "1.5rem" }],
        base: ["1rem", { lineHeight: "1.625rem" }],
        lg: ["1.1875rem", { lineHeight: "1.75rem" }],
        xl: ["1.5rem", { lineHeight: "2rem", letterSpacing: "-0.01em" }],
        "2xl": ["2rem", { lineHeight: "2.5rem", letterSpacing: "-0.02em" }],
      },
      borderRadius: {
        control: "8px",
        panel: "12px",
      },
    },
  },
  plugins: [],
};

export default config;
