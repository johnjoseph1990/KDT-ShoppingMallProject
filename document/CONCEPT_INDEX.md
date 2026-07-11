# 🗂️ 개념 인덱스 (Concept Index)

> 전체 커리큘럼 목차(`인프라 강의 - 풀스택 모든 강의 정리.xlsx`, 13개 강의)를 기반으로,
> **이 쇼핑몰 프로젝트에 실제로 들어가는 개념**을 계층/기능별로 정리한 색인.
> 기능 ID는 [`FUNCTIONAL_SPEC.md`](./FUNCTIONAL_SPEC.md), 일정은 [`SPRINT_PLAN.md`](./SPRINT_PLAN.md), 장기 학습은 [`LEARNING_PLAN.md`](./LEARNING_PLAN.md) 기준.

**사용법:** 스프린트 중 어떤 코드가 이해 안 되면 → 아래 표에서 개념을 찾고 → "강의 위치"의 해당 섹션만 골라 본다. (강의를 순서대로 다 보는 용도가 아니라, **역방향 사전**으로 쓰는 문서)

---

## 0. 커리큘럼 구성과 프로젝트 관련도

| # | 강의 (엑셀 시트) | 핵심 주제 | 프로젝트 관련도 |
|---|------------------|-----------|:---:|
| 1 | 제대로 파는 자바 | Java 문법·OOP·컬렉션·스트림·예외 | 🔴 필수 |
| 2 | 스프링 핵심 원리 - 기본편 | IoC/DI·빈·컨테이너·컴포넌트 스캔 | 🔴 필수 |
| 3 | 스프링 MVC 1편 | HTTP·서블릿·MVC 구조·요청/응답 처리 | 🔴 필수 |
| 4 | 스프링 MVC 2편 | 검증·로그인(쿠키/세션)·필터/인터셉터·API 예외 처리 | 🔴 필수 |
| 5 | 스프링 DB 1편 | JDBC·커넥션 풀·**트랜잭션**·DB 락·예외 추상화 | 🔴 필수 |
| 6 | 스프링 DB 2편 | JdbcTemplate·MyBatis·JPA·**스프링 데이터 JPA**·트랜잭션 전파 | 🔴 필수 |
| 7 | 자바 ORM 표준 JPA - 기본편 | 영속성 컨텍스트·**연관관계 매핑**·JPQL·페치 조인 | 🔴 필수 |
| 8 | 스프링 부트 - 핵심 원리와 활용 | 내장 톰캣·실행 가능 JAR·**외부 설정/@Profile**·액츄에이터 | 🟡 배포 시 |
| 9 | html 강의 | HTML·CSS·Flexbox/Grid·반응형 | 🟡 프론트 기반 |
| 10 | 자바스크립트 강의 | JS 문법·DOM·이벤트·**Promise/async·map/filter** | 🟡 React 전제조건 |
| 11 | 따라하며 배우는 도커와 CI환경 | Docker·Compose·CI/CD·EB 배포 | 🟡 배포 시 |
| 12 | GIT&GITHUB | 커밋·브랜치·머지·PR | 🔴 매일 사용 |
| 13 | AWS | IAM·EC2·RDS·S3·CloudFront 등 | 🟡 배포 시 |

> 🔴 = 스프린트 Day 2~4 백엔드 코드에 직결 / 🟡 = 특정 Day(프론트·배포)에만 필요

---

## 1. Java 언어 기초 — 모든 코드의 재료

