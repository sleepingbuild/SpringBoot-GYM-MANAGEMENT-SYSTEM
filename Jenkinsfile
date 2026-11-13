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
        timeout(time: 45, unit: 'MINUTES')
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
                expression { env.GIT_BRANCH in ['origin/main', 'main', 'origin/master', 'master'] }
            }
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"
                    docker info *> $null
                    if ($LASTEXITCODE -ne 0) { throw "Docker khong chay hoac tai khoan Jenkins khong dung duoc Docker (mo Docker Desktop)" }

                    # SQL Server, Redis, db-init duoc giu nguyen (du lieu khong mat); chi dung lai backend va frontend.
                    docker compose -f $env:DEMO_COMPOSE up -d --build backend frontend
                    if ($LASTEXITCODE -ne 0) { throw "docker compose up that bai" }

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
