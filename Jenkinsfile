// Jenkinsfile - CI/CD cho Gym Management System (Spring Boot + Maven)
// Đặt ở THƯ MỤC GỐC repository (cùng cấp pom.xml).
// Yêu cầu trên máy Jenkins (Windows): JDK và Maven có trong PATH (kiểm tra: java -version, mvn -version).

pipeline {
    agent any

    tools {
        jdk 'JDK17'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    triggers {
        // Kích hoạt khi GitHub gửi webhook push (cần plugin GitHub).
        githubPush()
        // Dự phòng khi webhook chưa chạy: bỏ comment dòng dưới.
        // pollSCM('H/2 * * * *')
    }

    environment {
        MAVEN_OPTS  = '-Dfile.encoding=UTF-8'
        DEPLOY_DIR  = 'C:\\deploy\\gms'
        SERVICE     = 'GymManagementSystem'
        // Đổi cho đúng cổng/đường dẫn của ứng dụng (Swagger/springdoc hoặc actuator).
        APP_URL     = 'http://localhost:8081/v3/api-docs'
        // Ban Docker demo (docker-compose.demo.yml): web 3000, API 8082. Doi neu dat GMS_WEB_PORT/GMS_API_PORT khac.
        DEMO_COMPOSE = 'docker-compose.demo.yml'
        DEMO_MONITORING = 'docker-compose.monitoring.yml'
        DEMO_WEB_URL = 'http://localhost:3000/'
        DEMO_API_URL = 'http://localhost:8082/actuator/health'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Branch: ${env.GIT_BRANCH} - Commit: ${env.GIT_COMMIT}"
            }
        }

        stage('Restore') {
            steps {
                // Tải dependency về trước để lỗi mạng/phiên bản lộ ra sớm.
                bat 'mvn -B -ntp dependency:resolve'
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B -ntp -DskipTests clean compile'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn -B -ntp test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Publish') {
            steps {
                bat 'mvn -B -ntp -DskipTests package'
                archiveArtifacts artifacts: 'target/*.jar', excludes: 'target/*.original', fingerprint: true
            }
        }

        // Chỉ triển khai từ nhánh main/master. Nhánh khác dừng sau Publish.
        stage('Deploy') {
            when {
                expression { env.GIT_BRANCH in ['origin/main', 'main', 'origin/master', 'master'] }
            }
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"
                    $jar = Get-ChildItem target\\*.jar | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
                    if (-not $jar) { throw "Khong tim thay file jar trong target" }

                    New-Item -ItemType Directory -Force -Path $env:DEPLOY_DIR | Out-Null

                    $svc = Get-Service -Name $env:SERVICE -ErrorAction SilentlyContinue
                    if ($svc -and $svc.Status -ne "Stopped") {
                        Stop-Service -Name $env:SERVICE -Force
                        $svc.WaitForStatus("Stopped", [TimeSpan]::FromSeconds(30))
                    }

                    Copy-Item $jar.FullName (Join-Path $env:DEPLOY_DIR "gms.jar") -Force

                    if ($svc) { Start-Service -Name $env:SERVICE }
                    else { throw "Chua cai Windows service $env:SERVICE (xem huong dan nssm)" }
                '''
            }
        }

        stage('Verify') {
            when {
                expression { env.GIT_BRANCH in ['origin/main', 'main', 'origin/master', 'master'] }
            }
            steps {
                powershell '''
                    # Spring Boot + Flyway co the khoi dong cham khi may dang chay nhieu container: cho toi da ~6 phut
                    $ok = $false
                    for ($i = 1; $i -le 60; $i++) {
                        try {
                            $r = Invoke-WebRequest -Uri $env:APP_URL -UseBasicParsing -TimeoutSec 10
                            if ($r.StatusCode -eq 200) { $ok = $true; break }
                        } catch {
                            Write-Host "Lan $i chua san sang: $($_.Exception.Message)"
                        }
                        Start-Sleep -Seconds 5
                    }
                    if (-not $ok) {
                        $svc = Get-Service -Name $env:SERVICE -ErrorAction SilentlyContinue
                        Write-Host "Verify that bai. Dich vu $env:SERVICE dang: $($svc.Status). Xem log cua dich vu trong $env:DEPLOY_DIR"
                        exit 1
                    }
                    Write-Host "Verify OK: $env:APP_URL tra ve 200"
                '''
            }
        }

        // Sau khi backend (dich vu Windows) da trien khai va Verify xanh: dung lai image web + API trong Docker
        // tu CHINH ma nguon vua checkout trong workspace nay, roi doi cho den khi hai dich vu tra loi.
        stage('Docker web') {
            when {
                allOf {
                    expression { env.GIT_BRANCH in ['origin/main', 'main', 'origin/master', 'master'] }
                    // Da cau hinh Deploy SSH thi SSH lo viec nay, tranh hai duong cung dung lai mot he thong.
                    expression { !env.GMS_SSH_HOST?.trim() }
                }
            }
            steps {
                powershell '''
                    # PowerShell 5.1: lenh native ghi stderr (tien trinh build cua Docker) se thanh loi neu dung "Stop".
                    # Nen dung "Continue" va tu kiem tra $LASTEXITCODE sau moi lenh docker.
                    $ErrorActionPreference = "Continue"
                    docker version --format "{{.Server.Version}}" | Out-Null
                    if ($LASTEXITCODE -ne 0) { throw "Docker khong chay hoac tai khoan Jenkins khong dung duoc Docker (mo Docker Desktop, them tai khoan vao nhom docker-users)" }

                    # Ten du an co dinh "gms-demo" (khai bao trong file compose) nen Jenkins cap nhat DUNG cac container dang chay.
                    # SQL Server, Redis, volume du lieu duoc giu nguyen; chi dung lai backend va frontend tu ma nguon vua checkout.
                    # PHAI kem file giam sat: neu khong backend tao lai se mat cau hinh lo so lieu va Grafana bi trong.
                    docker compose -f $env:DEMO_COMPOSE -f $env:DEMO_MONITORING up -d --build backend frontend prometheus grafana
                    if ($LASTEXITCODE -ne 0) { throw "docker compose up that bai (xem log build o tren)" }
                    # Prometheus doc lai cau hinh va luat canh bao khi cac tep nay doi.
                    docker compose -f $env:DEMO_COMPOSE -f $env:DEMO_MONITORING kill -s SIGHUP prometheus | Out-Null

                    $targets = @($env:DEMO_API_URL, $env:DEMO_WEB_URL)
                    foreach ($url in $targets) {
                        $ok = $false
                        for ($i = 1; $i -le 40; $i++) {
                            try {
                                $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
                                if ($r.StatusCode -eq 200) { $ok = $true; break }
                            } catch {
                                Write-Host "Lan $i $url chua san sang: $($_.Exception.Message)"
                            }
                            Start-Sleep -Seconds 5
                        }
                        if (-not $ok) { throw "Docker web that bai: $url khong tra ve 200" }
                        Write-Host "Docker OK: $url tra ve 200"
                    }

                    docker image prune -f | Out-Null
                '''
            }
        }

        // Trien khai len MAY CHU qua SSH (Linux, Docker). Chi chay tren master VA khi da dat bien moi truong
        // GMS_SSH_HOST trong Jenkins (Manage Jenkins, System, Global properties, Environment variables).
        // Jenkins dang nhap bang khoa SSH, gui scripts/deploy/remote-deploy.sh, may chu tu lay dung commit nay va dung lai he thong.
        // Huong dan day du: docs/CICD_SSH_DEPLOY.md
        stage('Deploy SSH') {
            when {
                allOf {
                    expression { env.GIT_BRANCH in ['origin/main', 'main', 'origin/master', 'master'] }
                    expression { env.GMS_SSH_HOST?.trim() }
                }
            }
            steps {
                powershell '''
                    $ErrorActionPreference = "Continue"
                    if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) { throw "Jenkins khong co lenh ssh (Windows: Settings, Optional features, OpenSSH Client)" }

                    $sshHost = $env:GMS_SSH_HOST.Trim()
                    $sshUser = if ($env:GMS_SSH_USER) { $env:GMS_SSH_USER.Trim() } else { "gms" }
                    $sshPort = if ($env:GMS_SSH_PORT) { $env:GMS_SSH_PORT.Trim() } else { "22" }
                    $publicHost = if ($env:GMS_PUBLIC_HOST) { $env:GMS_PUBLIC_HOST.Trim() } else { $sshHost }
                    $repoUrl = if ($env:GMS_REPO_URL) { $env:GMS_REPO_URL.Trim() } else { $env:GIT_URL }
                    $key = $env:GMS_SSH_KEY
                    if (-not $key) { $key = Join-Path (Join-Path $env:USERPROFILE ".ssh") "gms_deploy_ed25519" }
                    if (-not (Test-Path $key)) { throw "Khong thay khoa SSH: $key (xem docs/CICD_SSH_DEPLOY.md, buoc tao khoa)" }
                    if (-not $repoUrl) { throw "Khong biet dia chi git cua repo (dat bien GMS_REPO_URL)" }

                    $target = "$sshUser@$sshHost"
                    $sshOpts = @("-i", $key, "-p", $sshPort, "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=20")
                    $scpOpts = @("-i", $key, "-P", $sshPort, "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=20")

                    Write-Host "== 1/4 Kiem tra ket noi SSH toi $target (cong $sshPort)"
                    ssh @sshOpts $target "echo ket-noi-ssh-ok"
                    if ($LASTEXITCODE -ne 0) { throw "Khong ket noi SSH duoc toi $target cong $sshPort (xem muc Xu ly su co trong docs/CICD_SSH_DEPLOY.md)" }

                    # GMS_SSH_OS=windows: may chu la Windows (OpenSSH Server), chay remote-deploy.ps1. Mac dinh: Linux, remote-deploy.sh.
                    $isWindows = ($env:GMS_SSH_OS -eq "windows")

                    Write-Host "== 2/4 Gui script trien khai"
                    if ($isWindows) { scp @scpOpts "scripts/deploy/remote-deploy.ps1" "${target}:gms-remote-deploy.ps1" }
                    else { scp @scpOpts "scripts/deploy/remote-deploy.sh" "${target}:/tmp/gms-remote-deploy.sh" }
                    if ($LASTEXITCODE -ne 0) { throw "scp that bai" }

                    Write-Host "== 3/4 Chay trien khai tren may chu (commit $env:GIT_COMMIT)"
                    if ($isWindows) {
                        $remote = "powershell -NoProfile -ExecutionPolicy Bypass -File gms-remote-deploy.ps1 -RepoUrl '$repoUrl' -Commit '$env:GIT_COMMIT' -Branch 'master' -PublicHost '$publicHost'"
                    } else {
                        $remote = "GMS_REPO_URL='$repoUrl' GMS_COMMIT='$env:GIT_COMMIT' GMS_BRANCH='master' GMS_PUBLIC_HOST='$publicHost' bash /tmp/gms-remote-deploy.sh"
                    }
                    ssh @sshOpts $target $remote
                    if ($LASTEXITCODE -ne 0) { throw "Trien khai tren may chu that bai (xem log o tren)" }

                    Write-Host "== 4/4 Kiem tra tu ben ngoai (Jenkins goi thang vao may chu)"
                    $urls = @(
                        ("http://{0}:8082/actuator/health" -f $publicHost),
                        ("http://{0}:3000/" -f $publicHost),
                        ("http://{0}:9090/-/ready" -f $publicHost),
                        ("http://{0}:3001/api/health" -f $publicHost)
                    )
                    foreach ($url in $urls) {
                        $ok = $false
                        for ($i = 1; $i -le 24; $i++) {
                            try {
                                $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
                                if ($r.StatusCode -eq 200) { $ok = $true; break }
                            } catch {
                                Write-Host "Lan $i $url chua san sang: $($_.Exception.Message)"
                            }
                            Start-Sleep -Seconds 5
                        }
                        if (-not $ok) { throw "May chu da chay nhung Jenkins khong goi duoc $url (kiem tra tuong lua, dia chi GMS_PUBLIC_HOST)" }
                        Write-Host "OK $url"
                    }
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline thanh cong.'
        }
        unsuccessful {
            echo 'Pipeline that bai/bi huy. Phien ban dang chay khong bi thay doi neu loi xay ra truoc Deploy.'
        }
    }
}
