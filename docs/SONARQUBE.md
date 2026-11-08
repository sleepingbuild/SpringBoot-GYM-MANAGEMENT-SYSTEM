# Phân tích chất lượng mã bằng SonarQube

SonarQube quét mã nguồn backend để tìm lỗi (bug), lỗ hổng bảo mật, đoạn mã khó bảo trì (code smell), mã trùng lặp và đo **độ phủ test** (JaCoCo). Kết quả xem trên web, có "Quality Gate" đạt hoặc không đạt.

## Yêu cầu
- Docker Desktop còn trống khoảng **3 GB RAM**. Nếu đang chạy bản demo (`docker-compose.demo.yml`) mà máy yếu thì tắt nó trước: `docker compose -f docker-compose.demo.yml down`.
- Cổng **9000** trống (đổi bằng biến `GMS_SONAR_PORT`).

## Bước 1: chạy SonarQube
```powershell
docker compose -f docker-compose.sonar.yml up -d
docker compose -f docker-compose.sonar.yml logs -f sonarqube
```
Đợi tới khi log có dòng `SonarQube is operational` (khoảng 1 đến 2 phút, lần đầu lâu hơn), bấm Ctrl+C để thoát phần xem log (SonarQube vẫn chạy). Kiểm tra: mở http://localhost:9000.

## Bước 2: đăng nhập và tạo token (làm một lần)
1. Vào http://localhost:9000, đăng nhập `admin` / `admin`, hệ thống buộc đổi mật khẩu.
2. Góc phải chọn biểu tượng tài khoản, vào **My Account**, mục **Security**.
3. Ở "Generate Tokens": đặt tên `gms`, loại **User Token**, hạn tuỳ chọn, bấm **Generate**. **Sao chép token ngay** (chỉ hiện một lần, dạng `squ_...`).

## Bước 3: phân tích
```powershell
$env:SONAR_TOKEN = 'squ_dan_token_vao_day'
.\scripts\sonar-analyze.ps1
```
Script chạy toàn bộ test, đo độ phủ, rồi gửi kết quả lên SonarQube (mất vài phút). Xong thì mở http://localhost:9000/dashboard?id=gym-management-system.

## Cách đọc kết quả
| Mục | Ý nghĩa |
|---|---|
| Quality Gate | Đạt hoặc không đạt theo ngưỡng mặc định ("Sonar way") trên **mã mới** |
| Bugs | Lỗi có khả năng gây sai kết quả |
| Vulnerabilities, Security Hotspots | Lỗ hổng bảo mật và chỗ cần con người xem xét |
| Code Smells | Mã khó đọc, khó bảo trì |
| Coverage | Độ phủ test do JaCoCo đo (đã loại DTO, entity, config và lớp khởi động) |
| Duplications | Tỉ lệ mã trùng lặp |

## Cách đo độ phủ
`pom.xml` có `jacoco-maven-plugin`: tác tử JaCoCo gắn vào lúc chạy test, báo cáo XML ở `target/site/jacoco/jacoco.xml` mà SonarQube đọc. `lombok.config` đánh dấu mã Lombok sinh ra (getter, setter, builder) là `@Generated` để không bị tính vào độ phủ. Các thư mục `dto`, `entity`, `config` và lớp `GymManagementApplication` được loại khỏi **độ phủ** (`sonar.coverage.exclusions` trong `pom.xml`) vì chỉ chứa dữ liệu hoặc cấu hình; chúng **vẫn được quét** lỗi và lỗ hổng.

## Xử lý sự cố
- Không vào được http://localhost:9000: xem log `docker compose -f docker-compose.sonar.yml logs sonarqube`; lỗi liên quan `max virtual memory` hoặc `vm.max_map_count` nghĩa là cấu hình bỏ qua mmap chưa có hiệu lực, báo lại để đổi cách.
- Script báo "Khong ket noi duoc SonarQube": chờ thêm, hoặc kiểm tra `http://localhost:9000/api/system/status` có `"status":"UP"`.
- Lỗi `Unable to open Git repository: repository not found ... \.git\worktrees\...`: thư mục đang dùng là git worktree, thư viện Git của SonarQube không đọc được. Script đã tắt việc đọc Git (`-Dsonar.scm.disabled=true`), nên không có thông tin tác giả từng dòng và việc xác định mã mới không dựa trên Git; kết quả quét lỗi, lỗ hổng, độ phủ và trùng lặp không đổi.
- Lỗi xác thực (`Not authorized`): token sai hoặc hết hạn, tạo lại ở Bước 2.
- Dừng: `docker compose -f docker-compose.sonar.yml down` (giữ dữ liệu), thêm `-v` để xoá hết.
