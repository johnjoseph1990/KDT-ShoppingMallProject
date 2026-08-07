# 🛒 KDT Shopping Mall Project

KDT(K-Digital Training) 과정 **[개인 프로젝트] 쇼핑몰 만들기** 과제로 진행하는 Spring 기반 쇼핑몰 웹 애플리케이션입니다.

사용자·상품·주문·결제 등 쇼핑몰의 핵심 도메인을 직접 설계하고, 프론트엔드 → 백엔드 → 데이터베이스로 이어지는 전체 서비스 흐름을 구현하는 것을 목표로 합니다.

---

## 📌 프로젝트 목표

### 1. 쇼핑몰 서비스의 기본 구조 구현
- 사용자, 상품, 주문, 결제 등 **핵심 도메인 설계 및 구현**
- 프론트엔드, 백엔드, 데이터베이스가 연결되는 **전체 흐름 이해**

### 2. Spring 기반 CRUD 및 인증 기능 구현
- 상품 / 회원 / 주문 데이터의 **등록·조회·수정·삭제(CRUD)** 기능 구현
- **회원가입, 로그인, 권한 분리** 등 인증 처리 로직 구현

### 3. 주문 및 재고 처리 흐름 고도화
- 주문 상태 흐름 정의 및 **단계별 상태 변경 로직** 구현
- **동시 주문 상황에서도 정확한 재고 처리**가 가능한 재고 동기화 구조 설계

### 4. 관리자 기능 구현
- 관리자 관점의 **상품 및 주문 내역 관리** 기능 구현
- 주문 상태 변경, 재고 확인 등 **운영 기능** 구성

### 5. 리뷰 기반 추천 및 UX 개선
- 리뷰 **작성 / 조회 / 삭제** 기능 구현
- **별점 평균, 키워드, 태그**를 활용한 정렬·추천·필터링 기능 구성

### 6. 배포 및 결과물 정리
- 전체 시스템 테스트 후 **클라우드 배포** — 과제 원문 표기는 AWS이나, 실제 배포는 **Azure**로 진행했습니다 (Step 7 요구사항의 "Azure 배포"를 따름)
- 기획 문서, DB 설계 문서, 기능 명세, 발표자료 등 **결과물 정리**

---

## 🎓 학습 관점 (Learning Perspective)

> 이 프로젝트의 **결과물은 쇼핑몰**이지만, **목적은 아래 개념들을 직접 부딪히며 체득**을 목표로 했습니다.
> 기능을 "돌아가게" 만드는 것과, "왜 이렇게 만드는지" 설명할 수 있는 것은 다릅니다. 각 기능을 구현할 때 아래의 *핵심 개념*과 *왜 중요한가*를 스스로 설명해 보세요.

| 구현 영역 | 익히는 핵심 개념 | 왜 중요한가 |
|-----------|------------------|-------------|
| **도메인 설계 (ERD)** | 엔티티, 연관관계(1:N, N:M), 정규화 | 현실의 비즈니스를 데이터 모델로 옮기는 사고력 — 여기서 틀리면 뒤가 다 흔들립니다 |
| **CRUD & JPA** | 영속성 컨텍스트, 트랜잭션, 지연 로딩 | 데이터를 다루는 가장 기본기. SQL을 직접 안 쓰고도 무슨 쿼리가 나가는지 아는 것이 핵심 |
| **인증 / 인가** | 세션 vs 토큰, 비밀번호 해싱, 역할(Role) 기반 접근 제어 | "로그인 됨"과 "이 사람이 이걸 할 권한이 있음"은 다른 문제 — 보안의 출발점 |
| **주문 상태 흐름** | 상태 머신(State Machine), 상태 전이 규칙 | 복잡한 비즈니스 로직을 "허용된 전이만 가능하게" 안전하게 관리하는 법 |
| **재고 & 동시성** | 레이스 컨디션, 비관적 락 / 낙관적 락 | 실무에서 가장 까다로운 문제 중 하나. 재고 1개에 주문 2건이 동시에 들어오면? |
| **관리자 기능** | 권한 분리, 관리자/사용자 화면 경계 | 같은 데이터라도 "누가 보느냐"에 따라 접근 범위가 달라지는 설계 |
| **리뷰 / 추천** | 집계(평균 별점), 정렬, 필터링 쿼리 | 원시 데이터를 사용자에게 의미 있는 정보로 가공하는 능력 |
| **테스트 & 배포** | 예외 처리, 환경 분리(로컬/운영), Azure 배포 | "내 PC에선 됐는데"를 넘어 실제 서비스로 내보내는 경험 |

### 💡 학습 원칙
- **먼저 스스로 설계해 보고, 막히면 찾아본다.** 정답을 먼저 보면 "왜"가 남지 않습니다.
- **작게 만들고 자주 확인한다.** 한 기능이 끝날 때마다 실제로 동작시켜 보며 이해를 검증합니다.
- **커밋 메시지에 "무엇을"이 아니라 "왜"를 적는다.** 나중의 나에게 남기는 학습 기록입니다.

