# GMS Frontend

Giao diện cho Gym Management System. Next.js 14 (App Router) + TypeScript + Tailwind CSS. Khung dùng chung do Agent 1 viết; mỗi agent thêm màn hình của module mình.

## Chạy

Cần Node 20 trở lên.

```bash
cd frontend
cp .env.example .env.local      # Windows PowerShell: Copy-Item .env.example .env.local
npm ci
npm run dev                     # http://localhost:3000
```

`NEXT_PUBLIC_API_BASE_URL` trong `.env.local` trỏ tới backend Spring Boot. Mặc định `http://localhost:8081` (bản do Jenkins triển khai). Backend chạy trên máy bạn thì đổi sang cổng bạn đặt. Backend đã cho phép CORS từ mọi nguồn khi phát triển.

Các lệnh kiểm tra (phải xanh trước khi mở PR): `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`.

Tài khoản đăng nhập thử: xem `docs/TEST_ACCOUNTS.md` (Agent 1 sẽ thêm cùng dữ liệu mẫu). Trước khi có, đăng ký tài khoản hội viên ở `/register`.

## Cấu trúc

```
src/
  app/                    Trang (App Router)
    login/, register/     Công khai
    (app)/                Mọi trang cần đăng nhập (có khung menu): dashboard, profile và các module
  components/
    ui/                   Thành phần dùng chung (Agent 1): Button, Field, Alert, PageHeader, EmptyState...
    layout/               AppShell (kiểm tra phiên + menu theo vai trò), RequireRole
  config/navigation.ts    Menu và phân quyền hiển thị. Mỗi chủ module chỉ sửa dòng của mình
  features/<module>/      Component và hàm gọi API riêng của từng module
  lib/
    api/client.ts         apiFetch: gọi backend, bóc ApiResponse, xử lý phiên theo errorCode
    api/errors.ts         ApiError, errorMessage
    auth/                 Phiên đăng nhập (session.ts) và AuthContext
    roles.ts, types.ts    Vai trò và kiểu dữ liệu dùng chung
    toast.tsx             Thông báo nhanh
```

Phân quyền thư mục: bạn chỉ sửa `src/app/(app)/<route của bạn>/` và `src/features/<module của bạn>/`. Các file ở `lib/`, `components/` và `config/` (ngoài dòng của bạn) thuộc Agent 1; cần đổi thì nhờ Agent 1.

## Làm một màn hình mới

1. Mở `src/app/(app)/<route>/page.tsx` của module bạn (hiện là trang giữ chỗ `ModulePlaceholder`) và thay toàn bộ nội dung.
2. Hàm gọi API và component riêng đặt trong `src/features/<module>/`.
3. Gọi backend bằng `apiFetch` (không dùng `fetch` trực tiếp, không ghi cứng URL):

```ts
import { apiFetch } from "@/lib/api/client";

const packages = await apiFetch<PageResponse<PackageItem>>("/api/v1/packages", {
  query: { page: 0, size: 10 },
  auth: false, // endpoint công khai
});
```

4. Bắt lỗi bằng `ApiError` và hiện chữ bằng `errorMessage(err)`; xử lý theo `err.errorCode`, không theo mã HTTP.
5. Bao trang chỉ dành cho một số vai trò bằng `<RequireRole roles={["SUPER_ADMIN"]}>`. Đây chỉ là lớp giao diện, backend mới là nơi phân quyền thật.
6. Khi màn hình xong, đổi `status` của mục tương ứng trong `src/config/navigation.ts` thành `"ready"` (Tổng quan sẽ hiện "Đã có").

Mỗi màn hình cần đủ bốn trạng thái: đang tải, rỗng (nói rõ việc tiếp theo), lỗi API (hiện `message`), không có quyền.

## Quy tắc xử lý phiên và lỗi (đã cài trong `apiFetch`)

| Tình huống | Hành vi |
|---|---|
| 401/403 không có `errorCode` (Spring Security chặn ở tầng lọc) hoặc `AUTH_TOKEN_EXPIRED`/`AUTH_TOKEN_INVALID` | Tự làm mới token một lần rồi gửi lại. Không được thì xoá phiên và chuyển về `/login` |
| 403 `AUTH_FORBIDDEN_ROLE` | Ném `ApiError` kind `forbidden`, giữ phiên |
| 401 có `errorCode` khác (`FACE_NOT_RECOGNIZED`, `AUTH_INVALID_CREDENTIALS`) | Chuyển nguyên cho trang gọi, không đụng tới phiên |
| Lỗi mạng | `ApiError` kind `network` |

Trang chạy lâu như kiosk điểm danh: truyền `skipAuthRedirect: true` để phiên hỏng chỉ ném lỗi, không đá người dùng ra trang đăng nhập.

Token: access token chỉ giữ trong bộ nhớ; refresh token lưu `localStorage` (`gms.refreshToken`). Không ghi log token, không đưa token vào URL.

Thời gian từ backend là `LocalDateTime` giờ Việt Nam, không có múi giờ trong chuỗi. Hiển thị nguyên, không cộng trừ múi giờ ở trình duyệt.

## Camera và HTTPS (Agent 4)

`getUserMedia` chỉ chạy ở `localhost` hoặc HTTPS. Thử kiosk trên chính máy chạy frontend thì dùng `http://localhost:3000`. Thiết bị khác (điện thoại) cần HTTPS: dùng đường hầm HTTPS (ví dụ ngrok trỏ tới cổng 3000) hoặc `next dev --experimental-https`.

## Những điều khung dựa trên giả định (cần đối chiếu với backend)

Khung được viết theo các DTO Agent 1 đã thấy. Bốn điểm sau chưa đối chiếu trực tiếp với controller thật; sai thì sửa đúng một chỗ:

1. Làm mới token: `POST /api/v1/auth/refresh` với body `{ "refreshToken": "..." }` (`src/lib/auth/session.ts`).
2. Đăng xuất: `POST /api/v1/auth/logout` với header `Authorization: Bearer <accessToken>` (`src/lib/auth/AuthContext.tsx`).
3. Hình dạng `PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`) ở `src/lib/types.ts`.
4. Trường của `AuthResponse`: `accessToken`, `refreshToken`, `email`, `fullName`, `roles`.

Chưa có trong khung: tải ảnh đại diện (`POST /api/v1/profile/me/avatar`), stage build frontend trong Jenkins.
