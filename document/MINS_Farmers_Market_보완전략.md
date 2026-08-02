# MINS Farmers Market — 보완 전략

> 평가 리포트 기준, "구매 여정이 끊기는 지점"부터 고치는 원칙으로 **P0(치명) → P1(사용성) → P2(품질)** 순으로 정리.

## 재점검 결과 (2026-07-24)

이 문서는 **백엔드가 붙기 전, 목데이터 기반 프로토타입** 단계를 평가한 리포트를 바탕으로 작성됐다. 이후 Spring Boot 백엔드가 붙으면서 아래 항목들은 이미 해결됨을 실제 코드로 확인했다.

| 항목 | 상태 | 근거 |
|---|---|---|
| P0-1 "전체" 필터 버그 | ✅ 해결됨 | `ProductListPage.jsx` — `all`은 `tag: ''`로 매핑, 빈 태그는 파라미터 자체를 생략 |
| P0-2 홈/쇼핑 데이터 이원화 | ✅ 해결됨 | 홈(`getBestProducts`)·쇼핑(`getProducts`) 모두 백엔드 `/api/products` 단일 소스 |
| P1-3 상품 상세 페이지 라우팅 | ✅ 해결됨 | `ProductDetailPage.jsx` + `/products/:id` 라우트 존재 |
| P1-4 장바구니→로그인 리다이렉트 | ✅ 해결됨 | `navigate('/login', { state: { from: location } })` 구현됨 |
| P1-5 상품별 실제 이미지 | ✅ 해결됨 | `product.imageUrl` 사용 |
| P2-6 404 페이지 | ✅ 해결됨 | `NotFoundPage.jsx` 존재 |
| P2-7 네비게이션 시맨틱 교체 | ⚠️ **부분 해결 — 남은 작업** | `Navbar.jsx` 메인 메뉴는 `Link`/`NavLink`로 교체됨. 단 상품 카드 클릭, "전체 보기"/"이번 주 수확물 보기" 등 다수 버튼은 여전히 `<div>`/`<span onClick={() => navigate(...)}>` 패턴 (`HomePage.jsx`, `ProductListPage.jsx`, `ProductDetailPage.jsx`, `StoryPage.jsx`, `CartPage.jsx`, `OrderDetailPage.jsx`, `Footer.jsx` 등) |
| P2-8 모바일 햄버거 메뉴 | ✅ 해결됨 | `Navbar.jsx` — `menuOpen` state로 구현됨 |

**남은 실제 작업은 P2-7 하나뿐.** 아래 원본 계획 중 이 항목만 유효하다.

---

## P0 — 데이터 정합성 (이번 주 안에) — ✅ 전체 해결됨, 아래는 원본 기록 보존용

### 1. "전체" 필터 버그

원인은 거의 확실히 필터 로직에서 `all`을 카테고리 값으로 비교하고 있기 때문. 전형적인 실수 패턴:

```jsx
// ❌ 버그: "all"이라는 카테고리를 가진 상품은 없음
const filtered = products.filter(p => p.category === selectedCategory);

// ✅ 수정: "all"이면 필터를 건너뜀
const filtered = selectedCategory === 'all'
  ? products
  : products.filter(p => p.category === selectedCategory);
```

### 2. 홈/쇼핑 데이터 이원화 통합

홈과 쇼핑이 각각 다른 배열(또는 다른 API)을 쓰고 있는 상태. 해결 방향:

- 상품 데이터 소스를 **하나로 통일** (백엔드 API가 있으면 `/api/products` 하나, 프론트 목데이터면 `products.js` 파일 하나)
- 홈 "이번 주의 수확"은 별도 데이터가 아니라 **같은 소스에서 필터링** (`featured: true` 플래그 or 최신순 상위 N개)

```js
// data/products.js — 단일 소스
export const products = [
  { id: 1, name: '설향딸기', category: 'produce', featured: true, ... },
];

// 홈: products.filter(p => p.featured)
// 쇼핑: products 전체 + 카테고리 필터
```

---

## P1 — 구매 여정 복구 (다음 단계) — ✅ 전체 해결됨, 아래는 원본 기록 보존용

### 3. 상품 상세 페이지 연결

React Router 기준:

```jsx
// 라우트 추가
<Route path="/products/:id" element={<ProductDetail />} />

// 카드 클릭
<Link to={`/products/${product.id}`}>...</Link>

// 상세 페이지에서
const { id } = useParams();
const product = products.find(p => p.id === Number(id));
```

