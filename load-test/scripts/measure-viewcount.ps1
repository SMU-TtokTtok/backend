<#
.SYNOPSIS
    조회수 동시성(Lost Update) 1회 측정 — 이슈 #346

.DESCRIPTION
    view_count 리셋 → k6 실행 → view_count 재조회 → 유실 계산을 하나로 묶는다.
    view_count 는 API 응답에 노출되지 않으므로 정확성 판정은 psql 로만 가능하다.

    유실 판정 기준은 "설정된 iterations" 가 아니라 k6 가 실제로 받은 2xx 응답 수
    (summary.json 의 successful_views) 다. 실패한 요청은 조회수를 올리지 않기 때문이다.

.EXAMPLE
    # 최초 1회: 측정 대상 행을 심고 스모크 확인
    .\measure-viewcount.ps1 -Variant smoke -VUs 1 -Iterations 10 -Seed

.EXAMPLE
    # 변형별 본 측정
    .\measure-viewcount.ps1 -Variant v0-baseline
#>
param(
    # 결과 파일명이 되는 변형 이름 (예: v0-baseline, v1b-atomic-update-last)
    [Parameter(Mandatory = $true)]
    [string]$Variant,

    [int]$VUs = 100,

    [int]$Iterations = 5000,

    # sql/seed-viewcount.sql 이 심는 부하테스트 전용 행
    [string]$ClubId = '00000000-0000-4000-8000-000000000346',

    # 측정 대상 admin/club 행을 다시 심는다 (DB 볼륨을 새로 만든 직후에 필요)
    [switch]$Seed,

    # 본 측정 전에 버리는 워밍업 요청 수.
    # 이걸 0으로 두면 컨테이너 재기동 직후의 콜드 스타트(JIT 미컴파일, 빈 커넥션풀,
    # 빈 쿼리플랜/버퍼 캐시)를 측정하게 되고, 변형 간 지연 비교가 무의미해진다.
    # 실측: 동일 코드로 콜드/웜 각 1회 실행 시 RPS 221 → 454, p99 4868ms → 476ms 로 벌어졌다.
    [int]$WarmupIterations = 2000
)

# 'Stop' 을 쓰지 않는다. Windows PowerShell 5.1 은 네이티브 실행 파일이 stderr 에 쓴 한 줄 한 줄을
# ErrorRecord 로 감싸기 때문에, docker 가 stderr 로 내보내는 평범한 진행 메시지만으로도
# 스크립트가 종료 오류로 죽는다. 실패 판정은 $LASTEXITCODE 로 명시적으로 한다.
$ErrorActionPreference = 'Continue'

$LoadTestRoot = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $LoadTestRoot 'docker-compose.yml'
$ResultsDir = Join-Path $LoadTestRoot 'results\issue-a'
$K6Dir = Join-Path $LoadTestRoot 'k6'

if (-not (Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null
}

function Invoke-Psql {
    param([string]$Sql)
    $out = docker compose -f $ComposeFile exec -T db psql -U ttokttok_user -d ttokttok -t -A -c $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "psql 실행 실패: $Sql"
    }
    return ($out | Out-String).Trim()
}

Write-Host "=== 조회수 동시성 측정: $Variant ===" -ForegroundColor Cyan
Write-Host "VU: $VUs / iterations: $Iterations / club: $ClubId"

# 1. 측정 대상 행 준비
if ($Seed) {
    Write-Host "`n[1/6] 시드 삽입" -ForegroundColor Yellow
    $seedSql = Join-Path $LoadTestRoot 'sql\seed-viewcount.sql'
    Get-Content $seedSql -Raw -Encoding UTF8 | docker compose -f $ComposeFile exec -T db psql -U ttokttok_user -d ttokttok
    if ($LASTEXITCODE -ne 0) { throw "시드 삽입 실패" }
} else {
    Write-Host "`n[1/6] 시드 생략 (-Seed 로 강제 가능)" -ForegroundColor Gray
}

# 2. 컴포즈 네트워크 확인 (프로젝트명에 따라 달라지므로 하드코딩하지 않는다)
Write-Host "[2/6] 네트워크 확인" -ForegroundColor Yellow
$appContainer = (docker compose -f $ComposeFile ps -q app).Trim()
if ([string]::IsNullOrWhiteSpace($appContainer)) { throw "app 컨테이너를 찾을 수 없다 — 스택이 기동되었는지 확인" }
$network = docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' $appContainer
$network = ($network | Out-String).Trim()
Write-Host "  network = $network"

