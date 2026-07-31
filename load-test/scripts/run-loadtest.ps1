# 부하테스트 실행 스크립트 (범용 러너)
# Usage: .\run-loadtest.ps1 -Scenario issue-a-view-count -VUs 50 -Duration 30s
#
# 조회수 동시성(이슈 #346) 측정에는 이 스크립트를 쓰지 않는다.
# view_count 리셋 → 워밍업 → 실행 → psql 대조까지 묶어야 유실을 계산할 수 있어서
# 전용 스크립트 measure-viewcount.ps1 을 따로 둔다.
# 이 스크립트는 정확성 판정이 필요 없는 일반 부하 시나리오용이다.

param(
    # k6/scenarios/ 아래의 시나리오 파일명 (.js 생략 가능)
    [Parameter(Mandatory = $true)]
    [string]$Scenario,

    [Parameter(Mandatory = $false)]
    [int]$VUs = 50,

    [Parameter(Mandatory = $false)]
    [string]$Duration = '30s',

    # k6 는 컴포즈 네트워크 안에서 실행되므로 컨테이너 이름으로 접근한다.
    # localhost 로 두면 k6 컨테이너 자신을 가리켜 전 요청이 실패한다.
    [Parameter(Mandatory = $false)]
    [string]$BaseUrl = 'http://nginx:80'
)

# 'Stop' 을 쓰지 않는다. Windows PowerShell 5.1 은 네이티브 실행 파일이 stderr 에 쓴 줄을
# ErrorRecord 로 감싸므로, docker 의 평범한 진행 메시지만으로도 스크립트가 죽는다.
# 실패 판정은 $LASTEXITCODE 로 명시적으로 한다.
$ErrorActionPreference = 'Continue'

$LoadTestRoot = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $LoadTestRoot 'docker-compose.yml'
$MonitoringFile = Join-Path $LoadTestRoot 'docker-compose.monitoring.yml'
$K6Dir = Join-Path $LoadTestRoot 'k6'

# 시나리오 파일 존재를 먼저 검증한다.
# 기존에는 '-Scenario issue-a' 를 'k6/scenarios/issue-a.js' 로 조립했는데
# 실제 파일명(issue-a-view-count.js)과 어긋나 항상 실패했다.
$scenarioName = $Scenario
if (-not $scenarioName.EndsWith('.js')) {
    $scenarioName = "$scenarioName.js"
}
$scenarioPath = Join-Path $K6Dir "scenarios\$scenarioName"
if (-not (Test-Path $scenarioPath)) {
    Write-Host "[ERROR] 시나리오 파일이 없습니다: $scenarioPath" -ForegroundColor Red
    Write-Host "사용 가능한 시나리오:" -ForegroundColor Yellow
    Get-ChildItem (Join-Path $K6Dir 'scenarios') -Filter *.js -ErrorAction SilentlyContinue |
        ForEach-Object { Write-Host "  - $($_.BaseName)" }
    exit 1
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  부하테스트 실행 시작" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "시나리오: $scenarioName"
Write-Host "VU: $VUs"
Write-Host "지속시간: $Duration"
Write-Host "BaseUrl: $BaseUrl"
Write-Host "----------------------------------------" -ForegroundColor Gray

# 1. Docker Compose 스택 기동 (--wait 로 healthcheck 통과까지 대기)
Write-Host "`n[1/2] Docker Compose 스택 기동 중..." -ForegroundColor Yellow
docker compose -f $ComposeFile -f $MonitoringFile up -d --build --wait --wait-timeout 240
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] 스택 기동에 실패했습니다." -ForegroundColor Red
    docker compose -f $ComposeFile -f $MonitoringFile logs --tail=50
    exit 1
}
Write-Host "  모든 서비스 정상 기동 완료" -ForegroundColor Green

# 2. k6 실행
Write-Host "`n[2/2] k6 부하테스트 실행 중..." -ForegroundColor Yellow

# 컴포즈가 만든 네트워크 이름은 프로젝트명에 따라 달라지므로 app 컨테이너에서 직접 읽는다.
$appContainer = (docker compose -f $ComposeFile ps -q app).Trim()
if ([string]::IsNullOrWhiteSpace($appContainer)) {
    Write-Host "[ERROR] app 컨테이너를 찾을 수 없습니다." -ForegroundColor Red
    exit 1
}
$network = (docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' $appContainer | Out-String).Trim()

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$resultsDir = Join-Path $LoadTestRoot "results\$($scenarioName -replace '\.js$', '')"
if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null
}
# 결과 경로는 컨테이너 기준의 파일명만 넘긴다 (호스트 경로를 그대로 넘기면 /results 아래에 중첩된다)
$resultsFileName = "run_$timestamp.json"

# k6 이미지에는 시나리오가 들어있지 않으므로 k6 디렉토리를 마운트해야 한다.
docker run --rm `
    --network $network `
    -v "${K6Dir}:/k6:ro" `
    -v "${resultsDir}:/results" `
    -e "VUS=$VUs" `
    -e "DURATION=$Duration" `
    -e "BASE_URL=$BaseUrl" `
    grafana/k6:latest `
    run "/k6/scenarios/$scenarioName" --out "json=/results/$resultsFileName"

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERROR] k6 테스트가 실패했습니다." -ForegroundColor Red
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  부하테스트 완료!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "결과 파일: $(Join-Path $resultsDir $resultsFileName)" -ForegroundColor Cyan
Write-Host "`nGrafana 대시보드: http://localhost:3000 (admin/admin)" -ForegroundColor Cyan
Write-Host "프로메테우스: http://localhost:9090" -ForegroundColor Cyan