강의: **제대로 파는 자바**

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 (섹션/강의명) |
|------|---------------------------|------------------------|
| 클래스·필드·메소드·접근 제어자 | 모든 엔티티/서비스/컨트롤러 | 클래스 기초 ~ 접근 제어자 |
| 상속·다형성·인터페이스 | `Repository` 인터페이스, 서비스 추상화 | 상속 / 다형성 / 인터페이스 |
| **열거형(enum)** | 주문 상태 `ORDERED→PAID→SHIPPING→DELIVERED`, 회원 `Role(USER/ADMIN)`, 결제 `SUCCESS/FAILED` | 열거형 |
| **레코드(record)** | 요청/응답 DTO (`ProductResponse` 등) | 레코드 (Java 16+) |
| 제네릭 | `List<Product>`, `JpaRepository<Product, Long>` | 제네릭 |
| 컬렉션 (List/Set/Map) | 장바구니 목록, 주문상품 목록 | 리스트 / 셋 / 맵 |
| **람다·스트림** | 총액 계산(`mapToInt().sum()`), DTO 변환(`stream().map()`), 별점 평균 | 람다식 ~ 스트림 연산 (상/하) |
| **예외 처리·커스텀 예외** | `OutOfStockException`(재고 부족), `try-catch`, 예외 되던지기 | 예외처리 ~ 예외 정의하고 발생시키기 |
| Optional | `repository.findById()` 반환값 처리 | NPE와 Optional |
| 어노테이션 | `@Entity`, `@Service` 등 모든 스프링 어노테이션 이해의 기반 | 표준 & 메타 어노테이션 / 리플렉션 |
| 날짜/시간 클래스 | `created_at`, `paid_at` (`LocalDateTime`) | 날짜와 시간 관련 클래스들 |
| 빌드 도구 (Gradle) | Spring Boot 프로젝트 빌드/실행 | 빌드 도구 (+ Gradle 사용해보기) |

> ⚪ 이 프로젝트에서 **안 쓰는 것**: 쓰레드/동기화(동시성은 DB 락으로 처리), 소켓 프로그래밍, I/O 스트림, 직렬화, 리플렉션 직접 사용

---

## 2. 스프링 핵심 — 코드가 "저절로" 연결되는 원리

강의: **스프링 핵심 원리 - 기본편**

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| **IoC / DI / 컨테이너** | Controller에 Service가, Service에 Repository가 자동으로 꽂히는 원리 | IoC, DI, 그리고 컨테이너 |
| 컴포넌트 스캔 (`@Component/@Service/@Repository/@Controller`) | 클래스에 어노테이션만 붙이면 빈으로 등록되는 이유 | 컴포넌트 스캔과 의존관계 자동 주입 |
| **생성자 주입** | `private final ProductRepository...` + 생성자 (롬복 `@RequiredArgsConstructor`) | 생성자 주입을 선택해라! / 롬복과 최신 트랜드 |
| 싱글톤 컨테이너 | 서비스 빈이 하나만 만들어짐 → **필드에 상태를 두면 안 되는 이유** | 웹 애플리케이션과 싱글톤 ~ 싱글톤 방식의 주의점 |
| `@Configuration`·수동 빈 등록 | `SecurityConfig`, `PasswordEncoder` 빈 등록 | @Configuration과 싱글톤 / 자동, 수동의 올바른 실무 운영 기준 |
| SOLID·관심사의 분리 | Controller(요청) / Service(비즈니스) / Repository(저장) 계층 분리 근거 | 좋은 객체 지향 설계의 5가지 원칙 / 관심사의 분리 |

---

## 3. 웹 & API 계층 — 요청이 들어와서 JSON이 나가기까지

강의: **스프링 MVC 1편** (구조) + **스프링 MVC 2편** (활용)

### 3-1. HTTP와 MVC 구조 (MVC 1편)

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| 웹 서버 vs WAS, 서블릿 | Spring Boot 내장 톰캣 위에서 API가 도는 구조 | 웹 서버, 웹 애플리케이션 서버 / 서블릿 |
| **HTML vs HTTP API (CSR/SSR)** | 우리는 화면(타임리프) 대신 **JSON API + React(CSR)** 구조를 선택 | HTML, HTTP API, CSR, SSR |
| 스프링 MVC 전체 구조 (DispatcherServlet) | `@GetMapping` 하나가 호출되기까지의 내부 흐름 | 스프링 MVC 전체 구조 / 핸들러 매핑과 핸들러 어댑터 |
| 요청 매핑 (`@GetMapping/@PostMapping`, 경로 변수) | `GET /api/products/{id}`, REST API 설계 전반 | 요청 매핑 / 요청 매핑 - API 예시 |
| `@RequestParam` | 검색어(P-4), 페이지 번호, 상태 필터(A-1) | HTTP 요청 파라미터 - @RequestParam |
| **`@RequestBody` + JSON** | 상품 등록, 회원가입, 주문 생성 등 모든 POST 요청 바디 | HTTP 요청 메시지 - JSON |
| **HTTP 메시지 컨버터** | 객체 ↔ JSON 자동 변환(`@RestController`)의 원리 | HTTP 메시지 컨버터 |
| 로깅 (SLF4J) | `log.info()` — 디버깅의 기본 | 로깅 간단히 알아보기 |