### 4. 장바구니 → 로그인 흐름 개선

핵심은 두 가지: (a) 왜 이동하는지 알려주기, (b) 로그인 후 원래 자리로 복귀.

```jsx
// 담기 시도 시
if (!isLoggedIn) {
  alert('로그인 후 담을 수 있어요'); // 나중에 토스트로 교체
  navigate('/login', { state: { from: location.pathname, productId } });
}

// 로그인 성공 후
const from = location.state?.from || '/';
navigate(from);
```

### 5. 상품별 실제 이미지

이미지 촬영/수급이 어려우면 임시로 무료 이미지라도 상품별로 다르게 적용. 데이터에 `imageUrl` 필드를 추가하고 하드코딩된 이미지 경로를 제거.

---

## P2 — 품질 개선 — 7번만 남음 (6, 8은 해결됨)

### 6. 404 페이지

```jsx
<Route path="*" element={<NotFound />} />
// NotFound: 안내 문구 + <Link to="/">홈으로</Link> + <Link to="/shop">쇼핑하기</Link>
```

### 7. 네비게이션 시맨틱 교체 — 🔧 남은 작업

`<div onClick={...}>` / `<span onClick={() => navigate(...)}>` → `<Link to="...">`로 교체. 스타일은 그대로 유지하면서 태그만 바꾸면 됨. 접근성(키보드 탐색) + SEO + 새 탭 열기(Ctrl+클릭)가 한 번에 해결됨.

**대상 위치 (2026-07-24 확인):**
- `HomePage.jsx` — "이번 주 수확물 보기", "전체 보기", "꾸러미 살펴보기", "스물세 농가의 이야기 읽기"
- `ProductListPage.jsx` — 상품 카드 클릭 영역 (이미지·정보 div)
- `ProductDetailPage.jsx` — "쇼핑으로 돌아가기", 추천 상품 카드
- `StoryPage.jsx`, `CartPage.jsx`, `OrderDetailPage.jsx`, `Footer.jsx` — 각 "쇼핑하기"류 버튼

**주의:** 상품 카드처럼 안에 "장바구니 담기" 버튼이 같이 있는 경우, 카드 전체를 `<Link>`로 감싸면 `<button>`이 `<a>` 안에 중첩되어 HTML 스펙 위반이 된다. 이런 곳은 카드를 감싸는 `<Link>`와 별개로 장바구니 버튼에 `e.preventDefault()` + `e.stopPropagation()`을 쓰거나, 카드 자체는 `<article>`로 두고 안의 텍스트/이미지 부분만 `<Link>`로 감싸는 절충이 필요하다.

### 8. 모바일 햄버거 메뉴 — ✅ 해결됨

`useState`로 열림/닫힘 토글 + CSS 미디어쿼리(`@media (max-width: 768px)`)로 데스크톱 메뉴 숨기고 햄버거 버튼 표시. `Navbar.jsx`에 이미 구현되어 있다.

---

## 작업 순서 요약 (2026-07-24 갱신 — 1~6, 8은 완료, 7만 남음)

| 순서 | 작업 | 상태 |
|---|---|---|
| ~~1~~ | ~~필터 버그 수정~~ | ✅ 완료 |
| ~~2~~ | ~~상품 데이터 단일 소스 통합~~ | ✅ 완료 |
| ~~3~~ | ~~상세 페이지 라우팅~~ | ✅ 완료 |
| ~~4~~ | ~~로그인 리다이렉트 흐름~~ | ✅ 완료 |
| ~~5~~ | ~~이미지 교체~~ | ✅ 완료 |
| ~~6~~ | ~~404 페이지~~ | ✅ 완료 |
| **7** | **네비게이션 시맨틱 교체 (div/span onClick → Link)** | 🔧 남음 |
| ~~8~~ | ~~모바일 햄버거~~ | ✅ 완료 |

---

## 다음 단계

프로젝트는 Spring Boot 백엔드(`/api/products`) 기반으로 확정됨 — 이 문서 작성 시점의 "목데이터 기반인지 확인 필요"는 해소됨. 남은 유일한 작업은 위 7번(네비게이션 시맨틱 교체)이며, 카드형 컴포넌트에서 `<Link>`와 내부 버튼(`<button>`)의 중첩 문제를 어떻게 풀지가 핵심 설계 포인트다.
