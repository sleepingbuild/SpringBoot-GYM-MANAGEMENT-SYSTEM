# Công cụ giám sát, theo dõi và volume trên môi trường Docker

Tài liệu này dùng để cài đặt, chạy thử và **trình bày** ba nhóm việc trên Docker của dự án GMS: công cụ giám sát, công cụ theo dõi, và volume lưu dữ liệu. Chi tiết Prometheus và Grafana nằm ở `docs/MONITORING.md`, cách chạy cả hệ thống ở `docs/DOCKER_DEMO.md`.

## 1. Bức tranh tổng thể

```
        Trình duyệt ─────────────►  Web (frontend)  :3000 ──► API (backend) :8082 ──► SQL Server (volume sqlserver-data)
                                                                    │  │                 Redis
                                                                    │  └────────────────► volume uploads (ảnh, tệp tải lên)
                                                  /actuator/prometheus│
                                                                    ▼
   cAdvisor :8083 ──(CPU, RAM, mạng từng container)──► Prometheus :9090 ──► Grafana :3001  (2 dashboard + cảnh báo)
                                                      (volume prometheus-data)  (volume grafana-data)

   Docker Desktop / lệnh docker / Portainer :9443  ──► xem, theo dõi, quản lý container, image, volume
```

## 2. Công cụ giám sát và theo dõi

| Công cụ | Dùng để | Cài ở đâu | Địa chỉ |
|---|---|---|---|
| **Docker Desktop** | Xem container, image, volume, log bằng giao diện | Đã cài sẵn | cửa sổ Docker Desktop |
| **Lệnh `docker`** | Theo dõi nhanh: trạng thái, log, tài nguyên | Đi kèm Docker | PowerShell |
| **Spring Boot Actuator + Micrometer** | Backend tự xuất số liệu (yêu cầu HTTP, JVM, kết nối DB) | Trong mã backend | `:8082/actuator/health`, `/actuator/prometheus` |
| **Prometheus** | Thu thập và lưu số liệu, đánh giá luật cảnh báo | `docker-compose.monitoring.yml` | `http://localhost:9090` |
| **Grafana** | Vẽ biểu đồ từ số liệu của Prometheus | `docker-compose.monitoring.yml` | `http://localhost:3001` |
| **cAdvisor** | Đo CPU, RAM, mạng, ổ đĩa của **từng container** | `docker-compose.monitoring.yml` | `http://localhost:8083` |
| **Portainer** (tùy chọn) | Quản lý Docker bằng giao diện web, xem volume | `docker-compose.portainer.yml` | `https://localhost:9443` |

### 2.1 Cài đặt và chạy

```powershell
# Hệ thống + giám sát (Prometheus, Grafana, cAdvisor) cùng lúc:
docker compose -f docker-compose.demo.yml -f docker-compose.monitoring.yml up -d --build

# Portainer (tùy chọn, bật khi cần trình bày):
docker compose -f docker-compose.portainer.yml up -d
```

Khi Jenkins deploy (Docker web hoặc Deploy SSH), các dịch vụ giám sát được dựng lại cùng hệ thống nên không phải làm tay.

Portainer lần đầu yêu cầu tạo tài khoản quản trị trong vòng 5 phút; trình duyệt sẽ cảnh báo chứng chỉ tự ký, chọn tiếp tục. Portainer cần đọc socket Docker nên có quyền rất cao trên máy: chỉ bật khi học và demo, tắt bằng `docker compose -f docker-compose.portainer.yml down`.

### 2.2 Theo dõi nhanh bằng lệnh Docker

```powershell
docker compose -f docker-compose.demo.yml -f docker-compose.monitoring.yml ps    # trạng thái, cổng, healthy
docker stats --no-stream                                                         # CPU, RAM, mạng, I/O từng container
docker logs -f gms-demo-backend-1                                                # theo dõi log trực tiếp (Ctrl+C để thoát)
docker logs --since 10m gms-demo-backend-1                                       # log 10 phút gần nhất
docker top gms-demo-backend-1                                                    # tiến trình bên trong container
docker inspect gms-demo-backend-1 --format "{{.State.Health.Status}}"            # trạng thái healthcheck
docker events --since 5m --filter type=container                                 # container nào vừa khởi động hoặc dừng
docker system df                                                                 # dung lượng image, container, volume
```