### 3-2. 검증·인증·예외 (MVC 2편)

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| **Bean Validation** (`@NotBlank`, `@Min` 등) | 회원가입 폼 검증, 상품 등록 검증(가격>0, 별점 1~5) | 검증2 - Bean Validation 섹션 전체 |
| Form 전송 객체 분리 (DTO) | 엔티티를 API에 직접 노출하지 않고 요청/응답 DTO를 따로 두는 이유 | Form 전송 객체 분리 - 소개/개발 |
| **쿠키·세션 로그인** | M-2 로그인/로그아웃 (세션 방식 선택 시의 원리) | 로그인 처리 - 쿠키, 세션 섹션 전체 |
| **필터·인터셉터 (인증 체크)** | 비로그인 접근 차단, ADMIN 권한 체크(M-3) — Spring Security가 내부에서 쓰는 원리 | 서블릿 필터 - 인증 체크 / 스프링 인터셉터 - 인증 체크 |
| **API 예외 처리** (`@ExceptionHandler`, `@RestControllerAdvice`) | 재고 부족·존재하지 않는 상품 등을 일관된 JSON 에러로 응답 | @ExceptionHandler / @ControllerAdvice |
| 타입 컨버터 | 요청 파라미터 `"1"` → `Long id` 자동 변환 | 스프링 타입 컨버터 소개 |
| ⚪ 파일 업로드 | **범위 제외** — 이미지는 URL만 저장하기로 결정 | (파일 업로드 섹션 — 참고만) |
| ⚪ 타임리프·메시지/국제화 | **사용 안 함** — 화면은 React가 담당 | (타임리프 섹션 — 건너뜀) |

---

## 4. DB & JPA — 이 프로젝트의 심장

강의: **스프링 DB 1편** (원리) + **JPA 기본편** (매핑) + **스프링 DB 2편** (활용)

### 4-1. 트랜잭션과 커넥션 (DB 1편)

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| JDBC·커넥션 풀·DataSource | 모든 DB 접근의 바닥. `application.yml`의 DB 설정이 뭘 만드는지 | JDBC 이해 / 커넥션 풀과 DataSource 이해 |
| **트랜잭션 (`@Transactional`)** | **주문 생성(O-2)**: 주문 저장+재고 차감+장바구니 비우기가 전부 성공하거나 전부 취소되어야 함 | 트랜잭션 - 개념 이해 ~ 트랜잭션 AOP 적용 |
| **DB 락** | 재고 동시성(O-5)의 배경 개념 (비관적 락 = `SELECT FOR UPDATE`) | DB 락 - 개념 이해/변경/조회 |
| 체크/언체크 예외, 스프링 예외 추상화 | 서비스 계층에서 `RuntimeException` 기반 커스텀 예외를 쓰는 이유 | 예외 이해와 기본 규칙 / 스프링 예외 추상화 |

### 4-2. 엔티티 매핑과 연관관계 (JPA 기본편) — ERD와 1:1 대응

