# DevEnv.ps1
# dev-up.ps1이 쓰는 "감지(sensing)"와 "판정(deciding)" 함수 모음.
#
# 왜 파일을 나눴나:
#   이 파일에는 실행 코드가 없고 함수 정의만 있다. 그래서 테스트 스크립트가
#   dot-source(. 파일경로)로 함수만 꺼내 쓸 수 있다 — 개발 환경을 실제로 띄우지 않고도
#   판정 로직을 검증할 수 있게 하는 이음매(seam)다.
#
# 주의: 이 파일은 반드시 UTF-8 with BOM 으로 저장할 것.
#   Windows PowerShell 5.1은 BOM이 없으면 ANSI 코드페이지(cp949)로 파싱해서 한글이 깨진다.
#   확인:  ([System.IO.File]::ReadAllBytes($path)[0..2]) -join ','   -> 239,187,191

# ─────────────────────────────────────────────────────────────────────────────
# 감지(sensing) — 바깥 세상의 현재 상태를 읽어오는 함수들
# ─────────────────────────────────────────────────────────────────────────────

# 특정 포트가 LISTEN 상태인지 1회 확인한다.
function Test-PortOpen {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $conn
}

# 그 호스트 포트를 "발행(publish)"하며 실행 중인 컨테이너 이름들을 돌려준다.
#
# 이번 결함 수정의 핵심 함수다. "포트가 열렸나"가 아니라 "누가 열었나"를 본다.
# 이름(--filter name=)이 아니라 포트(--filter publish=)로 찾는 이유:
#   mins-backend 말고도 8080을 쓰는 무관한 컨테이너(이 PC에는 dvwa가 있다)까지
#   한 번에 잡아내기 위해서다. 이름으로 찾으면 그런 건 놓친다.
function Get-ContainerNamesOnPort {
    param([int]$Port)
    # @( )로 감싸는 이유: docker 출력이 0개면 $null, 1개면 문자열, 2개 이상이면 배열이라
    # 타입이 들쭉날쭉하다. @()는 무조건 배열로 통일해줘서 .Count를 안전하게 쓸 수 있다.
    $raw = @(docker ps --filter "publish=$Port" --filter "status=running" --format "{{.Names}}" 2>$null)
    return @($raw | Where-Object { $_ -and $_.Trim() -ne "" })
}

# 포트를 잡고 있는 프로세스 이름을 사람이 읽을 형태로 돌려준다.
#
# 함정: Docker가 잡은 포트의 소유 프로세스는 java가 아니라 Docker Desktop의 중계
#       프로세스('wslrelay', 'com.docker.backend')다. 그래서 이 함수 결과로 "판정"하면 안 된다.
#       판정은 위의 Get-ContainerNamesOnPort가 하고, 이 함수는 마지막 요약에서
#       사람에게 근거를 보여주는 용도로만 쓴다. (java면 소스, wslrelay면 컨테이너)
function Get-PortOwnerLabel {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($null -eq $conn) { return "(없음)" }
    $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
    if ($null -eq $proc) { return "PID $($conn.OwningProcess)" }
    return "$($proc.ProcessName) (PID $($proc.Id))"
}

# ─────────────────────────────────────────────────────────────────────────────
# 판정(deciding) — 감지 결과를 받아 "무엇을 할지" 결정하는 순수 함수
# ─────────────────────────────────────────────────────────────────────────────

# 포트 상태와 컨테이너 목록을 받아, 이 포트에 대해 어떤 행동을 해야 하는지 결정한다.
#
# "순수 함수"란 같은 입력에 항상 같은 출력을 내고, 바깥 세상(Docker, 네트워크, 파일)을
# 건드리지 않는 함수를 말한다. 그래서 Docker가 꺼져 있어도 테스트할 수 있다 —
# 이 감지/판정 분리가 회귀 테스트를 가능하게 하는 핵심 설계다.
#
# 반환해야 할 값 (문자열 셋 중 하나):
#   'start'    아무도 안 잡고 있음        -> 소스(bootRun)로 새로 기동한다
#   'skip'     소스가 이미 잡고 있음      -> 재시작하지 않는다 (멱등성)
#   'conflict' 컨테이너가 잡고 있음        -> 정리하거나 중단해야 한다
#
function Resolve-PortAction {
    param(
        [bool]$IsPortOpen,
        # [AllowEmptyCollection()]: 빈 배열 @() 도 유효한 인자로 받아들이라는 표시.
        # 이게 없으면 PowerShell이 빈 배열을 거부한다.
        [AllowEmptyCollection()][string[]]$ContainerNames
    )

    # 판단 순서가 핵심이다. 컨테이너 여부를 '포트보다 먼저' 본다.
    # 반대로 포트를 먼저 보면(옛 로직) 컨테이너가 잡은 8080도 "이미 실행 중"으로 읽혀
    # 소스 실행을 건너뛴다 — 이번 결함 그 자체다.
    # 또한 컨테이너가 기동 중이라 아직 포트가 안 열린 순간에도 conflict여야 한다.
    # 여기서 'start'를 돌려주면 bootRun과 컨테이너가 8080을 두고 경쟁하게 된다.
    if (@($ContainerNames).Count -gt 0) { return 'conflict' }

    # 컨테이너가 없는데 포트가 열려 있다 = 소스로 띄운 백엔드가 이미 살아 있다.
    # 재시작하지 않고 통과시킨다 (멱등성).
    if ($IsPortOpen) { return 'skip' }

    # 아무도 안 잡고 있다 -> 소스로 새로 기동한다.
    return 'start'
}

