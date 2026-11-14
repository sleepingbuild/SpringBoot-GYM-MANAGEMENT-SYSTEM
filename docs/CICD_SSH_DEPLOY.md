# CI/CD và triển khai tự động lên máy chủ qua SSH

Tài liệu này mô tả phần "Tự động deploy lên server qua SSH" (Agent 1). Phần Prometheus và Grafana xem `docs/MONITORING.md`, phần Docker demo xem `docs/DOCKER_DEMO.md`.

## 1. Luồng hoạt động

```
GitHub (push / merge vào master)
   │ webhook (qua ngrok)
   ▼
Jenkins (Windows)  Checkout → Restore → Build → Test → Publish → Deploy → Verify → Docker web → Deploy SSH
                                                                        (dịch vụ    (Docker      │
                                                                         Windows)    trên máy     │ SSH (khóa riêng)
                                                                                     Jenkins)     ▼
                                                              Máy chủ Linux:  git checkout đúng commit vừa xanh
                                                                              docker compose up -d --build
                                                                              chờ API, web, Prometheus, Grafana trả lời
```

Điểm chính:

- Chỉ nhánh `master` được triển khai. Jenkins đỏ ở bất kỳ stage nào thì máy chủ giữ nguyên bản cũ.
- Máy chủ lấy **đúng commit** Jenkins vừa kiểm tra (không phải "bản mới nhất"), nên không có chuyện deploy nhầm.
- Hệ thống trên máy chủ gồm: SQL Server, Redis, backend (API), frontend (web), Prometheus, Grafana. Dữ liệu nằm trong volume Docker và được giữ nguyên qua các lần deploy.
- Stage `Deploy SSH` **tự bỏ qua** khi chưa đặt `GMS_SSH_HOST`, nên pipeline vẫn xanh trong lúc bạn chưa chuẩn bị máy chủ.

## 2. Chuẩn bị máy chủ Linux (làm một lần)

> Không có chỗ để tạo máy ảo? Dùng **chính máy Windows** làm máy chủ SSH, xem mục 2B. Không tốn thêm ổ đĩa.

Máy chủ là Ubuntu 22.04 hoặc 24.04, tối thiểu 2 CPU, 4 GB RAM (SQL Server cần ít nhất 2 GB), 20 GB ổ đĩa. Có thể là:

- Máy ảo VirtualBox hoặc Hyper-V trên chính máy Windows của bạn. **Đặt mạng là Bridged** để Jenkins (Windows) và trình duyệt gọi được vào IP của máy ảo.
- Máy chủ thật hoặc VPS (Azure for Students, Oracle Cloud miễn phí, nhà trường cấp).

Trên máy chủ:

```bash
# 1. Tạo tài khoản triển khai (bỏ qua nếu đã có tài khoản thường dùng sudo)
sudo adduser gms
sudo usermod -aG sudo gms
su - gms

# 2. Cài Docker, git, SSH server, mở cổng (cần Internet)
curl -fsSLO https://raw.githubusercontent.com/sleepingbuild/SpringBoot-GYM-MANAGEMENT-SYSTEM/master/scripts/deploy/setup-server.sh
bash setup-server.sh

# 3. Đăng xuất rồi đăng nhập lại (để nhóm docker có hiệu lực), kiểm tra:
docker run --rm hello-world
hostname -I          # ghi lại địa chỉ IP, ví dụ 192.168.1.50
```

Mở cổng trên tường lửa nếu có: **22** (SSH), **3000** (web), **8082** (API), **3001** (Grafana), **9090** (Prometheus). Script đã tự mở nếu `ufw` đang bật.

### Bí mật trên máy chủ (nên làm trước lần deploy đầu)

Mật khẩu và khóa bí mật **không nằm trong repo**. Tạo tệp `~/.gms.env` trên máy chủ, script triển khai tự dùng:

```bash
cat > ~/.gms.env <<EOF
GMS_DB_PASSWORD=ChonMatKhauManh123
GMS_JWT_SECRET=$(openssl rand -hex 32)
GMS_GRAFANA_PASSWORD=MatKhauGrafana123
EOF
chmod 600 ~/.gms.env
```

