---
name: dev-run
description: Launch and verify the shopping mall app (Spring Boot backend + React frontend) in a browser. Use when asked to run/start the project, "브라우저에 띄워줘", "실행해서 확인", or to confirm a change works end-to-end.
---

# 쇼핑몰 프로젝트 실행 (dev-run)

이 프로젝트를 로컬에서 실행해서 브라우저로 확인할 때는 아래 스크립트 하나로 전 과정이 자동화되어 있다.

## 실행 방법

프로젝트 루트에서:

```powershell
& "scripts\dev-up.ps1"
```

또는 (Windows PowerShell 5.1 환경에서도 동일하게 동작):

```powershell
powershell -ExecutionPolicy Bypass -File "scripts\dev-up.ps1"
```

판정만 보고 아무것도 띄우지 않으려면:

```powershell
powershell -ExecutionPolicy Bypass -File "scripts\dev-up.ps1" -DryRun
```

## 대원칙 — 백엔드는 항상 소스로 뜬다

이 스크립트는 백엔드를 **반드시 `gradlew bootRun`(소스)** 으로 띄운다. 개발 중에는 "방금 고친
코드"가 도는 게 유일하게 맞는 동작이기 때문이다. 배포와 동일한 컨테이너 형상을 확인할 때는
이 스크립트가 아니라 아래를 쓴다.

```powershell
docker compose --profile full up -d --build   # --build 필수 (없으면 낡은 이미지 재사용)
docker compose stop backend frontend          # 확인 끝나면 정리
```

## 스크립트가 하는 일 (순서대로)

1. **사전 점검** — Docker 데몬 접속 확인 후, 8080/5173을 **누가** 잡고 있는지 판정한다.
   포트가 열렸는지가 아니라 `docker ps --filter publish=<포트>`로 **점유 주체**를 본다
2. **DB 컨테이너** — `docker compose up -d db --wait`로 `mins-db`(PostgreSQL, 호스트 5433)를
   healthy까지 대기. 서비스명 `db`를 명시한다
3. **백엔드** — 새 PowerShell 창에서 `cd backend; .\gradlew.bat bootRun`.
   포트가 아니라 `/actuator/health`가 `UP`이 될 때까지 최대 120초 대기
4. **프론트엔드** — 새 창에서 `cd frontend; npm run dev`, 최대 30초 대기
5. **상태 요약 + 브라우저 오픈** — 무엇이 어떤 방식으로 떠 있는지 표로 출력한 뒤
   `http://localhost:5173` 오픈

## 멱등성의 정확한 범위 (중요)

**멱등성은 "소스로 떠 있을 때만" 적용된다.** 이미 소스 백엔드가 8080에 있으면 재시작하지 않고
통과하지만, **컨테이너가 8080을 잡고 있으면 통과시키지 않는다.**

| 8080 점유 주체 | 스크립트 동작 |
| --- | --- |
| 아무도 없음 | `bootRun` 기동 |
| 소스 `java` | 재시작하지 않고 통과 (멱등) |
| `mins-backend` 컨테이너 | 이미지 빌드일을 알린 뒤 `docker compose stop backend frontend` → 포트 해제 대기 → `bootRun` |
| 그 외 컨테이너 | **`exit 1`로 중단.** 남의 컨테이너는 자동으로 끄지 않고 `docker stop` 명령만 안내 |

> 예전에는 이 문서에 "모두 멱등적이라 이미 떠 있는 서비스는 그대로 통과한다"고만 적혀 있었다.
> 그 문장 때문에 2026-08-02에 빌드된 `mins-backend` 컨테이너가 8080을 선점한 상태가
> "정상 통과"로 보였고, 소스 실행이 조용히 스킵된 채 낡은 백엔드가 최신 코드인 척 떴다.
> (2026-08-07 수정)

## 마지막 요약을 반드시 읽을 것

스크립트 끝에 나오는 표가 이 결함의 재발을 잡는 장치다.

```
구성요소    방식              주소                     근거
DB        Docker (mins-db)  localhost:5433           healthy
백엔드     소스 bootRun       http://localhost:8080    java (PID 61864)
프론트엔드  소스 Vite dev      http://localhost:5173    node (PID 42252)

OK: 8080 = 소스 실행 (컨테이너 아님). 최신 코드가 반영되어 있습니다.
```

- `근거` 열이 `java`면 소스, `wslrelay`/`com.docker.backend`면 컨테이너다
- 마지막 줄이 `OK:`가 아니라 `!!`로 시작하면 **낡은 이미지를 보고 있는 것**이다

## 참고

- 백엔드/프론트엔드는 각각 새 PowerShell 창에서 실행되므로, 에러 로그는 해당 창에서 직접 확인해야 한다.
- 포트 확인은 `Get-NetTCPConnection`을 사용하므로 Windows 환경 전용이다.
- 스크립트 원본: `scripts/dev-up.ps1`, 감지·판정 함수: `scripts/lib/DevEnv.ps1`
- 판정 로직 회귀 테스트 (Docker 없이 실행 가능):
  `powershell -ExecutionPolicy Bypass -File .\scripts\tests\Test-DevUpLogic.ps1`