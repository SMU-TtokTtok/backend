# 부하테스트 실행 스크립트
# Usage: .\run-loadtest.ps1 -Scenario issue-a -VUs 50 -Duration 30s

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('issue-a', 'issue-b', 'issue-c')]
    [string]$Scenario,

    [Parameter(Mandatory=$false)]
    [int]$VUs = 50,

    [Parameter(Mandatory=$false)]
    [string]$Duration = '30s',

    [Parameter(Mandatory=$false)]
    [string]$BaseUrl = 'http://localhost'
)

$ErrorActionPreference = 'Stop'

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  부하테스트 실행 시작" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "시나리오: $Scenario"
Write-Host "VU: $VUs"
Write-Host "지속시간: $Duration"
Write-Host "BaseUrl: $BaseUrl"
Write-Host "----------------------------------------" -ForegroundColor Gray

# 1. Docker Compose 스택 기동
Write-Host "`n[1/3] Docker Compose 스택 기동 중..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot\.."
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d --build

# 2. 서비스 정상 기동 대기
Write-Host "`n[2/3] 서비스 기동 대기 중 (최대 120초)..." -ForegroundColor Yellow
$timeout = 120
$interval = 5
$elapsed = 0
$healthy = $false

while ($elapsed -lt $timeout) {
    $dbStatus = docker compose -f docker-compose.yml -f docker-compose.monitoring.yml ps --format json | ConvertFrom-Json | Where-Object { $_.Service -eq 'db' } | Select-Object -ExpandProperty State
    $appStatus = docker compose -f docker-compose.yml -f docker-compose.monitoring.yml ps --format json | ConvertFrom-Json | Where-Object { $_.Service -eq 'app' } | Select-Object -ExpandProperty State

    if ($dbStatus -eq 'healthy' -and $appStatus -eq 'healthy') {
        $healthy = $true
        break
    }

    Write-Host "  대기 중... (경과: ${elapsed}s, DB: $dbStatus, APP: $appStatus)" -NoNewline
    Start-Sleep -Seconds $interval
    $elapsed += $interval
    Write-Host "`r" -NoNewline
}

if (-not $healthy) {
    Write-Host "`n[ERROR] 서비스 기동이 타임아웃되었습니다." -ForegroundColor Red
    docker compose -f docker-compose.yml -f docker-compose.monitoring.yml logs --tail=50
    exit 1
}
Write-Host "`n  모든 서비스 정상 기동 완료" -ForegroundColor Green

# 3. k6 부하테스트 실행
Write-Host "`n[3/3] k6 부하테스트 실행 중..." -ForegroundColor Yellow
$k6Image = "grafana/k6:latest"
$scenarioFile = "k6/scenarios/$Scenario.js"
$resultsDir = "k6/results"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$resultsFile = "$resultsDir/${Scenario}_${timestamp}.json"

# k6 실행
docker run --rm `
    --network "$(docker compose -f docker-compose.yml -f docker-compose.monitoring.yml ps --format json | ConvertFrom-Json | Where-Object { $_.Service -eq 'app' } | Select-Object -ExpandProperty Networks"`[0`"`] | Split-Path -Leaf)" `
    -e VUS=$VUs `
    -e DURATION=$Duration `
    -e BASE_URL=$BaseUrl `
    -v "${PWD}/$resultsDir":/results `
    $k6Image `
    run "/app/$scenarioFile" `
    --out json="/results/$resultsFile"

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERROR] k6 테스트가 실패했습니다." -ForegroundColor Red
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  부하테스트 완료!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "결과 파일: $resultsFile" -ForegroundColor Cyan

# 결과 요약 출력
if (Test-Path $resultsFile) {
    Write-Host "`n결과 파일 크기: $((Get-Item $resultsFile).Length / 1KB) KB" -ForegroundColor Gray

    # JSON 결과에서 주요 메트릭 추출
    $json = Get-Content $resultsFile -Raw | ConvertFrom-Json
    Write-Host "총 요청 수: $($json | Get-Member -MemberType NoteProperty | Where-Object Name -match 'iterations' | ForEach-Object { $json.$_ } | Measure-Object -Sum | Select-Object -ExpandProperty Sum)" -ForegroundColor Gray
}

Write-Host "`nGrafana 대시보드: http://localhost:3000 (admin/admin)" -ForegroundColor Cyan
Write-Host "프로메테우스: http://localhost:9090" -ForegroundColor Cyan