Mật khẩu SQL Server phải có chữ hoa, chữ thường, số và dài ít nhất 8 ký tự; chỉ dùng chữ và số cho dễ. **Đặt trước lần deploy đầu**: mật khẩu SQL Server được gán lúc tạo volume, đổi sau đó phải xoá volume dữ liệu (`docker compose ... down -v`, mất dữ liệu).

Không đặt `.gms.env` thì hệ thống chạy với mật khẩu mặc định của bản demo (script sẽ in cảnh báo).

## 2B. Máy chủ là chính máy Windows (không cần máy ảo)

Windows có sẵn OpenSSH Server (vài MB). Jenkins đăng nhập SSH vào `127.0.0.1`, chạy `scripts/deploy/remote-deploy.ps1`, script này tự lấy đúng commit vào thư mục `%USERPROFILE%\gms-server` rồi dùng Docker Desktop đang có để dựng lại hệ thống. Cơ chế SSH, khóa, chạy lệnh từ xa giống hệt máy chủ Linux. Khác biệt duy nhất: máy chủ và máy Jenkins là cùng một máy. Muốn tách riêng, trỏ `GMS_SSH_HOST` sang một máy Windows khác (laptop của bạn trong nhóm) đã cài Docker Desktop, làm lại đúng các bước dưới đây trên máy đó.

Mở PowerShell **Run as Administrator**:

```powershell
# 1. Cài và bật SSH server
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
Set-Service sshd -StartupType Automatic
Start-Service sshd
Get-NetFirewallRule -Name *ssh*        # phải thấy luật OpenSSH-Server-In-TCP đang Enabled

# 2. Cho tài khoản của bạn dùng Docker không cần quyền admin (nếu chưa)
net localgroup docker-users $env:USERNAME /add
```

Sau đó làm mục 3 (tạo khóa) với địa chỉ `127.0.0.1` thay cho IP. **Lưu ý riêng của Windows:** nếu tài khoản của bạn thuộc nhóm Administrators, khóa công khai phải nằm ở `C:\ProgramData\ssh\administrators_authorized_keys` chứ không phải `~\.ssh\authorized_keys`:

```powershell
# (Run as Administrator) thay bằng đường dẫn khóa công khai của bạn
Get-Content "$env:USERPROFILE\.ssh\gms_deploy_ed25519.pub" | Add-Content -Encoding ascii C:\ProgramData\ssh\administrators_authorized_keys
icacls C:\ProgramData\ssh\administrators_authorized_keys /inheritance:r /grant:r "SYSTEM:F" "Administrators:F"
Restart-Service sshd

# Thử: phải ra tên máy mà không hỏi mật khẩu
ssh -i "$env:USERPROFILE\.ssh\gms_deploy_ed25519" "$env:USERNAME@127.0.0.1" hostname
```

Bí mật (mật khẩu DB, JWT) của máy chủ Windows đặt ở `%USERPROFILE%\.gms.env`, cùng nội dung với mục 2 (mỗi dòng `TEN=giatri`). Nếu bạn đang chạy bản Docker demo với mật khẩu mặc định thì **không đặt tệp này**, để hệ thống giữ nguyên mật khẩu SQL Server hiện có.

Ở mục 4, đặt thêm `GMS_SSH_OS=windows`, `GMS_SSH_HOST=127.0.0.1`, `GMS_SSH_USER=<tên tài khoản Windows>`. Khi đã đặt `GMS_SSH_HOST`, stage `Docker web` tự bỏ qua để hai đường không cùng dựng lại một hệ thống. Hệ thống đang chạy (project `gms-demo`, dữ liệu trong volume) được cập nhật tại chỗ, không mất dữ liệu.

## 3. Khóa SSH (trên máy Windows chạy Jenkins)

Jenkins đăng nhập bằng khóa, không dùng mật khẩu. Mở PowerShell **bằng tài khoản đang chạy dịch vụ Jenkins**:

```powershell
# Xem Jenkins đang chạy bằng tài khoản nào
Get-CimInstance Win32_Service -Filter "Name='Jenkins'" | Select-Object StartName

# Tạo khóa (không đặt passphrase, chỉ dùng cho việc deploy)
ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\gms_deploy_ed25519" -N '""'

# Chép khóa công khai lên máy chủ (nhập mật khẩu tài khoản gms một lần)
type "$env:USERPROFILE\.ssh\gms_deploy_ed25519.pub" | ssh gms@192.168.1.50 "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"

# Thử: phải vào được ngay, không hỏi mật khẩu
ssh -i "$env:USERPROFILE\.ssh\gms_deploy_ed25519" gms@192.168.1.50 "echo ket-noi-ok"
```

