# Test-DevUpLogic.ps1
# scripts/lib/DevEnv.ps1 의 순수 판정 함수에 대한 회귀 테스트.
#
# 실행:
#   powershell -ExecutionPolicy Bypass -File .\scripts\tests\Test-DevUpLogic.ps1
#
# Docker가 꺼져 있어도, 인터넷이 없어도 돌아간다 — 순수 함수만 검증하기 때문이다.
#
# 왜 Pester를 안 쓰나:
#   이 PC에 설치된 Pester는 3.4.0(Windows 기본 동봉 버전)이라 요즘 문서에 나오는 문법
#   (Should -Be 등)과 달라서, 검색해서 따라 쓰면 오히려 막힌다. 검증할 게 함수 하나뿐이라
#   프레임워크 없이 최소 헬퍼만 두는 편이 낫다.
#
# 주의: 이 파일도 UTF-8 with BOM 으로 저장할 것 (PS 5.1 한글 깨짐 방지)

# dot-source: 점(.) 하나 + 경로. 다른 파일의 함수를 이 스크립트 안으로 불러온다.
# $PSScriptRoot = 이 파일이 있는 폴더(scripts/tests) -> ..\lib\DevEnv.ps1
. "$PSScriptRoot\..\lib\DevEnv.ps1"

# $script: 접두사 = 이 스크립트 전체에서 공유되는 변수. 함수 안에서 값을 바꿔도
# 함수 밖에 반영된다 (안 붙이면 함수 안에서만 사는 지역 변수가 된다).
$script:fail = 0

function Assert-Equal {
    param($Expected, $Actual, [string]$Case)
    if ($Expected -ne $Actual) {
        Write-Host "FAIL [$Case] 기대=$Expected 실제=$Actual" -ForegroundColor Red
        $script:fail++
    } else {
        Write-Host "PASS [$Case]" -ForegroundColor Green
    }
}

Write-Host "=== Resolve-PortAction 판정 진리표 ===" -ForegroundColor Cyan

Assert-Equal 'start' (Resolve-PortAction -IsPortOpen $false -ContainerNames @()) `
    '포트 비어있음 -> 소스 기동'

Assert-Equal 'skip' (Resolve-PortAction -IsPortOpen $true -ContainerNames @()) `
    '소스가 이미 점유 -> 재시작 안 함 (멱등성)'

# ↓ 이 두 줄이 2026-08-07 결함의 회귀 테스트다.
#   고치기 전 로직은 "포트 열림 = 이미 실행 중 = skip"이었다. 그래서 8/2에 빌드된
#   mins-backend 컨테이너가 8080을 잡고 있으면 gradlew bootRun을 통째로 건너뛰었고,
#   낡은 이미지가 최신 코드인 척 화면에 떴다. 이제 반드시 'conflict'여야 한다.
Assert-Equal 'conflict' (Resolve-PortAction -IsPortOpen $true -ContainerNames @('mins-backend')) `
    '우리 컨테이너 점유 -> conflict (DEF 재현 조건)'

Assert-Equal 'conflict' (Resolve-PortAction -IsPortOpen $true -ContainerNames @('dvwa')) `
    '무관한 컨테이너 점유 -> conflict'

Assert-Equal 'conflict' (Resolve-PortAction -IsPortOpen $true -ContainerNames @('mins-backend','dvwa')) `
    '컨테이너 여러 개 점유 -> conflict'

# 방어적 경계: 컨테이너는 있는데 포트가 아직 안 열린 순간(컨테이너 기동 중)도 충돌로 본다.
# 여기서 'start'를 돌려주면 bootRun과 컨테이너가 8080을 두고 경쟁하게 된다.
Assert-Equal 'conflict' (Resolve-PortAction -IsPortOpen $false -ContainerNames @('mins-backend')) `
    '컨테이너 기동 중(포트 아직 안 열림) -> conflict'

Write-Host ""
if ($script:fail -gt 0) {
    Write-Host "$($script:fail)건 실패" -ForegroundColor Red
    exit 1
}
Write-Host "전부 통과" -ForegroundColor Green
exit 0
