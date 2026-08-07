# dev-up.ps1
# 쇼핑몰 프로젝트 로컬 개발 환경 실행 자동화 스크립트
#
# 순서: 사전 점검 -> DB(Docker) -> 백엔드(소스 bootRun) -> 프론트엔드(Vite) -> 요약/브라우저
#
# 실행:
#   powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1
#   powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1 -DryRun   (판정만 보고 종료)
#
# 판정 로직 테스트:
#   powershell -ExecutionPolicy Bypass -File .\scripts\tests\Test-DevUpLogic.ps1
#
# 설계 원칙: 백엔드는 '항상 소스(gradlew bootRun)'로 띄운다.
#   개발 중에는 "방금 고친 코드"가 도는 게 유일하게 맞는 동작이기 때문이다.
#   배포와 동일한 컨테이너 형상을 볼 땐 이 스크립트가 아니라 아래를 쓴다:
#     docker compose --profile full up -d --build     (확인 후: docker compose stop backend frontend)
#
# 주의: 이 파일은 반드시 UTF-8 with BOM 으로 저장할 것.
#   PS 5.1은 BOM이 없으면 cp949로 '파싱'해서 한글 주석·출력이 깨진다.
#   [Console]::OutputEncoding 설정은 출력 단계라 이걸 못 막는다.

