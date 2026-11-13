# Kich ban demo end-to-end (Issue X.2), chay vao API that:
#   Sales tao Lead -> convert -> Le tan ban goi -> Member dat lich PT -> Member face check-in
#   -> PT hoan thanh buoi -> Commission tu tinh -> Le tan ban POS -> Admin xem bao cao
# Dung: .\scripts\e2e\demo-flow.ps1 [-BaseUrl http://localhost:18080]
# Dieu kien: xem scripts\e2e\README.md (backend can 2 bien moi truong de buoi hoc bat dau sau vai phut).
# File nay co y viet khong dau, va khong in ten tieng Viet lay tu API, vi Windows PowerShell 5.1
# co the doc sai ma hoa phan hoi (chi anh huong hien thi).
param(
    [string]$BaseUrl = "http://localhost:18080",
    [string]$Password = "Gym@12345",
    [string]$AdminEmail = "admin@gym.test",
    [string]$SalesEmail = "sales@gym.test",
    [string]$ReceptionEmail = "reception@gym.test",
    [string]$TrainerEmail = "trainer1@gym.test",
    [int]$StartInMinutes = 3,
    [int]$SessionMinutes = 30
)
$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")
$ci = [Globalization.CultureInfo]::InvariantCulture

function Step([string]$name) { Write-Host ""; Write-Host "=== $name" -ForegroundColor Cyan }
function Pass([string]$msg) { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Info([string]$msg) { Write-Host "       $msg" -ForegroundColor DarkGray }
function Expect([bool]$cond, [string]$msg, $detail = $null) {
    if (-not $cond) {
        $extra = ""
        if ($null -ne $detail) { $extra = " | Phan hoi that: " + ($detail | ConvertTo-Json -Depth 6 -Compress) }
        throw "Kiem tra that bai: $msg$extra"
    }
    Pass $msg
}

# Goi API, tra ve phan "data" cua ApiResponse. Loi HTTP duoc nem kem noi dung loi cua backend.
function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null, [string]$Token = $null)
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = $headers; ContentType = "application/json" }
    if ($null -ne $Body) {
        if ($Body -is [string]) { $params["Body"] = $Body } else { $params["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress) }
    }
    try {
        $resp = Invoke-RestMethod @params
    } catch {
        $detail = $_.Exception.Message
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $detail = $_.ErrorDetails.Message
        } elseif ($_.Exception.Response -and ($_.Exception.Response | Get-Member -Name GetResponseStream -ErrorAction SilentlyContinue)) {
            try {
                $sr = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream(), [Text.Encoding]::UTF8)
                $txt = $sr.ReadToEnd()
                if ($txt) { $detail = $txt }
            } catch { }
        }
        throw "$Method $Path -> $detail"
    }
    if ($resp -and ($resp.PSObject.Properties.Name -contains "success") -and ($resp.success -eq $false)) {
        throw "$Method $Path -> $($resp.errorCode): $($resp.message)"
    }
    return $resp.data
}

function Login([string]$email, [string]$pw) {
    $data = Invoke-Api -Method Post -Path "/api/v1/auth/login" -Body @{ email = $email; password = $pw }
    if (-not $data.accessToken) { throw "Dang nhap $email khong tra ve accessToken" }
    return $data.accessToken
}

# Gio Viet Nam (backend dung Asia/Ho_Chi_Minh).
$vnZone = $null
foreach ($id in @("SE Asia Standard Time", "Asia/Ho_Chi_Minh")) {
    try { $vnZone = [TimeZoneInfo]::FindSystemTimeZoneById($id); break } catch { }
}
if ($null -eq $vnZone) { throw "Khong tim thay mui gio Viet Nam tren may nay" }
function VnNow { return [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $vnZone) }

