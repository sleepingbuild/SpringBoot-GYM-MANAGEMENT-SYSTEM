# Giám sát hệ thống bằng Prometheus và Grafana

Bản demo Docker có thể bật thêm giám sát: **Prometheus** thu thập số liệu của backend mỗi 10 giây, **Grafana** vẽ dashboard "GMS Tổng quan backend" có sẵn.

## Bật giám sát
```powershell
docker compose -f docker-compose.demo.yml -f docker-compose.monitoring.yml up -d --build
docker compose -f docker-compose.demo.yml -f docker-compose.monitoring.yml ps
```
Backend được tạo lại để lộ endpoint số liệu; các dịch vụ khác giữ nguyên. Mọi lệnh `docker compose` sau đó cũng phải kèm đủ hai file `-f`, nếu không Docker coi Prometheus và Grafana là "dịch vụ thừa".

| Địa chỉ | Dùng để |
|---|---|
| http://localhost:3001 | Grafana. Dashboard mở sẵn, xem không cần đăng nhập. Muốn sửa: `admin` / `admin12345` |
| http://localhost:9090 | Prometheus. Menu Status, Targets: `gms-backend` phải là `UP` |
| http://localhost:8082/actuator/prometheus | Số liệu thô của backend |

## Tạo lưu lượng để dashboard có số
Dashboard chỉ có số khi có người gọi API. Chạy vòng lặp này (có cả yêu cầu đúng và yêu cầu bị từ chối để thấy các mã 200 và 401):
```powershell
1..300 | ForEach-Object {
  Invoke-WebRequest http://localhost:8082/api/v1/packages -UseBasicParsing | Out-Null
  try { Invoke-WebRequest http://localhost:8082/api/v1/users -UseBasicParsing | Out-Null } catch { }
}
```
Hoặc chạy `.\scripts\demo-smoke.ps1` vài lần, hoặc thao tác trên giao diện web.

## Các ô trên dashboard
| Ô | Ý nghĩa |
|---|---|
| Backend | `UP` khi Prometheus lấy được số liệu |
| Yêu cầu mỗi giây, theo mã trạng thái | Lưu lượng HTTP, tách 200 / 401 / 403 / 500... |
| Lỗi 5xx mỗi giây | Lỗi phía máy chủ, đỏ khi vượt 0,01 lần mỗi giây |
| Độ trễ p50 và p95 | 50% và 95% yêu cầu được xử lý trong thời gian này |
| Bộ nhớ JVM, CPU, luồng và GC | Sức khoẻ của tiến trình Java |
| Kết nối cơ sở dữ liệu (Hikari) | Số kết nối đang dùng, rảnh và tối đa tới SQL Server |
| Top 5 đường dẫn | Những endpoint được gọi nhiều nhất |

## Cảnh báo (alert)

Luật cảnh báo nằm ở `monitoring/prometheus/alerts.yml`, xem tại Prometheus, trang **Alerts** (`http://localhost:9090/alerts`). Chưa nối Alertmanager nên không gửi mail hay tin nhắn, chỉ hiện trên Prometheus.

| Cảnh báo | Điều kiện | Mức |
|---|---|---|
| `BackendDown` | Prometheus không lấy được số liệu backend quá 30 giây | critical |
| `HighServerErrorRate` | Lỗi 5xx trên 0,05 lần/giây trong 1 phút | warning |
| `HighLatencyP95` | 95% yêu cầu chậm hơn 1 giây trong 2 phút | warning |
| `HeapAlmostFull` | Heap JVM trên 90% trong 5 phút | warning |
| `DatabasePoolExhausted` | Trên 90% kết nối của pool đang dùng trong 2 phút | warning |

**Thử cảnh báo:** `docker compose -f docker-compose.demo.yml -f docker-compose.monitoring.yml stop backend`, đợi khoảng 1 phút, `BackendDown` chuyển Pending rồi Firing. Bật lại bằng `... start backend`.

Sửa `alerts.yml` xong, nạp lại không cần tạo lại container: `docker compose -f docker-compose.demo.yml -f docker-compose.monitoring.yml kill -s SIGHUP prometheus`. Jenkins và script triển khai SSH đã tự làm bước này.

## Bảo mật
- Endpoint `/actuator/prometheus` được cho phép không cần đăng nhập ở tầng bảo mật, **nhưng mặc định không được mở** (trả 404). Chỉ khi bật như trên (`SPRING_APPLICATION_JSON` trong `docker-compose.monitoring.yml`) nó mới có số liệu. Bản chạy Jenkins (cổng 8081) và chạy tay không bật nên không lộ số liệu.
- Số liệu gồm tên đường dẫn và thống kê JVM, không có dữ liệu người dùng hay mật khẩu. Nếu đưa lên môi trường thật, hãy chặn cổng này ở tầng mạng hoặc thêm xác thực.
- Đổi mật khẩu Grafana bằng biến `GMS_GRAFANA_PASSWORD` trước lệnh `up`. Cổng đổi bằng `GMS_GRAFANA_PORT`, `GMS_PROMETHEUS_PORT`.

## Xử lý sự cố
- Target `gms-backend` là `DOWN`: kiểm tra `http://localhost:8082/actuator/prometheus` trả 200. Trả 404 nghĩa là backend chưa được tạo lại với file giám sát, chạy lại lệnh `up` ở trên.
- Dashboard trống: chưa có lưu lượng, chạy vòng lặp ở trên rồi đợi 20 giây.
- Ô "Độ trễ" trống dù đã có lưu lượng: cần ít nhất một phút dữ liệu sau khi backend khởi động.
