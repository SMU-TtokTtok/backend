# 부하테스트 환경 정리 스크립트
# Usage: .\cleanup.ps1 [-RemoveVolumes]

param(
    [switch]$RemoveVolumes
)

$ErrorActionPreference = 'Stop'

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  부하테스트 환경 정리" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Set-Location "$PSScriptRoot\.."

# 1. docker compose 내려주기
Write-Host "`n[1/2] docker compose 스택 종료 중..." -ForegroundColor Yellow
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml down

if ($RemoveVolumes) {
    Write-Host "`n[2/2] 볼륨 삭제 중 (데이터 완전 초기화)..." -ForegroundColor Yellow
    docker compose -f docker-compose.yml -f docker-compose.monitoring.yml down -v
    Write-Host "  모든 볼륨이 삭제되었습니다." -ForegroundColor Green
} else {
    Write-Host "`n[2/2] 볼륨 유지 (데이터 보존)" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  정리 완료" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

if ($RemoveVolumes) {
    Write-Host "참고: --RemoveVolumes 없이 실행하면 데이터가 보존됩니다." -ForegroundColor Gray
}