### 2.3 Xem trên Grafana

Vào `http://localhost:3001` (tài khoản `admin`, mật khẩu mặc định `admin12345`), thư mục **GMS** có hai dashboard:

- **GMS Tổng quan backend:** tình trạng API, số yêu cầu, lỗi 5xx, độ trễ, bộ nhớ JVM, kết nối DB.
- **GMS Docker (container và tài nguyên):** số container đang chạy, CPU, RAM, mạng vào và ra, dung lượng ghi, thời gian chạy của từng container. Dữ liệu lấy từ cAdvisor.

Kiểm tra cAdvisor có số liệu: mở `http://localhost:9090/targets`, `cadvisor` phải là **UP**. Nếu dashboard Docker trống, vào `http://localhost:9090/graph` chạy `container_last_seen{name!=""}`: không có kết quả nghĩa là cAdvisor không đọc được tên container (xem mục 5).

## 3. Volume

Container bị xóa thì dữ liệu trong container mất theo. **Volume** là nơi lưu dữ liệu nằm ngoài vòng đời của container, nên xóa và tạo lại container, hay cập nhật phiên bản, dữ liệu vẫn còn.

| Loại | Cách khai báo | Dùng cho |
|---|---|---|
| **Named volume** | `sqlserver-data:/var/opt/mssql` | Dữ liệu cần giữ lâu dài (do Docker quản lý) |
| **Bind mount** | `./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro` | Đưa tệp cấu hình từ thư mục dự án vào container (chỉ đọc) |
| **tmpfs** | `tmpfs: /tmp` | Dữ liệu tạm trong RAM, mất khi dừng (dự án không dùng) |

### 3.1 Volume của dự án

Compose thêm tiền tố tên dự án (`gms-demo_`) vào tên volume.

| Volume thực tế | Gắn vào | Chứa gì |
|---|---|---|
| `gms-demo_sqlserver-data` | SQL Server `/var/opt/mssql` | **Toàn bộ cơ sở dữ liệu** của hệ thống (quan trọng nhất) |
| `gms-demo_uploads` | Backend `/app/uploads` | Tệp do người dùng tải lên |
| `gms-demo_prometheus-data` | Prometheus `/prometheus` | Số liệu giám sát, giữ 7 ngày |
| `gms-demo_grafana-data` | Grafana `/var/lib/grafana` | Cài đặt và tài khoản Grafana |
| `gms-sonar_sonar-db-data`, `sonar-data`, `sonar-extensions`, `sonar-logs` | SonarQube và PostgreSQL | Kết quả phân tích mã |
| `gms-portainer_portainer-data` | Portainer `/data` | Tài khoản và cấu hình Portainer |

### 3.2 Tạo và quản lý volume

```powershell
docker volume ls                                        # liệt kê
docker volume inspect gms-demo_sqlserver-data           # chi tiết: Mountpoint, Labels, thời điểm tạo
docker system df -v                                     # dung lượng từng volume

docker volume create gms-demo-thuchanh                  # tạo volume thủ công
docker volume rm gms-demo-thuchanh                      # xóa (chỉ được khi không container nào đang dùng)
docker volume prune                                     # xóa MỌI volume không gắn container nào (cẩn thận)
```

Volume thực nằm bên trong máy ảo của Docker Desktop, không mở trực tiếp bằng File Explorer; muốn xem hay sao lưu thì dùng một container phụ (mục 3.4).

### 3.3 Thực hành: chứng minh dữ liệu không mất khi xóa container

```powershell
docker volume create gms-demo-thuchanh

# 1. Ghi một tệp vào volume bằng container tạm (--rm: tự xóa container khi xong)
docker run --rm -v gms-demo-thuchanh:/data alpine sh -c "echo du lieu con nguyen > /data/ghi-chu.txt"

# 2. Container đó đã bị xóa. Tạo một container MỚI gắn cùng volume và đọc lại
docker run --rm -v gms-demo-thuchanh:/data alpine cat /data/ghi-chu.txt     # in ra: du lieu con nguyen

docker volume rm gms-demo-thuchanh
```

