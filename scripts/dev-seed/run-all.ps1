# Chay toan bo dev-seed vao SQL Server trong Docker, dung thu tu phu thuoc.
# Dung: .\scripts\dev-seed\run-all.ps1   (tham so tuy chon: -Container, -Database, -Password)
# Dieu kien: container SQL Server dang chay va backend da khoi dong it nhat mot lan voi ban code moi nhat
# (Flyway tao bang). File nay co y viet khong dau de Windows PowerShell 5.1 doc dung ma hoa.
param(
    [string]$Container = $(if ($env:GMS_SQL_CONTAINER) { $env:GMS_SQL_CONTAINER } else { "gms-sqlserver" }),
    [string]$Database = $(if ($env:GMS_DB) { $env:GMS_DB } else { "gms_db" }),
    [string]$Password = $(if ($env:MSSQL_SA_PASSWORD) { $env:MSSQL_SA_PASSWORD } else { "GmsStrongP@ss123" })
)
$ErrorActionPreference = "Stop"
$sqlcmd = "/opt/mssql-tools18/bin/sqlcmd"

# Kiem tra truoc: schema da duoc Flyway migrate toi V6 (bang leads) chua.
$check = docker exec $Container $sqlcmd -S localhost -U sa -P $Password -d $Database -C -h -1 -W -Q "SET NOCOUNT ON; SELECT CASE WHEN OBJECT_ID('dbo.leads') IS NULL THEN 'MISSING' ELSE 'OK' END"
if ($LASTEXITCODE -ne 0) {
    throw "Khong ket noi duoc database '$Database' trong container '$Container'. Kiem tra ten container, ten database, mat khau sa."
}
if (($check | Out-String).Trim() -ne "OK") {
    throw "Database '$Database' trong container '$Container' chua co bang leads (V6): schema chua duoc Flyway migrate. Hay khoi dong backend voi ban code moi nhat tren database nay roi chay lai."
}

# Thu tu: 00 (tai khoan) truoc 10 va 20; 30 chi can 00; 61 can goi do 60 tao.
$files = @("00_users.sql", "10_packages_memberships.sql", "20_bookings.sql", "30_pos.sql", "60_leads_reports.sql", "61_expiring_members.sql")

foreach ($f in $files) {
    Write-Host "==> $f"
    # docker cp roi chay bang -i trong container: tranh PowerShell lam hong tieng Viet khi pipe vao lenh ngoai.
    docker cp (Join-Path $PSScriptRoot $f) "${Container}:/tmp/$f"
    if ($LASTEXITCODE -ne 0) { throw "docker cp loi o $f" }
    docker exec $Container $sqlcmd -S localhost -U sa -P $Password -d $Database -C -f 65001 -b -i "/tmp/$f"
    if ($LASTEXITCODE -ne 0) { throw "Chay $f loi (ma $LASTEXITCODE)" }
}
Write-Host "Xong. Dang nhap thu: admin@gym.test / Gym@12345"
