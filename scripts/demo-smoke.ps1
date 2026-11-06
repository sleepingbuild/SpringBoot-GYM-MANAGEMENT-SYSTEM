<#
 demo-smoke.ps1 (Agent 1) - kiem thu khoi (smoke test) he thong GMS theo tung vai tro.
 Dang nhap 6 tai khoan mau (scripts/dev-seed/00_users.sql), goi cac endpoint GET chinh va kiem tra ma tra ve:
 200 voi cho duoc phep, 401 khi chua dang nhap, 403 khi sai vai tro.
 Chay:   .\scripts\demo-smoke.ps1                       (mac dinh API http://localhost:8082 cua docker-compose.demo.yml)
         .\scripts\demo-smoke.ps1 -Api http://localhost:8081   (ban Jenkins, seed vao gms_db_ci)
 Day la kiem tra "co song va dung quyen", khong thay the test nghiep vu. Dong FAIL: doc ma Thuc_te de biet 401/403/404/500.
#>
param(
  [string]$Api = 'http://localhost:8082',
  [string]$Password = 'Gym@12345'
)

$today = (Get-Date).ToString('yyyy-MM-dd')
$script:fail = 0
$script:results = @()

function Get-Code([string]$Path, [string]$Token) {
  $h = @{}
  if ($Token) { $h['Authorization'] = "Bearer $Token" }
  try {
    $r = Invoke-WebRequest -Uri ($Api + $Path) -Method Get -Headers $h -UseBasicParsing -TimeoutSec 30
    return [int]$r.StatusCode
  } catch {
    if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
    return 0
  }
}

function Get-Token([string]$Email) {
  try {
    $body = (@{ email = $Email; password = $Password } | ConvertTo-Json)
    $r = Invoke-RestMethod -Method Post -Uri "$Api/api/v1/auth/login" -ContentType 'application/json' -Body $body -TimeoutSec 30
    return $r.data.accessToken
  } catch { return $null }
}

function Add-Result([string]$Who, [string]$Path, $Expect, $Actual, [bool]$Ok) {
  if (-not $Ok) { $script:fail++ }
  $label = 'FAIL'
  if ($Ok) { $label = 'PASS' }
  $script:results += [pscustomobject]@{ Ket_qua = $label; Vai_tro = $Who; Kiem_tra = $Path; Ky_vong = $Expect; Thuc_te = $Actual }
}

function Check([string]$Who, [string]$Token, [string]$Path, [int]$Expect) {
  $code = Get-Code $Path $Token
  Add-Result $Who $Path $Expect $code ($code -eq $Expect)
}

function Check-Count([string]$Who, [string]$Token, [string]$Path, [string]$Prop, [int]$Min) {
  $n = -1
  try {
    $r = Invoke-RestMethod -Uri ($Api + $Path) -Headers @{ Authorization = "Bearer $Token" } -TimeoutSec 30
    $d = $r.data
    if ($Prop) { $d = $r.data.$Prop }
    $n = @($d).Count
  } catch { }
  Add-Result $Who ($Path + '  (so dong tra ve)') ('>= ' + $Min) $n ($n -ge $Min)
}

Write-Host ("Kiem thu khoi he thong tai " + $Api) -ForegroundColor Cyan

# --- Chua dang nhap
Check 'khach' $null '/actuator/health' 200
Check 'khach' $null '/v3/api-docs' 200
Check 'khach' $null '/api/v1/packages' 200
Check 'khach' $null '/api/v1/packages/admin' 401
Check 'khach' $null '/api/v1/users' 401

$accounts = [ordered]@{
  'admin'     = 'admin@gym.test'
  'reception' = 'reception@gym.test'
  'sales'     = 'sales@gym.test'
  'trainer'   = 'trainer1@gym.test'
  'member'    = 'member.active@gym.test'
  'member0'   = 'member.nopackage@gym.test'
}
$tok = @{}
foreach ($k in $accounts.Keys) {
  $t = Get-Token $accounts[$k]
  $tok[$k] = $t
  Add-Result $k ('dang nhap ' + $accounts[$k]) 'co token' $(if ($t) { 'co token' } else { 'khong co' }) ([bool]$t)
}

# --- Quan tri vien
if ($tok['admin']) {
  $t = $tok['admin']; $w = 'admin'
  Check $w $t '/api/v1/packages/admin' 200
  Check $w $t '/api/v1/reports/revenue' 200
  Check $w $t '/api/v1/reports/top-packages' 200
  Check $w $t '/api/v1/reports/checkin-traffic' 200
  Check $w $t '/api/v1/reports/renewal-rate' 200
  Check $w $t '/api/v1/reports/commissions-summary' 200
  Check $w $t '/api/v1/commissions' 200
  Check $w $t "/api/v1/staff-attendance?date=$today" 200
  Check $w $t "/api/v1/bookings?date=$today" 200
  Check $w $t '/api/v1/payments' 200
  Check $w $t '/api/v1/pos/orders' 200
  Check $w $t '/api/v1/leads' 200
  Check $w $t '/api/v1/branches' 200
  Check-Count $w $t '/api/v1/trainers' '' 2
  Check-Count $w $t '/api/v1/users?role=MEMBER&q=gym.test' 'content' 3
}

# --- Le tan
if ($tok['reception']) {
  $t = $tok['reception']; $w = 'reception'
  Check-Count $w $t '/api/v1/users?role=MEMBER&q=active' 'content' 1
  Check $w $t '/api/v1/payments' 200
  Check $w $t '/api/v1/pos/orders' 200
  Check $w $t '/api/v1/products/admin' 200
  Check $w $t '/api/v1/staff-attendance/me' 200
  Check $w $t '/api/v1/reports/revenue' 403
  Check $w $t '/api/v1/commissions' 403
}

# --- Sales
if ($tok['sales']) {
  $t = $tok['sales']; $w = 'sales'
  Check $w $t '/api/v1/leads' 200
  Check $w $t '/api/v1/leads/expiring-soon' 200
  Check $w $t '/api/v1/staff-attendance/me' 200
  Check $w $t '/api/v1/pos/orders' 403
  Check $w $t '/api/v1/users' 403
}

# --- Huan luyen vien
if ($tok['trainer']) {
  $t = $tok['trainer']; $w = 'trainer'
  Check $w $t '/api/v1/bookings/me' 200
  Check $w $t '/api/v1/commissions/me' 200
  Check $w $t '/api/v1/trainer-schedules' 200
  Check $w $t '/api/v1/profile/me' 200
  Check $w $t '/api/v1/leads' 403
  Check $w $t '/api/v1/commissions' 403
}

# --- Hoi vien co goi
if ($tok['member']) {
  $t = $tok['member']; $w = 'member'
  Check $w $t '/api/v1/member-packages/me' 200
  Check $w $t '/api/v1/bookings/me' 200
  Check $w $t '/api/v1/check-ins/me' 200
  Check $w $t '/api/v1/payments/me' 200
  Check $w $t "/api/v1/trainer-schedules/trainers?date=$today" 200
  Check $w $t '/api/v1/trainer-schedules/trainers' 400
  Check $w $t '/api/v1/packages' 200
  Check $w $t '/api/v1/users' 403
  Check $w $t '/api/v1/reports/revenue' 403
}

# --- Hoi vien chua co goi
if ($tok['member0']) {
  $t = $tok['member0']; $w = 'member0'
  Check $w $t '/api/v1/profile/me' 200
  Check $w $t '/api/v1/payments/me' 200
  Check $w $t '/api/v1/leads' 403
}

Write-Host ""
$script:results | Format-Table -AutoSize
$total = $script:results.Count
if ($script:fail -eq 0) {
  Write-Host ("TAT CA $total kiem tra deu dat.") -ForegroundColor Green
} else {
  Write-Host ("$($script:fail)/$total kiem tra KHONG dat. Dan bang tren (cac dong FAIL) cho Agent 1.") -ForegroundColor Red
  exit 1
}
