# dev-up.ps1
# 쇼핑몰 프로젝트 전체 실행 자동화 스크립트
# 순서: DB(Docker) 확인/실행 -> 백엔드(Spring Boot) 확인/실행 -> 프론트엔드(React) 확인/실행 -> 브라우저 오픈
# 실행 방법: 프로젝트 루트에서  powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1

$ErrorActionPreference = "Stop"
# Windows PowerShell(5.1)에서 실행해도 한글이 깨지지 않도록 콘솔 출력 인코딩을 UTF-8로 고정
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
# $PSScriptRoot = 이 스크립트 파일이 있는 폴더(scripts/) -> 그 부모가 프로젝트 루트
$root = Split-Path -Parent $PSScriptRoot

# 특정 포트가 이미 LISTEN 상태인지 즉시 확인하는 함수 (재시도 없이 1회 체크)
function Test-PortOpen {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $conn
}

# 포트가 열릴 때까지 기다리는 함수 (백엔드/프론트엔드 기동 대기에 공통으로 재사용)
function Wait-ForPort {
    param(
        [int]$Port,
        [int]$TimeoutSec = 30
    )
    # 경과 시간을 누적하면서 2초 간격으로 포트 상태를 재확인 (DB 대기 로직과 동일한 패턴)
    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        if (Test-PortOpen $Port) {
            return $true
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    return $false
}

Write-Host "=== 쇼핑몰 프로젝트 실행 시작 ===" -ForegroundColor Cyan

# 1) DB 컨테이너 확인/실행 (docker-compose.yml: mins-db, PostgreSQL)
Write-Host "[1/4] DB 컨테이너 확인 중..."
docker compose -f "$root\docker-compose.yml" up -d

$elapsed = 0
$dbStatus = ""
while ($elapsed -lt 30) {
    $dbStatus = docker inspect --format='{{.State.Health.Status}}' mins-db 2>$null
    if ($dbStatus -eq "healthy") { break }
    Start-Sleep -Seconds 2
    $elapsed += 2
}
Write-Host "DB 상태: $dbStatus"

# 2) 백엔드 확인/실행 (Spring Boot, 8080 포트)
if (Test-PortOpen 8080) {
    Write-Host "[2/4] 백엔드 이미 실행 중 (8080)" -ForegroundColor Yellow
} else {
    Write-Host "[2/4] 백엔드 실행 중... (새 창에서 gradlew bootRun)"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\backend'; .\gradlew.bat bootRun"
    if (Wait-ForPort -Port 8080 -TimeoutSec 90) {
        Write-Host "백엔드 준비 완료 (8080)" -ForegroundColor Green
    } else {
        Write-Host "백엔드가 90초 안에 뜨지 않았습니다. 새로 열린 창의 로그를 확인하세요." -ForegroundColor Red
    }
}

# 3) 프론트엔드 확인/실행 (Vite, 5173 포트)
if (Test-PortOpen 5173) {
    Write-Host "[3/4] 프론트엔드 이미 실행 중 (5173)" -ForegroundColor Yellow
} else {
    Write-Host "[3/4] 프론트엔드 실행 중... (새 창에서 npm run dev)"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\frontend'; npm run dev"
    if (Wait-ForPort -Port 5173 -TimeoutSec 30) {
        Write-Host "프론트엔드 준비 완료 (5173)" -ForegroundColor Green
    } else {
        Write-Host "프론트엔드가 30초 안에 뜨지 않았습니다. 새로 열린 창의 로그를 확인하세요." -ForegroundColor Red
    }
}

# 4) 브라우저 오픈
Write-Host "[4/4] 브라우저 오픈"
Start-Process "http://localhost:5173"