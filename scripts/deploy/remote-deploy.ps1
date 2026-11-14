# remote-deploy.ps1 - CHAY TREN MAY CHU WINDOWS (Jenkins goi qua SSH, xem Jenkinsfile stage "Deploy SSH" khi GMS_SSH_OS=windows).
# Giong remote-deploy.sh: lay dung commit vua xanh, dung lai backend + frontend + Prometheus + Grafana bang Docker Compose,
# cho den khi tat ca tra loi. Giu nguyen SQL Server, Redis va du lieu.
# Bi mat (mat khau DB, JWT, Grafana) dat trong %USERPROFILE%\.gms.env, KHONG dat trong repo. Xem docs/CICD_SSH_DEPLOY.md.
param(
    [Parameter(Mandatory = $true)][string]$RepoUrl,
    [string]$Commit = "",
    [string]$Branch = "master",
    [string]$AppDir = (Join-Path $env:USERPROFILE "gms-server"),
    [string]$PublicHost = "localhost"
)

# Lenh native (docker, git) ghi stderr nhieu, nen dung Continue va tu kiem tra $LASTEXITCODE.
$ErrorActionPreference = "Continue"

function Log($m) { Write-Host "[deploy] $m" }
function Fail($m) { Write-Host "[deploy][LOI] $m"; exit 1 }

$apiPort = if ($env:GMS_API_PORT) { $env:GMS_API_PORT } else { "8082" }
$webPort = if ($env:GMS_WEB_PORT) { $env:GMS_WEB_PORT } else { "3000" }
$promPort = if ($env:GMS_PROMETHEUS_PORT) { $env:GMS_PROMETHEUS_PORT } else { "9090" }
$grafanaPort = if ($env:GMS_GRAFANA_PORT) { $env:GMS_GRAFANA_PORT } else { "3001" }

foreach ($cmd in @("git", "docker")) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { Fail "Chua co lenh $cmd trong PATH cua phien SSH" }
}
docker info *> $null
if ($LASTEXITCODE -ne 0) { Fail "Khong dung duoc Docker: mo Docker Desktop va dam bao tai khoan $env:USERNAME thuoc nhom docker-users" }

# 1. Lay ma nguon
if (-not (Test-Path (Join-Path $AppDir ".git"))) {
    Log "Clone lan dau vao $AppDir"
    git clone $RepoUrl $AppDir
    if ($LASTEXITCODE -ne 0) { Fail "git clone that bai" }
}
Set-Location $AppDir
git remote set-url origin $RepoUrl
git fetch --prune origin $Branch
if ($LASTEXITCODE -ne 0) { Fail "git fetch that bai" }
$target = if ($Commit) { $Commit } else { "origin/$Branch" }
git checkout --force --detach $target
if ($LASTEXITCODE -ne 0) { Fail "Khong checkout duoc $target (commit chua co tren nhanh $Branch?)" }
Log ("Ma nguon: " + (git rev-parse --short HEAD) + " " + (git log -1 --format=%s))

# Bi mat cua may chu nam ngoai repo
$envFile = Join-Path $env:USERPROFILE ".gms.env"
if (Test-Path $envFile) {
    Copy-Item $envFile (Join-Path $AppDir ".env") -Force
    Log "Dung bi mat tu $envFile"
} else {
    Log "CANH BAO: chua co $envFile nen dang dung mat khau/JWT mac dinh cua ban demo (xem docs/CICD_SSH_DEPLOY.md)"
}

# 2. Trinh duyet goi API theo dia chi cong khai cua may chu (nhung vao ma web luc build)
$env:GMS_PUBLIC_API_URL = "http://${PublicHost}:${apiPort}"
$compose = @("compose", "-f", "docker-compose.demo.yml", "-f", "docker-compose.monitoring.yml")

# 3. Dung lai he thong
$existing = docker @compose ps -aq sqlserver 2>$null
if (-not $existing) {
    Log "Lan dau: dung toan bo he thong (SQL Server, Redis, backend, nap du lieu mau, frontend, giam sat). Mat 5 den 10 phut."
    docker @compose up -d --build
} else {
    Log "Cap nhat backend, frontend, Prometheus, Grafana (giu nguyen SQL Server, Redis, du lieu)"
    docker @compose up -d --build backend frontend prometheus grafana
}
if ($LASTEXITCODE -ne 0) {
    Log "Neu loi co chu: error getting credentials / logon session: xoa dong credsStore trong %USERPROFILE%\.docker\config.json roi chay lai (docs/CICD_SSH_DEPLOY.md)"
    Fail "docker compose up that bai"
}
docker @compose kill -s SIGHUP prometheus *> $null

# 4. Cho tat ca san sang
function Wait-For($name, $url, $tries) {
    for ($i = 1; $i -le $tries; $i++) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
            if ($r.StatusCode -eq 200) { Log "OK  $name  $url"; return }
        } catch { }
        Start-Sleep -Seconds 5
    }
    docker @compose ps
    docker @compose logs --tail=60 backend
    Fail "$name khong san sang: $url"
}
Wait-For "API (backend)" "http://localhost:$apiPort/actuator/health" 72
Wait-For "Web (frontend)" "http://localhost:$webPort/" 36
Wait-For "Prometheus" "http://localhost:$promPort/-/ready" 24
Wait-For "Grafana" "http://localhost:$grafanaPort/api/health" 24

$scraping = $false
for ($i = 1; $i -le 12; $i++) {
    try {
        $q = [uri]::EscapeDataString('max(up{job="gms-backend"})')
        $r = Invoke-WebRequest -Uri "http://localhost:$promPort/api/v1/query?query=$q" -UseBasicParsing -TimeoutSec 10
        if ($r.Content -match ',"1"\]') { $scraping = $true; break }
    } catch { }
    Start-Sleep -Seconds 5
}
if ($scraping) { Log "OK  Prometheus dang thu thap so lieu cua gms-backend" }
else { Log "CANH BAO: chua thay gms-backend o trang thai up (xem http://${PublicHost}:${promPort}/targets)" }

docker image prune -f *> $null
Log "XONG. Web http://${PublicHost}:${webPort}  API http://${PublicHost}:${apiPort}/swagger-ui.html  Grafana http://${PublicHost}:${grafanaPort}"
exit 0
