# 리팩토링: ProductCard + FeaturedCard 통합

**날짜:** 2026-07-27  
**분류:** 코드 품질 개선 (기능 변경 없음)

---

## 왜 했나

`ProductCard`(상품 목록)와 `FeaturedCard`(홈 베스트)는 구조가 동일한데 각 파일 안에 따로 정의돼 있었다.  
가격 포맷 함수 `fmt`도 세 파일에 복사-붙여넣기 상태였다.  
카드 디자인이 바뀌면 두 곳을 동시에 고쳐야 하는 구조여서 유지보수 부담이 있었다.

---

## 무엇을 바꿨나

### 고도화인가?

**아니다.** 이번 작업은 **리팩토링(내부 품질 개선)** 이다.

| 구분 | 이번 작업 | 고도화 |
|---|---|---|
| 사용자 입장 | 화면이 전혀 달라지지 않음 | 새 기능 / 더 나은 UX |
| 코드 입장 | 중복 제거, 구조 정리 | 새로운 로직 추가 |
| 목적 | 유지보수성 향상 | 기능 확장 |

리팩토링은 고도화를 **가능하게 해주는 기반 정리**다.  
코드가 깔끔해야 그 위에 기능을 쌓기 쉬워진다.

---

## 변경 파일

| 파일 | 작업 |
|---|---|
| `frontend/src/utils/product.js` | **신규 생성** — `fmt`, `stockBadge` 함수 통합 |
| `frontend/src/components/ProductCard.jsx` | **신규 생성** — 통합 카드 컴포넌트 |
| `frontend/src/pages/ProductListPage.jsx` | 로컬 `fmt`, `stockBadge`, `ProductCard` 정의 제거 → import로 교체 |
| `frontend/src/pages/HomePage.jsx` | 로컬 `fmt`, `FeaturedCard` 정의 제거 → `<ProductCard featured />` 로 교체 |
| `frontend/src/pages/ProductDetailPage.jsx` | 로컬 `fmt` 제거 → import로 교체 |

---

## 핵심 설계: `featured` prop

두 카드의 차이점을 `featured` boolean prop 하나로 제어한다.

```jsx
// 상품 목록 (기본 동작)
<ProductCard product={p} onOpen={...} onAdd={...} />

// 홈 베스트 섹션
<ProductCard product={p} onOpen={...} onAdd={...} featured />
```

`featured` 값에 따라 달라지는 동작:

| 항목 | featured=false (목록) | featured=true (홈) |
|---|---|---|
| 설명 글자 수 | 40자 | 30자 |
| 재고 배지 | 있음 | 없음 |
| 품절 이미지 처리 | grayscale + opacity | 없음 |
| 별점 행 | 있음 | 없음 |
| 장바구니 버튼 | 있음 (품절 시 disabled) | 없음 |
| onClick 위치 | 이미지/텍스트 영역만 | 카드 전체 div |
| h3 font-size | 17px | 16px |
| 정보 div flex | flex: 1 | 없음 |

---

## 배운 것

### 1. DRY 원칙 (Don't Repeat Yourself)

같은 함수를 여러 파일에 복사하면 나중에 규칙이 바뀔 때 모든 파일을 동시에 수정해야 한다.  
`utils/product.js` 한 곳에 정의하고 import하면 한 번만 수정하면 된다.

```js
// utils/product.js
export function fmt(n) { return n.toLocaleString('ko-KR') + '원' }
export function stockBadge(qty) { ... }
```

### 2. Boolean prop 기본값 패턴

```jsx
// 컴포넌트 선언부 — featured를 생략하면 false가 기본값
function ProductCard({ product, onOpen, onAdd, featured = false }) { ... }

// JSX에서 boolean prop은 값 없이 이름만 써도 true
<ProductCard featured />        // featured={true}와 동일
<ProductCard featured={true} /> // 위와 같음
<ProductCard />                 // featured={false} (기본값)
```

### 3. Rules of Hooks — 조건부 호출 금지

```jsx
// 잘못된 예 (React 규칙 위반 — 렌더마다 훅 수가 달라지면 안 됨)
if (!featured) {
  const [btnHovered, setBtnHovered] = useState(false) // ❌
}

// 올바른 예 — 훅은 항상 선언, 사용만 조건 분기
const [hovered, setHovered] = useState(false) // ✅ 항상 선언
const soldOut = !featured && product.stockQuantity === 0 // 계산만 분기
```

### 4. 리팩토링 vs 고도화 구별

- **리팩토링:** 외부 동작은 같고 내부 코드 구조를 개선. 사용자는 변화를 모른다.
- **고도화:** 새 기능 추가, UX 개선 등 사용자가 체감하는 변화.
- 리팩토링은 고도화를 위한 **기반 작업**이다.