Nếu `StartName` là `LocalSystem` hoặc tài khoản khác với tài khoản bạn đang dùng, Jenkins sẽ không thấy thư mục `.ssh` của bạn. Khi đó đặt khóa ở một thư mục cố định và chỉ cấp quyền đọc cho tài khoản Jenkins (OpenSSH từ chối khóa mà người khác đọc được):

```powershell
mkdir C:\gms-deploy
copy "$env:USERPROFILE\.ssh\gms_deploy_ed25519" C:\gms-deploy\key
icacls C:\gms-deploy\key /inheritance:r /grant:r "SYSTEM:R" "Administrators:R"
```

rồi đặt biến `GMS_SSH_KEY=C:\gms-deploy\key` ở mục 4.

## 4. Cấu hình Jenkins

Vào **Manage Jenkins → System → Global properties → Environment variables**, thêm:

| Biến | Bắt buộc | Ý nghĩa | Ví dụ |
|---|---|---|---|
| `GMS_SSH_HOST` | có | Địa chỉ máy chủ. Đặt biến này thì stage `Deploy SSH` mới chạy | `192.168.1.50` |
| `GMS_SSH_OS` | không | `windows` nếu máy chủ là Windows (mục 2B); bỏ trống nếu là Linux | `windows` |
| `GMS_SSH_USER` | không | Tài khoản SSH (mặc định `gms`) | `gms` |
| `GMS_SSH_PORT` | không | Cổng SSH (mặc định `22`) | `22` |
| `GMS_SSH_KEY` | không | Đường dẫn khóa riêng (mặc định `%USERPROFILE%\.ssh\gms_deploy_ed25519`) | `C:\gms-deploy\key` |
| `GMS_PUBLIC_HOST` | không | Địa chỉ mà **trình duyệt** dùng để gọi API (mặc định bằng `GMS_SSH_HOST`) | `192.168.1.50` |
| `GMS_REPO_URL` | không | Địa chỉ git để máy chủ clone (mặc định lấy từ Jenkins) | `https://github.com/sleepingbuild/SpringBoot-GYM-MANAGEMENT-SYSTEM.git` |

Máy Jenkins cần có lệnh `ssh` và `scp` (Windows 10 trở lên có sẵn: Settings → Optional features → OpenSSH Client). Repo công khai nên máy chủ clone được không cần khóa; nếu repo riêng phải thêm deploy key cho máy chủ.

## 5. Chạy và kiểm tra

Push hoặc merge một commit vào `master`. Trong Jenkins (job GMS-CICD, Stage View) thấy thêm stage **Deploy SSH**. Log chia 4 bước:

1. Kiểm tra kết nối SSH.
2. Gửi `scripts/deploy/remote-deploy.sh` lên máy chủ.
3. Chạy trên máy chủ: lấy đúng commit, `docker compose up -d --build`, chờ API, web, Prometheus, Grafana, kiểm tra Prometheus đang thu thập số liệu backend.
4. Jenkins tự gọi lại từ bên ngoài vào 4 địa chỉ để chắc chắn tường lửa và địa chỉ đúng.

Lần đầu mất 5 đến 10 phút (tải image, build, nạp dữ liệu mẫu). Các lần sau khoảng 2 đến 5 phút.

| Dịch vụ | Địa chỉ |
|---|---|
| Web | `http://<máy-chủ>:3000` |
| API / Swagger | `http://<máy-chủ>:8082/swagger-ui.html` |
| Grafana | `http://<máy-chủ>:3001` (tài khoản `admin`, mật khẩu `GMS_GRAFANA_PASSWORD`, mặc định `admin12345`) |
| Prometheus | `http://<máy-chủ>:9090` (xem Targets và Alerts) |

Tài khoản mẫu để đăng nhập web: xem `docs/DOCKER_DEMO.md`.

## 6. Kịch bản demo cho giảng viên