$runDir = Join-Path $ResultsDir $Variant
if (-not (Test-Path $runDir)) { New-Item -ItemType Directory -Force -Path $runDir | Out-Null }

function Invoke-K6 {
    param([int]$Iters, [string]$LogPath)
    docker run --rm `
        --network $network `
        -v "${K6Dir}:/k6:ro" `
        -v "${runDir}:/results" `
        -e "VUS=$VUs" `
        -e "ITERATIONS=$Iters" `
        -e "BASE_URL=http://nginx:80" `
        -e "CLUB_ID=$ClubId" `
        grafana/k6:latest run /k6/scenarios/issue-a-view-count.js 2>&1 |
        Tee-Object -FilePath $LogPath
}

# 3. 워밍업 — 결과는 버린다. JVM JIT / 커넥션풀 / 쿼리플랜·버퍼 캐시를 데운다.
if ($WarmupIterations -gt 0) {
    Write-Host "[3/6] 워밍업 ($WarmupIterations 요청, 결과 폐기)" -ForegroundColor Yellow
    Invoke-K6 -Iters $WarmupIterations -LogPath (Join-Path $runDir 'k6-warmup.log') | Out-Null
} else {
    Write-Host "[3/6] 워밍업 생략 — 콜드 스타트를 측정하게 된다" -ForegroundColor Red
}

# 4. view_count 리셋 — 워밍업이 올린 값을 포함해 0으로 되돌려 변형 간 조건을 맞춘다
Write-Host "[4/6] view_count 리셋" -ForegroundColor Yellow
Invoke-Psql "UPDATE clubs SET view_count = 0 WHERE id = '$ClubId';" | Out-Null
$before = [long](Invoke-Psql "SELECT view_count FROM clubs WHERE id = '$ClubId';")
Write-Host "  before = $before"
if ($before -ne 0) { throw "리셋 실패 — before 가 0이 아니다: $before" }

# 5. 본 측정
Write-Host "[5/6] 본 측정 ($Iterations 요청)" -ForegroundColor Yellow
$k6Log = Join-Path $runDir 'k6-stdout.log'
Invoke-K6 -Iters $Iterations -LogPath $k6Log

Write-Host "  view_count 재조회" -ForegroundColor Yellow
$after = [long](Invoke-Psql "SELECT view_count FROM clubs WHERE id = '$ClubId';")
$actual = $after - $before

$summaryPath = Join-Path $runDir 'summary.json'
if (-not (Test-Path $summaryPath)) { throw "summary.json 이 없다 — k6 실행이 실패했는지 $k6Log 확인" }
$summary = Get-Content $summaryPath -Raw | ConvertFrom-Json

$expected = [long]$summary.successful_views
$lost = $expected - $actual
if ($expected -gt 0) {
    $lostRate = [math]::Round(($lost / $expected) * 100, 2)
} else {
    $lostRate = 0
}

Write-Host "  기대(2xx 응답 수) = $expected / 실제 증가분 = $actual / 유실 = $lost ($lostRate%)" -ForegroundColor Cyan

# 6. 기록
Write-Host "[6/6] 결과 기록" -ForegroundColor Yellow
$reportPath = Join-Path $ResultsDir "$Variant.md"
$lines = @()
$lines += "# $Variant"
$lines += ""
$lines += "측정 시각: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += ""
$lines += "| 항목 | 값 |"
$lines += "|---|---|"
$lines += "| VU | $VUs |"
$lines += "| iterations (설정) | $Iterations |"
$lines += "| 워밍업 (폐기) | $WarmupIterations |"
$lines += "| 2xx 응답 수 (기대 증가분) | $expected |"
$lines += "| view_count 실제 증가분 | $actual |"
$lines += "| **유실** | **$lost ($lostRate%)** |"
$lines += "| 실패율 | $($summary.http_req_failed_rate) |"
$lines += "| RPS | $([math]::Round($summary.rps, 1)) |"
$lines += "| p50 (ms) | $([math]::Round($summary.latency_ms.med, 1)) |"
$lines += "| p95 (ms) | $([math]::Round($summary.latency_ms.p95, 1)) |"
$lines += "| p99 (ms) | $([math]::Round($summary.latency_ms.p99, 1)) |"
$lines += "| max (ms) | $([math]::Round($summary.latency_ms.max, 1)) |"
$lines += ""
$lines += "원본 k6 출력: ``$Variant/k6-stdout.log`` / 원본 요약: ``$Variant/summary.json``"
$lines += ""

$lines -join "`r`n" | Out-File -FilePath $reportPath -Encoding utf8
Write-Host "  기록 완료: $reportPath" -ForegroundColor Green
