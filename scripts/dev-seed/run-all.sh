#!/usr/bin/env bash
# Chạy toàn bộ dev-seed vào SQL Server trong Docker, đúng thứ tự phụ thuộc.
# Dùng: ./scripts/dev-seed/run-all.sh   (biến tuỳ chọn: GMS_SQL_CONTAINER, GMS_DB, MSSQL_SA_PASSWORD)
# Điều kiện: container SQL Server đang chạy và backend đã khởi động ít nhất một lần (Flyway tạo bảng).
set -euo pipefail

CONTAINER="${GMS_SQL_CONTAINER:-gms-sqlserver}"
DB="${GMS_DB:-gms_db}"
PASSWORD="${MSSQL_SA_PASSWORD:-GmsStrongP@ss123}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQLCMD="/opt/mssql-tools18/bin/sqlcmd"

# Kiểm tra trước: schema đã được Flyway migrate tới V6 (bảng leads) chưa.
if ! CHECK="$(docker exec "$CONTAINER" "$SQLCMD" -S localhost -U sa -P "$PASSWORD" -d "$DB" -C -h -1 -W \
    -Q "SET NOCOUNT ON; SELECT CASE WHEN OBJECT_ID('dbo.leads') IS NULL THEN 'MISSING' ELSE 'OK' END")"; then
  echo "Không kết nối được database '$DB' trong container '$CONTAINER'. Kiểm tra tên container, tên database, mật khẩu sa." >&2
  exit 1
fi
if [ "$(echo "$CHECK" | tr -d '[:space:]')" != "OK" ]; then
  echo "Database '$DB' trong container '$CONTAINER' chưa có bảng leads (V6): schema chưa được Flyway migrate." >&2
  echo "Hãy khởi động backend với bản code mới nhất trên database này rồi chạy lại." >&2
  exit 1
fi

# Thứ tự: 00 (tài khoản) trước 10 và 20; 30 chỉ cần 00; 61 cần gói do 60 tạo.
FILES=(00_users.sql 10_packages_memberships.sql 20_bookings.sql 30_pos.sql 60_leads_reports.sql 61_expiring_members.sql)

for f in "${FILES[@]}"; do
  echo "==> $f"
  docker cp "$DIR/$f" "$CONTAINER:/tmp/$f"
  docker exec "$CONTAINER" "$SQLCMD" -S localhost -U sa -P "$PASSWORD" -d "$DB" -C -f 65001 -b -i "/tmp/$f"
done
echo "Xong. Đăng nhập thử: admin@gym.test / Gym@12345"