1. Mở web trên máy chủ, nhớ một chỗ trên giao diện.
2. Vào GitHub, sửa một chữ trong frontend (ví dụ tiêu đề một trang), Commit trực tiếp vào `master`.
3. Jenkins tự chạy, chờ stage **Deploy SSH** xanh.
4. Tải lại web trên máy chủ: chữ đã đổi, không ai chạm vào máy chủ.
5. Mở Grafana (dashboard GMS Overview) cho thấy số liệu và, nếu muốn, dừng backend (`docker compose ... stop backend`) để cảnh báo `BackendDown` hiện trên Prometheus.

## 7. Xử lý sự cố

| Triệu chứng trong log | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Stage `Deploy SSH` hiện "skipped" | Chưa đặt `GMS_SSH_HOST`, hoặc build không phải nhánh `master` | Đặt biến ở mục 4 |
| `Khong thay khoa SSH` | Jenkins chạy bằng tài khoản khác nên không thấy `.ssh` của bạn | Đặt `GMS_SSH_KEY` như mục 3 |
| `Permission denied (publickey)` | Khóa công khai chưa vào `authorized_keys` của đúng tài khoản | Làm lại bước chép khóa, kiểm tra `ssh -i ... gms@IP` bằng tay |
| `UNPROTECTED PRIVATE KEY FILE` | Tệp khóa riêng cho người khác đọc | Chạy `icacls` như mục 3 |
| `Connection timed out` | Sai IP, máy ảo không ở mạng Bridged, hoặc tường lửa chặn cổng 22 | Ping và thử `ssh` bằng tay từ Windows |
| `Host key verification failed` | Máy chủ cài lại nên khóa máy chủ đổi | Xoá dòng của IP đó trong `%USERPROFILE%\.ssh\known_hosts` |
| Bước 3 trên Windows báo `error getting credentials` hoặc `logon session does not exist` | Phiên SSH không dùng được kho mật khẩu Docker Desktop | Mở `%USERPROFILE%\.docker\config.json`, xoá dòng `"credsStore": "desktop"`, chạy lại (image công khai không cần đăng nhập) |
| Bước 3 trên Windows báo `Khong dung duoc Docker` | Docker Desktop chưa chạy, hoặc tài khoản chưa thuộc `docker-users` | Mở Docker Desktop, thêm vào nhóm `docker-users`, đăng xuất rồi đăng nhập lại |
| `Tai khoan ... khong dung duoc Docker` | Tài khoản chưa vào nhóm `docker` hoặc chưa đăng nhập lại | `sudo usermod -aG docker gms`, đăng nhập lại |
| Bước 3 báo API không sẵn sàng | Backend lỗi khi khởi động (thường do mật khẩu DB không khớp volume cũ, hoặc thiếu RAM) | Xem log ở cuối bước 3, hoặc `docker compose logs backend` trên máy chủ |
| Bước 4 báo "Jenkins không gọi được" | Tường lửa chưa mở cổng 3000, 8082, 9090, 3001, hoặc `GMS_PUBLIC_HOST` sai | Mở cổng, kiểm tra địa chỉ |
| Web mở được nhưng không có dữ liệu, lỗi mạng trong Console trình duyệt | Web được build với địa chỉ API sai | Kiểm tra `GMS_PUBLIC_HOST` là địa chỉ mà **trình duyệt** gọi tới được |
| Camera điểm danh khuôn mặt không bật | Trình duyệt chỉ cho dùng camera trên `https` hoặc `localhost` | Dùng qua HTTPS hoặc thử bằng `localhost` (ghi chú khi demo) |

## 8. Bảo mật

- Khóa SSH không passphrase chỉ dùng cho việc deploy; chỉ để trên máy Jenkins, không đưa vào repo.
- Tài khoản `gms` trên máy chủ chỉ cần chạy Docker. Không dùng root, không mở đăng nhập SSH bằng mật khẩu nếu máy chủ ra Internet (`PasswordAuthentication no` trong `/etc/ssh/sshd_config`).
- Đổi mật khẩu mặc định (`~/.gms.env`), đừng để cổng 9090 (Prometheus) lộ ra Internet.
- `StrictHostKeyChecking=accept-new` chấp nhận khóa máy chủ ở lần kết nối đầu và từ chối nếu khóa đổi sau đó.