param(
    # [switch] = 값 없이 이름만 붙이면 $true가 되는 플래그형 파라미터. 예: dev-up.ps1 -DryRun
    # 아무것도 기동하지 않고 "무엇을 할 예정인지"만 출력하고 끝낸다.
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
# $PSScriptRoot = 이 스크립트가 있는 폴더(scripts/) -> 그 부모가 프로젝트 루트
$root = Split-Path -Parent $PSScriptRoot
$compose = "$root\docker-compose.yml"

# dot-source: 점(.) 하나 + 경로. 감지·판정·대기 함수들을 이 스크립트로 불러온다.
. "$PSScriptRoot\lib\DevEnv.ps1"

Write-Host "=== 쇼핑몰 프로젝트 실행 시작 ===" -ForegroundColor Cyan

# ─────────────────────────────────────────────────────────────────────────────
# [1/5] 사전 점검 — Docker 데몬 + 포트 점유 '주체' 판정
#   충돌 검사를 맨 앞에서 한다. 뒤늦게 중단하면 이미 띄운 것들이 어중간하게 남는다.
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "[1/5] 사전 점검 (Docker 데몬 + 포트 점유 주체)"

# 함정: 외부 프로그램의 stderr를 리다이렉트하면(2>&1 이든 2>$null 이든) PS 5.1이 그 줄을
#   ErrorRecord로 감싼다. $ErrorActionPreference="Stop" 이면 그 순간 스크립트가 죽어서
#   아래 안내 문구에 도달하지 못하고 날것의 에러 덤프만 남는다.
#   그래서 이 한 줄 동안만 'Continue'로 낮추고, 실패는 예외가 아니라 종료 코드로 판정한다.
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
docker info --format "{{.ServerVersion}}" > $null 2>$null
$dockerOk = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = $prevEap

if (-not $dockerOk) {
    Write-Host "[중단] Docker 데몬에 접속할 수 없습니다. Docker Desktop을 먼저 실행하세요." -ForegroundColor Red
    exit 1
}

$backendContainers  = Get-ContainerNamesOnPort 8080
$frontendContainers = Get-ContainerNamesOnPort 5173
$backendAction  = Resolve-PortAction -IsPortOpen (Test-PortOpen 8080) -ContainerNames $backendContainers
$frontendAction = Resolve-PortAction -IsPortOpen (Test-PortOpen 5173) -ContainerNames $frontendContainers

if ($DryRun) {
    Write-Host "  [DryRun] 8080 판정 = $backendAction  (컨테이너: $($backendContainers -join ', '))"
    Write-Host "  [DryRun] 5173 판정 = $frontendAction (컨테이너: $($frontendContainers -join ', '))"
    Write-Host "  [DryRun] :80  컨테이너 = $((Get-ContainerNamesOnPort 80) -join ', ')"
    Write-Host "  [DryRun] 아무것도 기동하지 않고 종료합니다." -ForegroundColor Cyan
    exit 0
}

if ($backendAction -eq 'conflict') {
    # 우리 프로젝트 컨테이너와 남의 컨테이너를 나눈다. 남의 것은 절대 자동으로 안 건드린다.
    $others = @($backendContainers | Where-Object { $_ -ne 'mins-backend' })
    if ($others.Count -gt 0) {
        Write-Host "[중단] 8080을 이 프로젝트와 무관한 컨테이너가 점유 중: $($others -join ', ')" -ForegroundColor Red
        Write-Host "       내용을 확인한 뒤 직접 정리하세요:  docker stop $($others -join ' ')" -ForegroundColor Yellow
        exit 1
    }

    # 이미지 이름을 하드코딩하지 않는 이유: compose 프로젝트명은 클론한 디렉터리 이름에서
    # 유도되므로, 폴더명이 다르면 이미지명도 달라진다. 컨테이너에서 이미지 ID를 얻어 조회한다.
    $imageId = docker inspect --format "{{.Image}}" mins-backend 2>$null
    $built   = docker inspect --format "{{.Created}}" $imageId 2>$null
    Write-Host "  8080을 컨테이너 mins-backend 가 점유 중입니다 (이미지 빌드: $built)." -ForegroundColor Yellow
    Write-Host "  그 이후의 소스 변경은 반영되지 않습니다. 소스로 다시 띄우기 위해 중지합니다." -ForegroundColor Yellow

    # docker stop <이름>이 아니라 docker compose stop <서비스>를 쓰는 이유:
    #   이 compose 파일에 정의된 서비스로 범위를 좁혀, 이름이 우연히 같은 남의 컨테이너를
    #   실수로 끄는 사고를 막는다. stop은 컨테이너를 삭제하지 않고 볼륨도 안 건드리므로
    #   DB 데이터는 안전하고 되돌리기도 쉽다.
    # frontend를 함께 내리는 이유: :80에 낡은 빌드가 남으면 그것대로 혼란을 준다.
    docker compose -f $compose stop backend frontend
    Assert-LastExitOk "컨테이너 정지 (docker compose stop)"

    if (-not (Wait-ForPortFree -Port 8080 -TimeoutSec 20)) {
        Write-Host "[중단] 컨테이너를 정지했는데도 8080이 20초 안에 비워지지 않았습니다." -ForegroundColor Red
        Write-Host "       docker ps 로 남아 있는 컨테이너를 확인하세요." -ForegroundColor Yellow
        exit 1
    }
    # 포트가 비었으니 판정을 다시 계산한다 (낡은 판정을 그대로 쓰면 안 된다).
    $backendAction = 'start'
}

# ─────────────────────────────────────────────────────────────────────────────
# [2/5] DB 컨테이너 (PostgreSQL, 호스트 5433)
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "[2/5] DB 컨테이너 확인/실행 (mins-db)"

# 서비스 이름 'db'를 명시한다. docker-compose.yml의 profiles로 이미 막아뒀지만,
# 스크립트에서도 명시해 compose 파일이 바뀌어도 의도가 흔들리지 않게 한다 (이중 방어).
# --wait: 헬스체크가 healthy가 될 때까지 블로킹한다. 직접 폴링 루프를 짤 필요가 없다.
docker compose -f $compose up -d db --wait --wait-timeout 60
Assert-LastExitOk "DB 컨테이너 기동"
Write-Host "  DB 준비 완료 (localhost:5433)" -ForegroundColor Green

# ─────────────────────────────────────────────────────────────────────────────
# [3/5] 백엔드 (Spring Boot, 소스 실행, 8080)
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "[3/5] 백엔드 (소스 gradlew bootRun)"

if ($backendAction -eq 'skip') {
    Write-Host "  이미 소스 백엔드가 8080에서 실행 중입니다 — 재시작하지 않습니다. ($(Get-PortOwnerLabel 8080))" -ForegroundColor Yellow
} else {
    Write-Host "  새 PowerShell 창에서 gradlew bootRun 실행..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\backend'; .\gradlew.bat bootRun"
    # 포트가 열린 것과 요청을 받을 준비가 된 것은 다르다 — 헬스 엔드포인트를 본다.
    if (Wait-ForBackendHealth -TimeoutSec 120) {
        Write-Host "  백엔드 준비 완료 (/actuator/health = UP)" -ForegroundColor Green
    } else {
        Write-Host "  백엔드가 120초 안에 준비되지 않았습니다. 새로 열린 창의 로그를 확인하세요." -ForegroundColor Red
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# [4/5] 프론트엔드 (Vite 개발 서버, 5173)
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "[4/5] 프론트엔드 (Vite 개발 서버)"

if ($frontendAction -eq 'skip') {
    Write-Host "  이미 5173에서 실행 중입니다 — 재시작하지 않습니다." -ForegroundColor Yellow
} else {
    Write-Host "  새 PowerShell 창에서 npm run dev 실행..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\frontend'; npm run dev"
    if (Wait-ForPort -Port 5173 -TimeoutSec 30) {
        Write-Host "  프론트엔드 준비 완료 (5173)" -ForegroundColor Green
    } else {
        Write-Host "  프론트엔드가 30초 안에 뜨지 않았습니다. 새로 열린 창의 로그를 확인하세요." -ForegroundColor Red
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# [5/5] 상태 요약 + 사후검증 + 브라우저
#   조용한 실패를 '관찰 가능한 상태'로 바꾸는 부분. 이 스크립트에서 가장 중요한 출력이다.
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "[5/5] 상태 요약"

# 중요: 여기서 상태를 '다시 측정'한다. 앞부분 판정을 그대로 출력하면 중간에 무언가 죽었을 때
# 거짓 요약을 내보내게 된다. 끝난 시점의 실제 상태를 읽는다.
$now8080 = Get-ContainerNamesOnPort 8080
$dbHealth = docker inspect --format "{{.State.Health.Status}}" mins-db 2>$null

Write-Host ""
@(
    [pscustomobject]@{
        구성요소 = 'DB'
        방식     = 'Docker (mins-db)'
        주소     = 'localhost:5433'
        근거     = $dbHealth
    }
    [pscustomobject]@{
        구성요소 = '백엔드'
        방식     = $(if (@($now8080).Count -gt 0) { "Docker ($($now8080 -join ','))" } else { '소스 bootRun' })
        주소     = 'http://localhost:8080'
        근거     = (Get-PortOwnerLabel 8080)
    }
    [pscustomobject]@{
        구성요소 = '프론트엔드'
        방식     = '소스 Vite dev'
        주소     = 'http://localhost:5173'
        근거     = (Get-PortOwnerLabel 5173)
    }
) | Format-Table -AutoSize | Out-String | Write-Host

# 사후검증(postcondition) — 이번 결함의 재발을 '매 실행마다' 자동 확인한다.
# 테스트 파일은 사람이 돌려야 하지만, 이 검사는 스크립트를 쓸 때마다 돈다.
if (@($now8080).Count -gt 0) {
    Write-Host "!! 8080을 컨테이너($($now8080 -join ','))가 점유 중입니다." -ForegroundColor Red
    Write-Host "   지금 보이는 백엔드는 소스가 아니라 '빌드된 이미지'입니다." -ForegroundColor Red
} else {
    Write-Host "OK: 8080 = 소스 실행 (컨테이너 아님). 최신 코드가 반영되어 있습니다." -ForegroundColor Green
}

# :80에 낡은 컨테이너 프론트가 남아 있으면 알려준다 (5173과 충돌하지 않으므로 중단은 안 한다).
$web80 = Get-ContainerNamesOnPort 80
if (@($web80).Count -gt 0) {
    Write-Host "참고: http://localhost (:80) 에 컨테이너 프론트($($web80 -join ','))가 떠 있습니다." -ForegroundColor Yellow
    Write-Host "      개발 중 확인은 반드시 http://localhost:5173 을 보세요. :80은 낡은 빌드입니다." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "확인할 주소 -> http://localhost:5173" -ForegroundColor Cyan
Start-Process "http://localhost:5173"