# Descriptor khuon mat 128 chieu, sinh tu seed co dinh (hai seed khac nhau cho hai vector cach xa nhau).
function New-DescriptorJson([int]$seed) {
    $rnd = New-Object System.Random($seed)
    $vals = New-Object System.Collections.Generic.List[string]
    for ($i = 0; $i -lt 128; $i++) { $vals.Add($rnd.NextDouble().ToString("R", $ci)) }
    return '{"descriptor":[' + ($vals -join ",") + ']}'
}

function Money($v) { if ($null -eq $v) { return [decimal]0 } return [decimal]$v }

$runId = (VnNow).ToString("yyMMddHHmmss", $ci)
$today = (VnNow).ToString("yyyy-MM-dd", $ci)
$seedBase = [int]$runId.Substring(6)
$memberEmail = "e2e.$runId@gym.test"
$memberPassword = "E2e@12345"
$leadPhone = "09" + $runId.Substring(4)

try {
    Write-Host "Kich ban demo end-to-end, backend: $BaseUrl, run: $runId" -ForegroundColor White

    # ---------------------------------------------------------------- 0
    Step "0. Chuan bi: dang nhap cac vai tro, chup so lieu bao cao truoc khi chay"
    $adminToken = Login $AdminEmail $Password
    $salesToken = Login $SalesEmail $Password
    $receptionToken = Login $ReceptionEmail $Password
    $trainerToken = Login $TrainerEmail $Password
    Pass "Dang nhap duoc Admin, Sales, Le tan, PT"

    $revBefore = Invoke-Api -Method Get -Path "/api/v1/reports/revenue?from=$today&to=$today" -Token $adminToken
    $comBefore = Invoke-Api -Method Get -Path "/api/v1/reports/commissions-summary" -Token $adminToken
    Info ("Doanh thu hom nay truoc khi chay: tong={0} goi={1} pos={2}" -f $revBefore.total, $revBefore.membershipTotal, $revBefore.posTotal)
    Info ("Luong PT thang nay truoc khi chay: {0} buoi, {1}" -f $comBefore.totalSessions, $comBefore.totalAmount)

    $probe = VnNow
    $slotEnd = $probe.AddMinutes($StartInMinutes + $SessionMinutes + 2)
    if ($slotEnd.Date -ne $probe.Date) { throw "Gan nua dem, buoi hoc se qua 24:00. Hay chay lai vao gio khac (truoc 23:00)." }

    # ---------------------------------------------------------------- 1
    Step "1. Sales tao Lead, cham soc, chuyen thanh hoi vien"
    $lead = Invoke-Api -Method Post -Path "/api/v1/leads" -Token $salesToken -Body @{ fullName = "E2E Khach $runId"; phone = $leadPhone; source = "e2e"; notes = "Kich ban e2e $runId" }
    Expect ($lead.status -eq "NEW") "Tao lead thanh cong, trang thai NEW (id $($lead.id))"
    $lead = Invoke-Api -Method Patch -Path "/api/v1/leads/$($lead.id)/status" -Token $salesToken -Body @{ status = "CONTACTED" }
    Expect ($lead.status -eq "CONTACTED") "Doi trang thai sang CONTACTED"
    $converted = Invoke-Api -Method Post -Path "/api/v1/leads/$($lead.id)/convert" -Token $salesToken -Body @{ email = $memberEmail; password = $memberPassword }
    Expect ($converted.status -eq "CONVERTED" -and $converted.convertedUserId) "Chuyen thanh hoi vien $memberEmail"
    $memberId = $converted.convertedUserId
    $memberToken = Login $memberEmail $memberPassword
    Pass "Hoi vien moi dang nhap duoc"

    # ---------------------------------------------------------------- 2
    Step "2. Le tan ban goi tap, xac nhan thu tien"
    $pkgPage = Invoke-Api -Method Get -Path "/api/v1/packages?page=0&size=50" -Token $receptionToken
    $packages = @($pkgPage.content | Where-Object { $_.active })
    Expect ($packages.Count -gt 0) "Co goi tap dang ban"
    $withSessions = @($packages | Where-Object { $_.sessionCount -and [int]$_.sessionCount -gt 0 } | Sort-Object { [decimal]$_.price })
    if ($withSessions.Count -gt 0) { $pkg = $withSessions[0] } else { $pkg = ($packages | Sort-Object { [decimal]$_.price })[0] }
    Info ("Chon goi ma {0}, gia {1}, so buoi {2}" -f $pkg.id, $pkg.price, $pkg.sessionCount)
    # Phan hoi cua POST /member-packages boc ca goi (memberPackage) va giao dich (paymentTransaction).
    $purchase = Invoke-Api -Method Post -Path "/api/v1/member-packages" -Token $receptionToken -Body @{ userId = $memberId; packageId = $pkg.id }
    $mp = $purchase.memberPackage
    $tx = $purchase.paymentTransaction
    Expect ($null -ne $mp -and $mp.status -eq "PENDING") "Dang ky goi, trang thai PENDING cho thanh toan"
    Expect ($null -ne $tx -and $tx.id) "Co giao dich cho thanh toan (so tien $($tx.amount))"
    $null = Invoke-Api -Method Post -Path "/api/v1/payments/$($tx.id)/confirm" -Token $receptionToken -Body @{ paymentMethod = "CASH" }
    $mine = Invoke-Api -Method Get -Path "/api/v1/member-packages/me" -Token $memberToken
    Expect ($mine.status -eq "ACTIVE") "Sau khi thu tien, goi cua hoi vien ACTIVE (con $($mine.remainingSessions) buoi)"

    # ---------------------------------------------------------------- 3
    Step "3. Member dat lich PT, PT xac nhan"
    $startAt = (VnNow).AddMinutes($StartInMinutes)
    $startAt = $startAt.AddSeconds(-$startAt.Second).AddMilliseconds(-$startAt.Millisecond)
    $endAt = $startAt.AddMinutes($SessionMinutes)
    $startStr = $startAt.ToString("HH:mm:ss", $ci)
    $endStr = $endAt.ToString("HH:mm:ss", $ci)
    $bookingDate = $startAt.ToString("yyyy-MM-dd", $ci)

    $mySchedules = @(Invoke-Api -Method Get -Path "/api/v1/trainer-schedules?from=$bookingDate&to=$bookingDate" -Token $trainerToken)
    # So sanh gio bang TimeSpan vi backend co the tra "06:00" hoac "06:00:00".
    $startTs = [TimeSpan]::Parse($startStr)
    $endTs = [TimeSpan]::Parse($endStr)
    $covering = @($mySchedules | Where-Object { $_.active -and ([TimeSpan]::Parse([string]$_.startTime) -le $startTs) -and ([TimeSpan]::Parse([string]$_.endTime) -ge $endTs) })
    if ($covering.Count -gt 0) {
        $trainerId = $covering[0].trainerId
        Pass "PT co ca lam viec phu khung gio $startStr - $endStr"
    } else {
        $sch = Invoke-Api -Method Post -Path "/api/v1/trainer-schedules" -Token $trainerToken -Body @{ workDate = $bookingDate; startTime = $startStr; endTime = $endStr; active = $true }
        $trainerId = $sch.trainerId
        Pass "Tao ca lam viec cho PT $startStr - $endStr"
    }
    Expect ($null -ne $trainerId) "Biet ma PT ($trainerId)"

    try {
        $booking = Invoke-Api -Method Post -Path "/api/v1/bookings" -Token $memberToken -Body @{ trainerId = $trainerId; bookingDate = $bookingDate; startTime = $startStr; endTime = $endStr; notes = "e2e $runId" }
    } catch {
        if ("$_" -match "BOOKING_TOO_SOON") {
            throw "Backend dang yeu cau dat lich truoc nhieu phut. Hay chay backend voi GMS_BOOKING_MIN_LEAD_MINUTES=0 (xem scripts\e2e\README.md). Chi tiet: $_"
        }
        throw
    }
    Expect ($booking.status -eq "PENDING") "Dat lich $bookingDate $startStr - $endStr, trang thai PENDING (id $($booking.id))"
    try {
        $booking = Invoke-Api -Method Post -Path "/api/v1/bookings/$($booking.id)/confirm" -Token $trainerToken
    } catch {
        if ("$_" -match "CANCELLED") {
            throw "Buoi PENDING bi tu huy vi gan gio bat dau. Hay chay backend voi GMS_BOOKING_PENDING_CANCEL_THRESHOLD_MINUTES=0 (xem scripts\e2e\README.md). Chi tiet: $_"
        }
        throw
    }
    Expect ($booking.status -eq "CONFIRMED") "PT xac nhan, trang thai CONFIRMED"
    $bookingId = $booking.id

    # ---------------------------------------------------------------- 4
    Step "4. Quet khuon mat: hoi vien vao, PT vao roi ra som, PT nop ly do"
    $memberFace = New-DescriptorJson ($seedBase + 1)
    $trainerFace = New-DescriptorJson ($seedBase + 7919)
    $null = Invoke-Api -Method Post -Path "/api/v1/face-profiles/$memberId" -Token $receptionToken -Body $memberFace
    $null = Invoke-Api -Method Post -Path "/api/v1/face-profiles/$trainerId" -Token $receptionToken -Body $trainerFace
    Pass "Dang ky khuon mat cho hoi vien va PT"

    $scan = Invoke-Api -Method Post -Path "/api/v1/face-attendance/kiosk" -Token $receptionToken -Body $memberFace
    Expect ($scan.matchedUserId -eq $memberId) "May quet nhan ra hoi vien" $scan
    # Backend khong tra bookingId cho luot quet cua hoi vien, chi bao da gan vao buoi dat lich.
    Expect ($scan.bookingLinked -eq $true -and $scan.targetType -eq "BOOKING" -and $scan.action -eq "CHECK_IN") "Luot quet cua hoi vien gan vao buoi dat lich (check-in)" $scan

    $scan = Invoke-Api -Method Post -Path "/api/v1/face-attendance/kiosk" -Token $receptionToken -Body $trainerFace
    Expect ($scan.matchedUserId -eq $trainerId -and $scan.bookingLinked -eq $true -and $scan.action -eq "CHECK_IN" -and $scan.bookingId -eq $bookingId) "PT quet vao, gan dung buoi dat lich" $scan

    Info "Cho 16 giay de qua khoang chong quet trung cua PT..."
    Start-Sleep -Seconds 16
    $scan = Invoke-Api -Method Post -Path "/api/v1/face-attendance/kiosk" -Token $receptionToken -Body $trainerFace
    Expect ($scan.matchedUserId -eq $trainerId -and $scan.action -eq "CHECK_OUT" -and $scan.earlyLeave -eq $true) "PT quet ra truoc gio ket thuc (ra som)" $scan
    $booking = Invoke-Api -Method Put -Path "/api/v1/bookings/$bookingId/early-leave-reason" -Token $trainerToken -Body @{ reason = "E2E hoan thanh som theo thoa thuan" }
    Expect (-not [string]::IsNullOrWhiteSpace($booking.ptEarlyLeaveReason)) "PT nop ly do roi buoi som"

    # ---------------------------------------------------------------- 5
    Step "5. PT hoan thanh buoi, luong tu tinh"
    $booking = Invoke-Api -Method Post -Path "/api/v1/bookings/$bookingId/complete" -Token $trainerToken
    Expect ($booking.status -eq "COMPLETED") "Buoi chuyen sang COMPLETED"
    Expect ($booking.fullDuration -eq $true) "Co ly do ra som nen van duoc tinh du buoi (fullDuration = true)"

    $commission = $null
    for ($i = 0; $i -lt 8 -and $null -eq $commission; $i++) {
        $coms = Invoke-Api -Method Get -Path "/api/v1/commissions/me?page=0&size=50" -Token $trainerToken
        $commission = @($coms.content | Where-Object { $_.bookingId -eq $bookingId })[0]
        if ($null -eq $commission) { Start-Sleep -Seconds 1 }
    }
    if ($null -eq $commission) { throw "Chua thay khoan luong cua buoi $bookingId. Kiem tra PT da duoc cai muc luong theo buoi (seed 30_pos.sql) va log backend." }
    Expect ($commission.status -eq "PENDING") ("Khoan luong tu dong ghi nhan: {0}, trang thai PENDING" -f $commission.amount)
    $commissionAmount = Money $commission.amount

    # ---------------------------------------------------------------- 6
    Step "6. Le tan ban POS"
    $prodPage = Invoke-Api -Method Get -Path "/api/v1/products?page=0&size=50" -Token $receptionToken
    $product = @($prodPage.content | Where-Object { $_.active -and [int]$_.stockQuantity -ge 2 })[0]
    Expect ($null -ne $product) "Co san pham con ton kho de ban (ma $($product.id), gia $($product.price))"
    $qty = 2
    $order = Invoke-Api -Method Post -Path "/api/v1/pos/orders" -Token $receptionToken -Body @{ memberId = $memberId; items = @(@{ productId = $product.id; quantity = $qty }) }
    $expectedTotal = (Money $product.price) * $qty
    Expect ((Money $order.totalAmount) -eq $expectedTotal) ("Don POS tong tien {0} (= {1} x {2}), trang thai {3}" -f $order.totalAmount, $product.price, $qty, $order.status)
    $posTotal = Money $order.totalAmount

    # ---------------------------------------------------------------- 7
    Step "7. Admin xem bao cao (dashboard)"
    $revAfter = Invoke-Api -Method Get -Path "/api/v1/reports/revenue?from=$today&to=$today" -Token $adminToken
    $pkgPrice = Money $pkg.price
    Expect (((Money $revAfter.membershipTotal) - (Money $revBefore.membershipTotal)) -ge $pkgPrice) ("Doanh thu goi tang it nhat {0} ({1} -> {2})" -f $pkgPrice, $revBefore.membershipTotal, $revAfter.membershipTotal)
    Expect (((Money $revAfter.posTotal) - (Money $revBefore.posTotal)) -ge $posTotal) ("Doanh thu POS tang it nhat {0} ({1} -> {2})" -f $posTotal, $revBefore.posTotal, $revAfter.posTotal)
    $comAfter = Invoke-Api -Method Get -Path "/api/v1/reports/commissions-summary" -Token $adminToken
    Expect (([int]$comAfter.totalSessions - [int]$comBefore.totalSessions) -ge 1) ("Bao cao luong PT thang nay tang them it nhat 1 buoi ({0} -> {1})" -f $comBefore.totalSessions, $comAfter.totalSessions)
    Expect (((Money $comAfter.totalAmount) - (Money $comBefore.totalAmount)) -ge $commissionAmount) ("Tong luong PT tang it nhat {0}" -f $commissionAmount)
    $null = Invoke-Api -Method Get -Path "/api/v1/reports/checkin-traffic?from=$today&to=$today" -Token $adminToken
    $null = Invoke-Api -Method Get -Path "/api/v1/reports/top-packages?from=$today&to=$today" -Token $adminToken
    $null = Invoke-Api -Method Get -Path "/api/v1/reports/renewal-rate?from=$today&to=$today" -Token $adminToken
    Pass "Cac bao cao luu luong, goi ban chay, ty le gia han tra ket qua binh thuong"

    Write-Host ""
    Write-Host "HOAN TAT: kich ban demo end-to-end chay dung ca 7 buoc." -ForegroundColor Green
    Write-Host "Tai khoan hoi vien vua tao de xem tren giao dien: $memberEmail / $memberPassword" -ForegroundColor White
    exit 0
} catch {
    Write-Host ""
    Write-Host "KICH BAN DUNG LAI: $_" -ForegroundColor Red
    exit 1
}