> ⚡ **실제 제출 계획(1주):** [`document/SPRINT_PLAN.md`](./document/SPRINT_PLAN.md) — 마감에 맞춘 일자별 계획 (AI 페어 + 이해 체크포인트)
> 📚 **제출 후 장기 학습 계획:** [`docs/learning/LEARNING_PLAN.md`](./docs/learning/LEARNING_PLAN.md) — 처음부터 다시 깊게 배우는 16~20주 계획

---

## 📝 학습 기록 (Learning Log)

학습하며 배운 것은 [`docs/learning/`](./docs/learning)에 기록합니다.
날짜별 노트는 `notes/`, 주제별 정리는 `topics/` — 인덱스는 [`docs/learning/README.md`](./docs/learning/README.md) 참고.

---

## 🧭 진행 단계 (Roadmap)

### Step 1. UI 가이드 학습 및 프로젝트 방향 설정
- [ ] [Vapor (Goorm Design System)](https://goormkdx.notion.site/Vapor-334c0ff4ce31809a9715f60a58152650?source=copy_link) 컴포넌트·스타일 가이드 학습
- [ ] 쇼핑몰 서비스에 필요한 주요 화면과 사용자 흐름 정리
- [ ] 개인 프로젝트 범위에 맞는 기능 우선순위 설정

### Step 2. 기능 명세 및 DB 설계
- [x] 사용자, 상품, 주문, 리뷰 등 핵심 도메인 정의
- [x] 테이블 구조 및 관계(ERD) 설계
- [x] 기능 명세서 / DB 설계 문서 작성 → [`document/`](./document)

### Step 3. Spring 기반 핵심 기능 구현
- [x] 상품 CRUD 기능 구현
- [x] 회원가입, 로그인, 인증 및 권한 관리 구현 (Spring Security)
- [ ] 상품 목록·상세·등록·수정 등 기본 쇼핑몰 화면 및 기능 구현 (프론트엔드)

### Step 4. 주문 및 결제 흐름 구현
- [x] 상품 구매 흐름 설계 및 주문 생성 기능 구현 (Cart → Order)
- [x] 주문 상태 관리 기본 로직 구성 (`OrderStatus`)
- [x] 주문 완료 이후 결제 및 처리 결과 흐름 연결 (모의 결제)

### Step 5. 재고 및 관리자 주문 처리 기능 확장
- [x] 상품별 재고 관리 로직 및 UI 구성 (목록 품절·마감임박 배지, 상세 재고 수량 표시)
- [x] 동시 주문 상황에서의 재고 동기화 처리 구조 구현 (`@Version` 낙관적 락)
- [ ] 관리자 주문 내역 조회 및 상태 변경 기능 구현

### Step 6. 리뷰 기반 추천 및 UX 개선 기능 확장
- [ ] 리뷰 작성 / 조회 / 삭제 기능 구현
- [ ] 평균 별점 계산, 별점 정렬, 키워드 기반 추천 기능 구현
- [x] 별점·태그·키워드 필터링 UI 및 베스트 상품 섹션 구성

### Step 7. 테스트, 배포 및 결과물 정리
- [x] 회원가입 → 주문 → 재고 → 리뷰 → 추천 전체 흐름 점검
  - 화면 경로 5개(홈·목록/상세·회원가입·장바구니·관리자)는 Playwright E2E 스모크로 검증 (`frontend/e2e/`, CI 자동 실행)
  - 리뷰·추천은 백엔드 테스트로 검증 (`ReviewServiceTest`, `ReviewControllerTest`, `ProductRepositoryBestProductsTest`)
- [x] 예외 상황 및 오류 케이스 테스트·수정 (재고 부족·동시 주문 충돌·비로그인 접근 등)
- [x] Azure 배포 (Container Apps + PostgreSQL Flexible Server, `azure/setup.sh`로 재현 가능)
- [x] 배포 링크 첨부 — https://www.minsdev.works
- [ ] 최종 결과물(프로젝트 소개 자료 / 발표자료) 정리
- [ ] 시연 영상 준비

#### 🚨 배포 전 체크리스트 (현재 개발 환경 전제로 꺼두거나 허술하게 둔 것들)

| 구분 | 현재 상태 | 배포 시 조치 |
|---|---|---|
| **CSRF** | `csrf.disable()` — Postman 테스트 편의로 비활성화 (`SecurityConfig.java:69`) | `CookieCsrfTokenRepository` 도입. React에서 쿠키의 `XSRF-TOKEN`을 읽어 `X-XSRF-TOKEN` 헤더에 실어 보내도록 Axios 인터셉터 추가 |
| **DB 비밀번호** | 기본값 `mins1234` (Docker 로컬 컨테이너용, `application.properties:9`) | `SPRING_DATASOURCE_PASSWORD` 환경변수로 실제 비밀번호 주입. 기본값 그대로 사용 금지 |
| **DDL 자동 변경** | `ddl-auto=update` — 엔티티 변경 시 스키마 자동 ALTER (`application.properties:14`) | `ddl-auto=validate` 로 변경 + Flyway 마이그레이션(`V1__`, `V2__`...)으로 스키마 이력 관리 |
| **SQL 로그 출력** | `show-sql=true` — 모든 쿼리가 콘솔에 출력 (`application.properties:15`) | `show-sql=false` 로 변경. 쿼리 분석이 필요하면 운영 로그 레벨 조정으로 대체 |
| **토스 시크릿 키** | 미설정 시 부팅은 되지만 결제 승인 호출 시 401 반환 (`application.properties:25`) | `TOSS_SECRET_KEY` 환경변수에 운영 발급 키 주입 (테스트 키 ↔ 운영 키 구분 필수) |
| ~~**DevDataInitializer**~~ ✅ | ~~`@Profile` 미설정 — 운영 서버에서도 시드 데이터가 삽입됨~~ | `@Profile("dev")` 추가 완료. `SPRING_PROFILES_ACTIVE=prod` 시 빈 등록 안 됨 |
| **CORS** | 별도 CORS 설정 없음 — Spring Boot 기본값(동일 출처만 허용) | 프론트 배포 도메인을 `allowedOrigins`에 명시. `*` 와일드카드 사용 금지 |
| **HTTPS / 쿠키 보안** | 로컬 HTTP 전제 — 세션 쿠키에 `Secure`, `SameSite` 미설정 | HTTPS 적용 후 `server.servlet.session.cookie.secure=true`, `same-site=Strict` 설정 추가 |

### Step 8. 과제 제출
- [ ] 최종 결과물 + 시연 영상을 하나의 파일로 압축
- [ ] 파일로 과제 제출

---

## ✨ 주요 기능

| 구분 | 기능 |
|------|------|
| **회원** | 회원가입, 로그인/로그아웃, 인증·인가(권한 분리) |
| **상품** | 상품 목록/상세 조회, 등록/수정/삭제, 재고 관리 |
| **장바구니** | 장바구니 담기/수량 변경/삭제 |
| **주문** | 장바구니 기반 주문 생성, 주문 상태 흐름 관리, 결제 흐름 연결 |
| **재고** | 상품별 재고 관리, 동시 주문 재고 동기화 처리 |
| **관리자** | 상품 관리, 주문 내역 조회, 주문 상태 변경, 재고 확인 |
| **리뷰** | 리뷰 작성/조회/삭제, 평균 별점 계산 |
| **추천/UX** | 별점 정렬, 키워드 기반 추천, 태그·키워드 필터링, 베스트 상품 섹션 |

---

## 🛠 기술 스택

> 프로젝트 진행에 따라 확정 예정

| 구분 | 기술 |
|------|------|
| **Backend** | Java, Spring Boot, Spring Data JPA, Spring Security |
| **Frontend** | React, JavaScript, Vapor (goorm Design System) |
| **Database** | PostgreSQL (학습 초기에는 H2로 시작 후 전환) |
| **Infra** | Azure (Container Apps, Container Registry, PostgreSQL Flexible Server, Blob Storage) |
| **Build/Tool** | IntelliJ IDEA, Gradle, Git/GitHub, Postman |

---

## 📂 프로젝트 구조

```
KDT-ShoppingMallProject/
├── backend/            # Spring Boot (Java 21, Gradle) - Web / Data JPA / Security / H2 / Validation / Lombok
│   └── src/main/java/com/kdt/shoppingmall/
│       ├── domain/     # member, product, cart, order, payment
│       ├── controller/ # Auth, Product, Cart, Order
│       ├── service/    # 도메인별 비즈니스 로직
│       └── repository/ # Spring Data JPA repository
├── frontend/            # React (Vite)
├── document/            # 기획서, 기능 명세서, DB 설계 문서(ERD), 개념 인덱스
└── README.md
```

---

## 🚀 시작하기

```bash
# 저장소 클론
git clone <repository-url>
cd KDT-ShoppingMallProject

# DB 실행 (Docker 필요) — 기본 설정이 PostgreSQL(localhost:5433)이라 이 단계 없이
# 바로 bootRun 하면 DB 연결 실패로 뜨지 않는다
docker compose up -d db

# 백엔드 실행 (http://localhost:8080)
cd backend
./gradlew bootRun

# 프론트엔드 실행 (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Windows에서는 위 세 단계를 스크립트 하나로 대체할 수 있다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1
```

---

## 📄 산출물

| 산출물 | 위치 |
|--------|------|
| 기획 문서 | `document/` |
| 기능 명세서 | `document/` |
| DB 설계 문서 (ERD) | `document/` |
| 발표자료 / 프로젝트 소개 자료 | `document/` |
| 배포 링크 | (배포 후 추가) |
| 시연 영상 | (제출 시 첨부) |

---

## 🔗 참고 자료

- [Vapor — Goorm Design System](https://goormkdx.notion.site/Vapor-334c0ff4ce31809a9715f60a58152650?source=copy_link)