| 개념 | 이 프로젝트에서 쓰이는 곳 (ERD 참조) | 강의 위치 |
|------|---------------------------|-----------|
| 영속성 컨텍스트·플러시 | `save()` 없이도 변경이 저장되는 **변경 감지(dirty checking)** — 주문 상태 변경(A-2)이 이 원리 | 영속성 컨텍스트 1, 2 / 플러시 |
| 객체-테이블 매핑 (`@Entity`, `@Column`) | 8개 테이블 전부 (`member`, `product`, `orders` ...) | 객체와 테이블 매핑 / 필드와 컬럼 매핑 |
| 기본 키 매핑 (`@GeneratedValue`) | 모든 테이블의 `bigint id PK` | 기본 키 매핑 |
| **다대일 [N:1] 단방향/양방향** | `order_item→orders`, `order_item→product`, `review→member` 등 FK 전부 | 단방향 연관관계 ~ 다대일 [N:1] |
| **연관관계의 주인** | `Orders ↔ OrderItem` 양방향 매핑 시 `mappedBy`를 어디에 쓰는가 | 양방향 연관관계와 연관관계의 주인 1, 2 |
| **다대다 [N:M] 해소** | `order_item`, `cart_item`이 존재하는 이유 (ERD 설계 결정 #1) | 다대다 [N:M] |
| 일대일 [1:1] | `orders ↔ payment` | 일대일 [1:1] |
| **즉시/지연 로딩 (`FetchType.LAZY`)** | 모든 `@ManyToOne`은 LAZY로 — 성능 문제의 예방 | 프록시 / 즉시 로딩과 지연 로딩 |
| 영속성 전이 (CASCADE) | 주문 저장 시 `order_item`도 함께 저장 | 영속성 전이(CASCADE)와 고아 객체 |
| DB 스키마 자동 생성 (`ddl-auto`) | 개발 중 H2에 테이블 자동 생성 | 데이터베이스 스키마 자동 생성 |
| **JPQL — 페이징** | 상품 목록 페이징(P-1) | 페이징 |
| **JPQL — 집계·정렬** | **평균 별점(R-2) `AVG(rating)`**, 별점순 정렬(R-3), 베스트 상품(R-5) | 기본 문법과 쿼리 API / 프로젝션 |
| 조인·서브쿼리 | 태그 필터(R-4), 키워드 추천(R-6) — 같은 태그의 상품 찾기 | 조인 / 서브 쿼리 |
| **페치 조인 (N+1 문제)** | 주문 내역 조회(O-6) 시 주문상품·상품을 한 방에 가져오기 | 페치 조인 1, 2 |
| ⚪ 상속관계 매핑·임베디드 타입 | 이 프로젝트 ERD에는 없음 | (참고만) |

### 4-3. 스프링 데이터 JPA와 실전 조합 (DB 2편)

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| **스프링 데이터 JPA (`JpaRepository`)** | `ProductRepository extends JpaRepository` — 인터페이스만으로 CRUD 완성 | 스프링 데이터 JPA 소개1, 2 / 주요 기능 |
| 쿼리 메소드 (`findByNameContaining`) | 상품 검색(P-4), 회원별 주문 조회(O-6) | 스프링 데이터 JPA 주요 기능 |
| 테스트 + `@Transactional` 롤백 | 통합 테스트(Day 6)에서 테스트마다 DB가 깨끗한 이유 | 테스트 - 데이터 롤백 / @Transactional |
| 트랜잭션 적용 위치·프록시 주의사항 | `@Transactional`이 안 먹는 함정 (내부 호출) | 트랜잭션 AOP 주의 사항 - 프록시 내부 호출 |
| 예외와 커밋/롤백 | 재고 부족 예외 발생 시 주문 전체가 롤백되는 원리 | 예외와 트랜잭션 커밋, 롤백 |
| ⚪ MyBatis·JdbcTemplate·Querydsl | 이 프로젝트는 JPA + 스프링 데이터 JPA만 사용 | (참고만) |

---

## 5. 스프링 부트 & 운영 — 배포 주간(Day 6~7)에 꺼내 볼 것

강의: **스프링 부트 - 핵심 원리와 활용**

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| 내장 톰캣·실행 가능 JAR | `./gradlew build` → `java -jar app.jar`로 서버에서 실행(D-2) | 내장 톰캣 / 실행 가능 JAR 파일 섹션 |
| 스프링 부트 스타터·자동 구성 | `spring-boot-starter-web` 하나로 다 되는 이유 | 스프링 부트 스타터 / 자동 구성의 이해 |
| **외부 설정·`@Profile`** | **H2(로컬) → PostgreSQL(배포) 전환(D-1)** — `application-prod.yml`, 환경 변수로 DB 비밀번호 주입 | 외부 설정의 이해 / YAML / @Profile |
| ⚪ 액츄에이터·프로메테우스/그라파나 | 과제 범위 밖 (여유 시 헬스체크 정도) | (참고만) |

---

## 6. 프론트엔드 — Day 5의 재료

강의: **html 강의** + **자바스크립트 강의**

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| HTML 구조·시맨틱 요소 | React JSX도 결국 HTML — 태그의 의미 | HTML 기초 섹션 |
| CSS 기초·Box Model | 컴포넌트 여백/테두리 조정 | CSS 기초 및 스타일링 섹션 |
| **Flexbox / Grid** | **상품 카드 그리드(메인 화면)**, 헤더/사이드바 배치 | CSS Flexbox 레이아웃 / Grid CSS |
| Media Query | 반응형 (여유 시) | Media Query |
| `let/const`, 스코프, 템플릿 리터럴 | 모든 React 코드의 기초 문법 | var, let, const ~ Template Literals |
| **구조 분해·전개 연산자** | `const { name, price } = product`, `setState({...cart})` — React 필수 관용구 | 구조 분해 할당 / 전개 연산자 |
| **map / filter / reduce** | 상품 배열 → 카드 목록 렌더링(`products.map(...)`), 장바구니 총액(reduce) | Map, Filter, Reduce |
| **Promise·async/await** | `fetch()`로 백엔드 API 호출 — 첫 풀스택 연결의 핵심 | ES6 Promise / Async, Await |
| DOM·이벤트 | React가 대신 해주는 일이 무엇인지 이해하는 배경 | DOM이란? / Event Listener |
| 얕은/깊은 복사 | React 상태를 직접 수정하면 안 되는(불변성) 이유 | 얕은 복사 vs 깊은 복사 |
| this·클로저·이벤트 루프 | 디버깅 시 배경지식 | this 키워드 / Closure / Event Loop |

---

## 7. 인프라 — Git(매일) / Docker·AWS(Day 6~7)

강의: **GIT&GITHUB** + **따라하며 배우는 도커와 CI환경** + **AWS**

| 개념 | 이 프로젝트에서 쓰이는 곳 | 강의 위치 |
|------|---------------------------|-----------|
| add·commit·push | **매일 커밋 원칙** (스프린트 원칙 #2) | 첫 번째 버전 만들기 ~ GitHub에 올리기 |
| 브랜치·머지·충돌 해결 | 기능별 브랜치 작업 시 | 브랜치 / 머지 / 컨플릭트 |
| Pull Request | 코드 리뷰 흐름 (개인 프로젝트라도 연습 가치) | 풀 리퀘스트 |
| 도커 이미지·컨테이너·Dockerfile | 백엔드/프론트를 컨테이너로 포장해 배포(D-2) | 도커 기초 / Dockerfile 만들기 |
| **Docker Compose** | 백엔드+프론트+DB를 한 번에 띄우기 (로컬 통합 테스트에도 유용) | Docker Compose 파일 작성하기 |
| 운영용 빌드 + Nginx | React 정적 빌드 서빙 | 운영환경을 위한 Nginx |
| **CI/CD (GitHub Actions)** | push → 자동 빌드/배포 (여유 시) | Github Action으로 교체하기 |
| IAM | AWS 계정 권한 관리 (배포 첫 단계) | 섹션 2: IAM |
| **EC2 / Elastic Beanstalk** | 백엔드 서버 올리기(D-2) | 섹션 3: EC2 / EB 환경 구성 (도커 강의) |
| **RDS** | **배포용 PostgreSQL(D-1)** — 강의는 MySQL이지만 절차 동일 | 섹션 4: RDS / MYSQL을 위한 AWS RDS 생성 |
| Security Group·VPC | DB는 백엔드에서만 접근 가능하게 막기 | VPC와 Security Group 설정하기 |
| S3 (+CloudFront) | React 정적 파일 호스팅 대안 | 섹션 5: S3 / 섹션 8: CloudFront |
| ⚪ Lambda·DynamoDB·ElastiCache 등 | 과제 범위 밖 | (참고만) |

---

## 8. ⚠️ 커리큘럼에 **없는** 개념 (프로젝트에 필요한데 강의가 안 다루는 것)

> 여기가 이 인덱스의 가장 중요한 표. 이 항목들은 공식 문서 + AI 페어로 보완해야 한다.

| 개념 | 어디에 필요한가 | 커리큘럼에서 가장 가까운 것 | 보완 방법 |
|------|-----------------|------------------------------|-----------|
| **React** (컴포넌트·props·state·useEffect) | Day 5 화면 전체 | JS 강의(문법) + 도커 강의(도구로만 등장) | 공식 문서 react.dev, JS 강의를 전제조건으로 |
| **Spring Security + BCrypt** | M-1, M-2, M-3 (인증/인가) | MVC 2편의 세션 로그인·필터/인터셉터 (**원리는 동일** — Security는 이걸 프레임워크화한 것) | 공식 문서 + MVC 2편 로그인 섹션을 먼저 이해 |
| **JWT** | 토큰 방식 선택 시(M-2) | MVC 2편 쿠키·세션 (대안 관계) | 세션 방식이 강의와 가까우므로 **세션 우선 고려** |
| **JPA 낙관적 락 `@Version`** | O-5 재고 동시성 (ERD 설계 결정 #4) | DB 1편 "DB 락"(비관적 락 개념) | JPA 공식 문서 — DB 1편의 락 개념을 먼저 이해하면 차이가 보임 |
| **PostgreSQL** | D-1 배포 DB | DB 강의는 H2, 도커/AWS 강의는 MySQL | 설정 문자열(driver·dialect)만 다름 — 전환 절차는 RDS 강의 그대로 |
| **CORS** | React(3000) ↔ Spring(8080) 연결 시 반드시 만남 | MVC 1편 HTTP 이해가 배경 | `WebMvcConfigurer.addCorsMappings` — 첫 fetch 에러 때 학습 |
| Swagger (springdoc) | API 문서화 (LEARNING_PLAN Phase 2) | 없음 | 의존성 추가만으로 동작, 여유 시 |
| Vapor (goorm 디자인 시스템) | Day 5 UI 컴포넌트 | 없음 | https://design.vapor.codes/ — React 이해가 전제 |

---

## 9. 빠른 참조 — Sprint Day별 / Learning Phase별 매핑

### Sprint (1주 제출용)

| Day | 작업 | 꺼내 볼 개념 (섹션 번호) |
|-----|------|--------------------------|
| 1 | 설계·환경 | §7 Git 기초, §4-2 연관관계(ERD 이해), §1 Gradle |
| 2 | 상품 CRUD·인증 | §3-1 요청 매핑/JSON, §4-2 엔티티 매핑, §4-3 JpaRepository, §3-2 검증·세션·필터 + **§8 Security(갭)** |
| 3 | 주문·결제·재고 | §4-1 **트랜잭션**, §4-2 연관관계·CASCADE, §1 enum, §4-1 DB 락 + **§8 @Version(갭)** |
| 4 | 리뷰·관리자 | §4-2 JPQL 집계·정렬·조인, §3-2 인터셉터(권한), §3-2 API 예외 처리 |
| 5 | React 화면 | §6 전체 + **§8 React·CORS·Vapor(갭)** |
| 6~7 | 배포 | §5 외부 설정·JAR, §7 Docker·RDS·EB + **§8 PostgreSQL(갭)** |

### Learning Plan (제출 후 16~20주 심화)

| Phase | 대응 커리큘럼 강의 |
|-------|--------------------|
| 0. 기초 체력 | 제대로 파는 자바 → MVC 1편 앞부분(웹 애플리케이션 이해) |
| 1~2. 수직 슬라이스·CRUD | 스프링 핵심 원리 → JPA 기본편 → DB 2편(스프링 데이터 JPA) → MVC 1편 |
| 3. JS + React | html 강의 → 자바스크립트 강의 → React(커리큘럼 외) |
| 4. 회원 & 인증 | MVC 2편(로그인·필터/인터셉터) → Spring Security(커리큘럼 외) |
| 5~6. 주문·재고·관리자 | JPA 기본편(연관관계) → DB 1편(트랜잭션·락) → DB 2편(트랜잭션 심화) |
| 7. 리뷰 & 추천 | JPA 기본편(JPQL·집계·페치 조인) |
| 8. 배포 & 정리 | 스프링 부트 → GIT&GITHUB → 도커와 CI환경 → AWS |