# ─────────────────────────────────────────────────────────────────────────────
# 대기(waiting) — 상태가 바뀔 때까지 폴링하는 함수들
# ─────────────────────────────────────────────────────────────────────────────

# 포트가 열릴 때까지 기다린다.
function Wait-ForPort {
    param([int]$Port, [int]$TimeoutSec = 30)
    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        if (Test-PortOpen $Port) { return $true }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    return $false
}

# 포트가 "비워질" 때까지 기다린다.
#
# 왜 필요한가: 컨테이너를 stop해도 Docker Desktop의 포트 중계가 풀리는 데 1~2초 걸린다.
# 곧바로 bootRun 하면 "Port 8080 was already in use"로 죽는데, 그 에러는 사용자가 보지 않는
# 새 PowerShell 창에 찍힌다 — 또 하나의 조용한 실패가 된다.
function Wait-ForPortFree {
    param([int]$Port, [int]$TimeoutSec = 20)
    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        if (-not (Test-PortOpen $Port)) { return $true }
        Start-Sleep -Seconds 1
        $elapsed += 1
    }
    return $false
}

# 백엔드가 "요청을 받을 준비"가 될 때까지 기다린다.
#
# 왜 포트가 아니라 헬스 엔드포인트를 보나:
#   Spring Boot는 소켓을 먼저 열고 초기화(빈 생성, Hibernate DDL 등)를 이어간다.
#   포트만 보고 넘어가면 브라우저가 먼저 붙어 500을 맞는다.
#   /actuator/health 는 SecurityConfig에서 permitAll이라 인증 없이 호출할 수 있다.
function Wait-ForBackendHealth {
    param([int]$TimeoutSec = 120)
    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        try {
            # -UseBasicParsing: 없으면 PS 5.1이 Internet Explorer 엔진으로 HTML을
            # 파싱하려다 실패한다. 5.1 호환을 위해 사실상 필수 옵션이다.
            $res = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" `
                                     -UseBasicParsing -TimeoutSec 3

            # 함정: .Content 가 문자열이 아니라 Byte[] 로 온다.
            #   Actuator의 Content-Type은 표준 application/json 이 아니라
            #   application/vnd.spring-boot.actuator.v3+json 이라서, PowerShell이 이걸
            #   텍스트로 인식하지 못하고 바이너리로 취급한다 (PS 5.1과 7 모두 동일).
            #   그냥 -match 하면 영원히 실패해서 120초를 기다린 뒤 거짓 실패를 낸다.
            $body = if ($res.Content -is [byte[]]) {
                [System.Text.Encoding]::UTF8.GetString($res.Content)
            } else {
                [string]$res.Content
            }
            if ($body -match '"status"\s*:\s*"UP"') { return $true }
        } catch {
            # 아직 안 떴으면 예외가 난다 — 정상적인 대기 상황이라 무시하고 재시도한다.
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    return $false
}

# ─────────────────────────────────────────────────────────────────────────────
# 방어(guarding)
# ─────────────────────────────────────────────────────────────────────────────

# 직전에 실행한 외부 프로그램이 실패했으면 스크립트를 중단한다.
#
# 함정: $ErrorActionPreference = "Stop" 은 PowerShell cmdlet에만 적용된다.
#       docker.exe 같은 외부 exe가 0이 아닌 코드로 죽어도 예외가 나지 않고 그냥 다음 줄로
#       넘어간다. 그래서 마지막 외부 명령의 종료 코드($LASTEXITCODE)를 직접 확인해야 한다.
function Assert-LastExitOk {
    param([string]$What)
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[중단] $What 실패 (종료 코드 $LASTEXITCODE)" -ForegroundColor Red
        exit 1
    }
}
