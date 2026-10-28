import type { Config } from "tailwindcss";

/**
 * Bảng màu lấy từ phòng tập: phấn (nền), thép (chữ phụ), tạ đĩa xanh ngọc (màu chính),
 * cao su đỏ (cảnh báo/xoá). Chỉ có MỘT màu nhấn là "plate"; đừng thêm màu nhấn khác.
 */
const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        chalk: "#F2F4F1",
        paper: "#FFFFFF",
        ink: "#161B1E",
        steel: "#4A5A63",
        line: "#D9DED9",
        plate: {
          DEFAULT: "#0E5A6B",
          dark: "#0A4552",
          tint: "#E3EEF0",
        },
        rubber: { DEFAULT: "#B4372E", tint: "#F8E7E5" },
        moss: { DEFAULT: "#2F7D4F", tint: "#E4F1E9" },
        amber: { DEFAULT: "#8A5F00", tint: "#FBF1D6" },
      },
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
        control: "6px",
        panel: "10px",
      },
    },
  },
  plugins: [],
};

export default config;
