# CONTRIBUTING.md — Quy tắc Đóng góp

## 1. Git Workflow

- Nhánh chính: `main` (luôn ở trạng thái build được, không push thẳng).
- Nhánh làm việc: `feature/agentN-ten-module` (vd: `feature/agent4-payment-pos`).
- Sửa lỗi: `fix/mo-ta-ngan`.
- Không bao giờ `git add .` — dùng `git add -p` hoặc liệt kê rõ từng file, để không vô tình commit đè file của agent khác đang chạy song song trên cùng máy/CI.
- Mọi thay đổi vào **file SHARED** (`SecurityConfig`, `JwtTokenProvider`, `GlobalExceptionHandler`, `BaseEntity`, `User`, `Role`) phải có PR riêng, gắn tag `[shared]`, và bắt buộc Agent 1 review trước khi merge.

## 2. Conventional Commits

```
<type>(<scope>): <mô tả ngắn>

feat(membership): thêm API bảo lưu gói tập
fix(payment): sửa lỗi double-multiply amount VietQR
refactor(checkin): tách logic anti-passback ra service riêng
docs(readme): cập nhật hướng dẫn chạy local
test(pt-booking): thêm test cho luồng xác nhận 2 chiều
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `style`.

## 3. Pull Request

Mỗi PR cần:
- Mô tả ngắn: làm gì, tại sao.
- Acceptance Criteria (checklist) — dựa theo nhiệm vụ trong `TASK_ASSIGNMENT.md`.
- Link migration mới (nếu có bảng/cột mới).
- Xác nhận: build pass (`mvn clean verify`), có test cơ bản, đã cập nhật `API_DESIGN.md` nếu thêm endpoint.
- Không merge PR của chính mình vào file SHARED — cần Agent 1 duyệt.

## 4. Coding Convention

- Package theo module nghiệp vụ, đặt tên class rõ nghĩa: `MembershipService`, `CheckInService`, `VietQrClient`...
- DTO tách riêng Request/Response, không expose Entity trực tiếp ra Controller.
- Dùng `@Transactional` cho mọi thao tác ghi có liên quan nhiều bảng (vd: check-in trừ buổi + ghi log).
- Validate input bằng Bean Validation (`@NotNull`, `@Valid`...) ở DTO, không validate thủ công trong Controller.
- Không hard-code secret/API key — luôn qua `application.yml` + biến môi trường (`.env`, không commit).
- Log đủ nhưng không log dữ liệu nhạy cảm (password, token, số thẻ...).

## 5. Migration (Flyway)

- Đặt tên: `V{n}__{mo_ta_snake_case}.sql`, số thứ tự tăng dần, **không** sửa lại migration đã merge vào `main`.
- Mỗi agent quản lý dải số migration riêng theo bảng trong `DATABASE_SCHEMA.md` (`V1`/`V2b` Agent 1, `V2` Agent 2, `V4` Agent 3, `V3`/`V5`/`V5b` Agent 4, `V6` Agent 5, `V7` Agent 6) để tránh đụng số thứ tự khi merge song song. Hậu tố chữ (`V2b`, `V5b`) dùng khi cần chèn thêm 1 migration nhỏ liên quan mà không muốn đẩy số các agent khác.

## 6. Test

- Tối thiểu 1 test cho mỗi nhánh logic quan trọng (đặc biệt: check-in 6 bước, tính hoa hồng, webhook idempotency, chống double-booking Group X).
- Ưu tiên test tầng Service (business logic) hơn test Controller.

## 7. Review checklist trước khi merge vào `main`

- [ ] Build pass, không warning nghiêm trọng
- [ ] Không có secret/API key bị commit
- [ ] Migration chạy được từ đầu trên DB sạch
- [ ] Response tuân thủ `ApiResponse<T>` chuẩn
- [ ] Swagger annotation đầy đủ trên endpoint mới
- [ ] Không sửa file/khu vực thuộc agent khác mà chưa xin phép
