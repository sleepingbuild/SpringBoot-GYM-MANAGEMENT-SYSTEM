# DESIGN_SYSTEM.md — Hệ thống Thiết kế Chung (UI/UX)

> Áp dụng thống nhất cho toàn bộ frontend của GMS (Landing Page, Member Portal, Admin/Receptionist Dashboard, PT App) — tham khảo ngôn ngữ thiết kế của các thương hiệu phòng gym/SaaS thể thao chuyên nghiệp (Planet Fitness, Gold's Gym, các dashboard fitness SaaS hiện đại) nhưng xây dựng bộ nhận diện **riêng, nguyên bản** cho GMS, không sao chép logo/màu thương hiệu của bên thứ ba.

## 1. Định hướng thương hiệu

**Tính cách thương hiệu:** Năng lượng – Kỷ luật – Đáng tin cậy – Hiện đại.
Landing page cần truyền cảm hứng vận động (bold, tương phản cao, hình ảnh động lực); phần Dashboard vận hành (Admin/Lễ tân/PT) cần **rõ ràng, mật độ thông tin cao, giảm mỏi mắt khi dùng nhiều giờ** — vì vậy hệ thống tách 2 "chế độ" thị giác dùng chung 1 bộ token màu.

## 2. Bảng màu (Color Palette)

### Màu thương hiệu chính
| Token | Hex | Vai trò |
|---|---|---|
| `--color-primary-900` | `#0B0F19` | Nền tối chủ đạo (Dashboard, Header, Footer) — than chì gần đen, sang trọng |
| `--color-primary-700` | `#151B2C` | Nền surface phụ (card trên nền tối) |
| `--color-accent` | `#FF4B2B` | **Màu nhấn năng lượng** (cam-đỏ) — CTA chính, nút "Đăng ký ngay", tag trạng thái quan trọng |
| `--color-accent-hover` | `#E8391B` | Hover state của accent |
| `--color-secondary` | `#00D9A3` | Xanh mint neon — dùng cho trạng thái thành công, số liệu tăng trưởng, tag "ACTIVE" |
| `--color-highlight` | `#FFC63A` | Vàng năng lượng — dùng có tiết chế cho huy hiệu/khuyến mãi, không dùng làm nền lớn |

### Màu trung tính
| Token | Hex | Vai trò |
|---|---|---|
| `--color-white` | `#FFFFFF` | Nền sáng, chữ trên nền tối |
| `--color-gray-100` | `#F4F5F7` | Nền light-mode chủ đạo |
| `--color-gray-300` | `#D8DBE2` | Border, divider |
| `--color-gray-500` | `#8A8F9C` | Text phụ, placeholder |
| `--color-gray-700` | `#4B505C` | Text phụ trên nền sáng |
| `--color-gray-900` | `#1B1F27` | Text chính trên nền sáng |

### Màu trạng thái hệ thống
| Token | Hex | Ý nghĩa |
|---|---|---|
| `--color-success` | `#00D9A3` | ACTIVE, SUCCESS, thanh toán thành công |
| `--color-warning` | `#FFC63A` | PENDING, sắp hết hạn (≤7 ngày) |
| `--color-danger` | `#FF4B2B` | EXPIRED, DENIED, lỗi |
| `--color-info` | `#3E8BFF` | FROZEN, thông tin trung tính |

**Nguyên tắc phối màu:** nền tối (`primary-900/700`) dùng cho khu vực điều hướng và Dashboard vận hành để giảm chói khi nhìn màn hình lâu; nền sáng (`gray-100/white`) dùng cho Landing Page và các form nhập liệu cần độ rõ nét cao. Màu accent cam-đỏ **chỉ dùng cho hành động chính** (1 CTA nổi bật/màn hình), tránh lạm dụng gây rối mắt.

## 3. Typography

| Vai trò | Font đề xuất | Ghi chú |
|---|---|---|
| Display / Heading (Landing) | **Urbanist** hoặc **Sora** (Bold/ExtraBold) | Nét vuông, hiện đại, tạo cảm giác mạnh mẽ |
| Body / UI text | **Inter** | Đọc tốt ở kích thước nhỏ, hỗ trợ tiếng Việt có dấu đầy đủ |
| Số liệu Dashboard (giá, thống kê) | **Inter** Tabular Numbers / **JetBrains Mono** cho bảng số liệu | Căn số đều cột, dễ so sánh |

**Type scale (rem, base 16px):**
| Token | Size | Line-height | Dùng cho |
|---|---|---|---|
| `--text-display` | 3.5rem (56px) | 1.1 | Hero headline Landing Page |
| `--text-h1` | 2.25rem (36px) | 1.2 | Tiêu đề trang |
| `--text-h2` | 1.75rem (28px) | 1.25 | Tiêu đề section/card lớn |
| `--text-h3` | 1.25rem (20px) | 1.3 | Tiêu đề card, modal |
| `--text-body` | 1rem (16px) | 1.5 | Nội dung chính |
| `--text-small` | 0.875rem (14px) | 1.4 | Label, caption, meta info |
| `--text-tiny` | 0.75rem (12px) | 1.3 | Badge, timestamp |

## 4. Spacing & Layout Grid

- Đơn vị cơ sở: **8px** (`--space-1: 4px`, `--space-2: 8px`, `--space-3: 12px`, `--space-4: 16px`, `--space-6: 24px`, `--space-8: 32px`, `--space-12: 48px`, `--space-16: 64px`).
- Grid Landing Page: 12 cột, max-width `1280px`, gutter `24px`.
- Grid Dashboard: sidebar cố định `260px` (thu gọn `72px`) + content area fluid, container padding `24px`.
- Bo góc (`border-radius`): `--radius-sm: 8px` (input, badge), `--radius-md: 12px` (card), `--radius-lg: 20px` (modal, hero card), `--radius-full: 9999px` (avatar, pill tag).
- Bóng đổ (`box-shadow`): dùng shadow mềm, độ mờ thấp trên nền sáng (`0 4px 16px rgba(11,15,25,0.08)`); trên nền tối dùng viền `1px solid rgba(255,255,255,0.06)` thay vì shadow.

## 5. Breakpoints (Responsive)

| Token | Width | Thiết bị |
|---|---|---|
| `--bp-sm` | 640px | Mobile lớn |
| `--bp-md` | 768px | Tablet |
| `--bp-lg` | 1024px | Laptop |
| `--bp-xl` | 1280px | Desktop |

Dashboard admin/lễ tân ưu tiên thiết kế **desktop-first** (nghiệp vụ nhập liệu nhiều); Member Portal & Landing Page thiết kế **mobile-first** (hội viên chủ yếu xem lịch/gói trên điện thoại).

## 6. Component Guidelines

### Button
- **Primary:** nền `--color-accent`, chữ trắng, bo `--radius-sm`, padding `12px 24px`, hover đổ đậm hơn + nâng nhẹ (`translateY(-1px)`).
- **Secondary (outline):** viền `1.5px` màu `--color-gray-300` (light) / `rgba(255,255,255,0.16)` (dark), nền trong suốt.
- **Destructive:** nền `--color-danger`, dùng cho huỷ gói/xoá dữ liệu — luôn kèm confirm dialog.

### Card (Gói tập / Hội viên / Lớp Group X)
- Nền `--color-white` (light) hoặc `--color-primary-700` (dark), bo `--radius-md`, padding `24px`.
- Card gói tập nổi bật ("Best Value") dùng viền gradient accent + badge góc trên.

### Badge trạng thái
| Trạng thái | Màu nền | Màu chữ |
|---|---|---|
| ACTIVE | `--color-success` (10% opacity) | `--color-success` |
| EXPIRED | `--color-danger` (10% opacity) | `--color-danger` |
| FROZEN | `--color-info` (10% opacity) | `--color-info` |
| PENDING | `--color-warning` (15% opacity) | `#8A5E00` |

### Bảng dữ liệu (Dashboard Admin)
- Header sticky, hàng zebra rất nhạt (`--color-gray-100` xen kẽ trắng), hover row highlight nhẹ bằng accent 5% opacity.
- Số liệu tiền tệ căn phải, định dạng `#,###₫`.

### Form nhập liệu
- Input height `44px`, border `--color-gray-300`, focus ring `2px` màu accent với opacity 30%.
- Validation lỗi: viền đỏ + text lỗi `--text-small` màu `--color-danger` ngay dưới field.

## 7. Iconography & Hình ảnh

- Icon set: dùng bộ outline nhất quán (ví dụ Lucide/Phosphor Icons), stroke-width đồng nhất `1.5–2px`.
- Ảnh Landing Page: ảnh thật/high-contrast người tập luyện, ưu tiên tông màu ám cam-đen để đồng bộ overlay với `--color-primary-900` + `--color-accent` (gradient overlay 60–80% từ dưới lên khi đặt text lên ảnh).
- Tuyệt đối không dùng ảnh/logo có bản quyền của thương hiệu gym khác — chỉ dùng làm nguồn cảm hứng phong cách (bold, năng lượng, tương phản cao), không sao chép nhận diện.

## 8. Cấu trúc trang tham khảo

### Landing Page
```
[Header: logo + menu + CTA "Đăng ký"]
[Hero: headline lớn + ảnh động lực + CTA accent nổi bật]
[Section: Gói tập nổi bật — 3 card giá song song, card giữa highlight]
[Section: Tiện ích phòng gym — icon grid]
[Section: Lịch Group X tiêu biểu]
[Section: Đánh giá hội viên]
[Footer: liên hệ, chi nhánh, social]
```

### Member Dashboard
```
[Sidebar tối: Trang chủ | Gói tập của tôi | Lịch PT | Group X | Lịch sử Check-in]
[Content: thẻ tóm tắt gói tập (progress bar số buổi/ngày còn lại) + lịch tuần dạng calendar grid]
```

### Admin/Receptionist Dashboard
```
[Sidebar tối, thu gọn được]
[Topbar: chọn chi nhánh + tìm kiếm hội viên nhanh + avatar]
[Content: widget KPI (doanh thu hôm nay, check-in hôm nay, gói sắp hết hạn) + bảng dữ liệu chính]
```

## 9. Design Tokens (CSS Variables — dùng chung cho mọi frontend framework)

```css
:root {
  /* Brand */
  --color-primary-900: #0B0F19;
  --color-primary-700: #151B2C;
  --color-accent: #FF4B2B;
  --color-accent-hover: #E8391B;
  --color-secondary: #00D9A3;
  --color-highlight: #FFC63A;

  /* Neutral */
  --color-white: #FFFFFF;
  --color-gray-100: #F4F5F7;
  --color-gray-300: #D8DBE2;
  --color-gray-500: #8A8F9C;
  --color-gray-700: #4B505C;
  --color-gray-900: #1B1F27;

  /* Status */
  --color-success: #00D9A3;
  --color-warning: #FFC63A;
  --color-danger: #FF4B2B;
  --color-info: #3E8BFF;

  /* Radius */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 20px;
  --radius-full: 9999px;

  /* Spacing */
  --space-1: 4px;  --space-2: 8px;  --space-3: 12px; --space-4: 16px;
  --space-6: 24px; --space-8: 32px; --space-12: 48px; --space-16: 64px;

  /* Typography */
  --font-display: 'Sora', sans-serif;
  --font-body: 'Inter', sans-serif;
}
```

> Frontend team (React/Vue/Flutter) import trực tiếp block token này làm nguồn chân lý duy nhất (single source of truth) — không tự ý đổi giá trị màu ở component riêng lẻ.