Với hệ thống thật: `docker compose ... down` (không có `-v`) xóa container nhưng giữ volume, `up -d` lại thì tài khoản và dữ liệu vẫn còn. **`down -v` xóa cả volume, mất toàn bộ dữ liệu**, chỉ dùng khi muốn làm lại từ đầu.

### 3.4 Sao lưu và khôi phục

Sao lưu volume tệp tải lên bằng container phụ (PowerShell):

```powershell
mkdir D:\gms-backup -ErrorAction SilentlyContinue
# sao lưu
docker run --rm -v gms-demo_uploads:/data:ro -v D:\gms-backup:/backup alpine tar czf /backup/uploads.tgz -C /data .
# khôi phục
docker run --rm -v gms-demo_uploads:/data -v D:\gms-backup:/backup alpine sh -c "cd /data && tar xzf /backup/uploads.tgz"
```

Cơ sở dữ liệu nên sao lưu bằng lệnh của SQL Server (an toàn hơn chép tệp khi đang chạy):

```powershell
docker exec -it gms-demo-sqlserver-1 bash
# bên trong container:
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "BACKUP DATABASE gms_demo TO DISK = N'/var/opt/mssql/data/gms_demo.bak' WITH INIT"
exit
# chép tệp sao lưu ra máy
docker cp gms-demo-sqlserver-1:/var/opt/mssql/data/gms_demo.bak D:\gms-backup\gms_demo.bak
```

## 4. Kịch bản trình bày (khoảng 10 phút)

1. Chạy `docker compose ... ps`: sáu dịch vụ chính cùng Prometheus, Grafana, cAdvisor đều `Up`, `healthy`.
2. `docker stats --no-stream`: tài nguyên từng container, giới thiệu theo dõi bằng lệnh.
3. `docker logs -f gms-demo-backend-1`, thao tác trên web, thấy log chạy theo thời gian thực.
4. Mở Docker Desktop (hoặc Portainer): thấy cùng danh sách, tab Volumes có các volume của dự án.
5. Mở Prometheus `/targets`: `gms-backend`, `cadvisor` đều UP; trang `/alerts` có 5 luật cảnh báo.
6. Mở Grafana, dashboard **GMS Docker**: CPU, RAM từng container; dashboard **GMS Tổng quan backend**: số yêu cầu, độ trễ.
7. Tạo tải: thao tác nhiều trên web hoặc `docker exec gms-demo-backend-1 ...`, thấy biểu đồ đổi.
8. Dừng thử backend: `docker compose ... stop backend`, sau khoảng 1 phút cảnh báo `BackendDown` chuyển Firing; bật lại bằng `start backend`.
9. Làm thực hành volume ở mục 3.3: tạo volume, ghi tệp, xóa container, đọc lại.
10. Chỉ `docker volume ls` và `docker volume inspect gms-demo_sqlserver-data`, giải thích vì sao cập nhật hệ thống qua Jenkins không mất dữ liệu.

## 5. Xử lý sự cố

| Triệu chứng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Dashboard Docker trống, `cadvisor` UP nhưng `container_last_seen{name!=""}` không có kết quả | Docker dùng kho image containerd, cAdvisor v0.49 không đọc được tên container | Tắt tùy chọn "Use containerd for pulling and storing images" trong Docker Desktop (Settings, General), khởi động lại Docker; hoặc xem theo mã container trên Prometheus |
| `cadvisor` ở trạng thái `Exited` hoặc DOWN | Máy chủ không cho cAdvisor đọc `/var/lib/docker` hay `/dev/kmsg` | Xem `docker logs gms-demo-cadvisor-1`, báo lại dòng lỗi |
| Cổng 8083 hay 9443 bị chiếm | Đã có dịch vụ khác dùng cổng | Đặt `GMS_CADVISOR_PORT` hoặc `GMS_PORTAINER_PORT` sang cổng khác |
| `docker volume rm` báo đang được dùng | Còn container gắn volume (kể cả đã dừng) | `docker ps -a --filter volume=<tên>` rồi xóa container đó trước |
| Mất dữ liệu sau khi chạy lệnh | Đã chạy `down -v` hoặc `volume prune` | Khôi phục từ bản sao lưu ở mục 3.4, hoặc nạp lại dữ liệu mẫu bằng `docker compose -f docker-compose.demo.yml run --rm seed` |
