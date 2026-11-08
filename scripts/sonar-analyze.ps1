<#
 sonar-analyze.ps1 (Agent 1) - chay test, do do phu (JaCoCo) va gui ket qua phan tich len SonarQube.
 Can: SonarQube dang chay (docker compose -f docker-compose.sonar.yml up -d) va mot token (xem docs/SONARQUBE.md).
 Chay o goc repo:
   $env:SONAR_TOKEN = 'squ_xxxxxxxx'
   .\scripts\sonar-analyze.ps1
 hoac:  .\scripts\sonar-analyze.ps1 -Token 'squ_xxxxxxxx' -HostUrl 'http://localhost:9000'
 Mat vai phut vi chay toan bo test truoc khi phan tich.
#>
param(
  [string]$Token = $env:SONAR_TOKEN,
  [string]$HostUrl = 'http://localhost:9000',
  [string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot',
  [string]$MavenHome = 'C:\tools\apache-maven-3.9.9'
)

if (-not $Token) {
  Write-Host 'Thieu token. Dat $env:SONAR_TOKEN hoac truyen -Token (xem docs/SONARQUBE.md).' -ForegroundColor Red
  exit 1
}
if (-not (Test-Path 'pom.xml')) {
  Write-Host 'Chay o goc repo (khong thay pom.xml).' -ForegroundColor Red
  exit 1
}

try {
  $st = Invoke-RestMethod -Uri ($HostUrl + '/api/system/status') -TimeoutSec 10
  if ($st.status -ne 'UP') { Write-Host ('SonarQube chua san sang, trang thai: ' + $st.status) -ForegroundColor Yellow; exit 1 }
} catch {
  Write-Host ('Khong ket noi duoc SonarQube tai ' + $HostUrl + '. Da chay docker-compose.sonar.yml chua? Doi 1-2 phut sau khi khoi dong.') -ForegroundColor Red
  exit 1
}

$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$MavenHome\bin;" + $env:PATH

& "$MavenHome\bin\mvn.cmd" -B -ntp clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar `
  "-Dsonar.host.url=$HostUrl" "-Dsonar.token=$Token"
if ($LASTEXITCODE -ne 0) {
  Write-Host 'Phan tich that bai. Xem log Maven o tren.' -ForegroundColor Red
  exit 1
}
Write-Host ''
Write-Host ('Xong. Xem ket qua: ' + $HostUrl + '/dashboard?id=gym-management-system') -ForegroundColor Green
