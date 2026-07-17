# 🎨 프론트엔드 학습 가이드 (이 프로젝트 기준)

> 이 쇼핑몰 프론트엔드(React + Vite + 인라인 스타일)를 만지면서 실제로 부딪힌 HTML·CSS·React
> 개념을, **"개념 → 이 프로젝트 어디에 있나 → 자주 하는 실수"** 순서로 정리한 실전 사전.
> 강의 매핑은 [`CONCEPT_INDEX.md`](./CONCEPT_INDEX.md) §6 (강의 #9 "html", #10 "자바스크립트", React는 커리큘럼 외) 기준.
>
> **사용법:** 코드에서 이해 안 되는 부분이 나오면 → 아래 목차에서 개념을 찾고 → 해당 절의
> "프로젝트 위치"에 적힌 파일을 열어 실제 코드와 대조한다.
>
> 구성: **A. HTML·CSS**(1~9) → **B. React**(10~16)

---

## 목차

### A. HTML · CSS
1. [인라인 스타일 vs CSS 클래스 — 언제 무엇을](#1-인라인-스타일-vs-css-클래스)
2. [CSS 우선순위(specificity)와 `!important`](#2-css-우선순위와-important)
3. [미디어 쿼리와 반응형](#3-미디어-쿼리와-반응형)
4. [Flexbox — 한 줄/한 열로 늘어놓기](#4-flexbox)
5. [Grid — 격자로 배치하기](#5-grid)
6. [position: 요소를 겹쳐 올리기 (배지)](#6-position--relativeabsolute)
7. [CSS 단위: px / % / vh / clamp](#7-css-단위)
8. [시맨틱 HTML과 접근성 (a·button·aria-label)](#8-시맨틱-html과-접근성)
9. [SVG 아이콘과 currentColor](#9-svg-아이콘과-currentcolor)

### B. React
10. [컴포넌트와 props](#10-컴포넌트와-props)
11. [children — 컴포넌트로 감싸기](#11-children)
12. [useState — 바뀌는 값 기억하기](#12-usestate)
13. [useEffect — 화면 뜬 뒤 데이터 불러오기](#13-useeffect)
14. [조건부 렌더링과 리스트 렌더링](#14-조건부-렌더링과-리스트-렌더링)
15. [React Router — 페이지 이동](#15-react-router)
16. [Context — 전역 상태 (로그인·장바구니)](#16-context)

---

# A. HTML · CSS

## 1. 인라인 스타일 vs CSS 클래스

**개념.** React에서 스타일 주는 방법은 크게 두 가지다.
- **인라인 스타일**: `style={{ color: '#333' }}` — 해당 요소 하나에만 바로 적용. 이 프로젝트는 대부분 이 방식.
- **CSS 클래스**: `.my-class { color: #333 }`를 CSS 파일에 쓰고 `className="my-class"`로 연결.

**언제 무엇을?**

| 상황 | 방법 | 이유 |
|------|------|------|
| 요소 하나의 단순 스타일 | 인라인 | 가깝고 직관적 |
| **미디어 쿼리(`@media`)** | **반드시 CSS 클래스** | 인라인 스타일은 `@media`를 **문법적으로 못 씀** |
| `:hover`, `::before` 등 가상 선택자 | CSS 클래스 | 인라인 불가 (이 프로젝트는 hover를 JS 이벤트로 우회) |
| 여러 요소가 공유하는 스타일 | CSS 클래스 | 한 곳만 고치면 전부 반영 |

**프로젝트 위치.**
- 인라인 스타일: 거의 모든 컴포넌트 (`frontend/src/pages/*.jsx`)
- CSS 클래스(미디어 쿼리 전용): `frontend/src/index.css` 하단

**자주 하는 실수.** "반응형 하고 싶은데 인라인 `style`에 `@media`를 넣으려다" 안 됨을 모르는 것.
→ 이 프로젝트는 그래서 반응형이 필요한 부분만 `index.css`로 빼고 `className`을 붙였다. (§3 참조)

---

## 2. CSS 우선순위와 `!important`

**개념.** 같은 요소에 스타일이 여러 군데서 걸리면 "누가 이기는가"가 정해져 있다.
낮은 것 → 높은 것 순서:

```
태그 선택자(p)  <  클래스(.foo)  <  id(#bar)  <  인라인 style  <  !important
```

**핵심 포인트.** 이 프로젝트는 그리드 열 수를 **인라인**으로 박아놨다(`style={{ gridTemplateColumns: ... }}`).
인라인은 클래스보다 세다. 그래서 모바일에서 클래스로 덮어쓰려면 **`!important`가 필요**하다 —
`!important`는 인라인 스타일도 이길 수 있는 유일한 카드이기 때문.

**프로젝트 위치.** `frontend/src/index.css`
```css
@media (max-width: 768px) {
  .mobile-1col { grid-template-columns: 1fr !important; }  /* 인라인 3열을 이겨서 1열로 */
}
```

**자주 하는 실수.** `!important`를 아무 데나 남발하는 것.
→ 한번 `!important`를 쓰면 그걸 또 덮어쓰려면 또 `!important`가 필요해져서 관리가 꼬인다.
여기서는 "인라인을 이겨야 하는 반응형 오버라이드"라는 **명확한 이유가 있을 때만** 썼다.

---

## 3. 미디어 쿼리와 반응형

**개념.** `@media (max-width: 768px) { ... }` = "화면 폭이 768px 이하일 때만 이 규칙을 적용".
데스크톱/태블릿/폰마다 레이아웃을 다르게 하는 반응형의 핵심 도구.

**프로젝트 위치.** `frontend/src/index.css` — 미디어 쿼리는 **이 파일 한 곳에만** 있다.

| 클래스 | 데스크톱 | 모바일(≤768px) | 쓰이는 곳 |
|--------|----------|----------------|-----------|
| `.navbar-desktop-nav` | 보임(flex) | 숨김(none) | `Navbar.jsx` 데스크톱 메뉴 |
| `.navbar-mobile-actions` | 숨김(none) | 보임(flex) | `Navbar.jsx` 모바일 카트+햄버거 |
| `.mobile-1col` | 인라인값 | 1열 강제 | 상세·장바구니·푸터·브랜드·스토리·홈배너 |
| `.mobile-2col` | 인라인값 | 2열 강제 | 상품목록·홈 추천상품 |
| `.hero-sticky-image` | 인라인값 | 고정해제+높이축소 | 상품상세·농부이야기 큰 이미지 |

**설계 원칙.** breakpoint 값(`768px`)과 반응형 규칙을 **한 파일에 모았다**.
흩어놓으면 나중에 기준을 바꿀 때 여러 파일을 다 고쳐야 하지만, 모아두면 `index.css` 한 곳만 보면 된다.

**짝 맞추기 패턴.** 데스크톱용(`.navbar-desktop-nav`)과 모바일용(`.navbar-mobile-actions`)이
**정확히 반대로** 동작하게 짝지었다 — 하나가 보이면 다른 하나는 숨어서, 둘이 겹쳐 보이는 일이 없다.

**자주 하는 실수.** 요소를 조건부로 안 그리려고 JS에서 `window.innerWidth`로 분기하는 것.
→ 창 크기 바뀔 때마다 다시 그려야 하고 깜빡임이 생긴다. CSS `@media`는 브라우저가 알아서 즉시 처리해준다.

---

## 4. Flexbox

**개념.** `display: flex`를 준 요소의 **자식들을 한 줄(또는 한 열)로 늘어놓는** 레이아웃.
- `flexDirection: 'row'`(기본, 가로) / `'column'`(세로)
- `justifyContent`: 주축(늘어선 방향) 정렬 — `space-between`(양 끝), `center` 등
- `alignItems`: 교차축(수직) 정렬 — `center`(가운데) 등
- `gap`: 자식들 사이 간격

**프로젝트 위치.** `frontend/src/components/Navbar.jsx` 헤더
```jsx
<header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
  {/* 로고(왼쪽) ...... 메뉴(오른쪽) — space-between이 양 끝으로 밀어냄 */}
```
카트 아이콘 버튼 내부 정렬(`display:flex; alignItems:center; justifyContent:center`)도 같은 원리.

**자주 하는 실수.** `justifyContent`(주축)와 `alignItems`(교차축)를 헷갈리는 것.
→ `flexDirection`이 `row`면 justify=가로, align=세로. `column`이면 반대가 된다.

---

## 5. Grid

**개념.** `display: grid` + `gridTemplateColumns`로 **격자(행·열)**를 만든다.
- `gridTemplateColumns: '1fr 1fr'` → 2열, 폭을 1:1로 나눔 (`fr` = 남은 공간의 비율 단위)
- `repeat(3, 1fr)` → `1fr 1fr 1fr`의 축약 = 3열
- `'1.1fr 1fr'` → 첫 열을 조금 더 넓게 (이미지 1.1 : 정보 1)

**프로젝트 위치.**
- 상품 목록 그리드: `frontend/src/pages/ProductListPage.jsx` (`repeat(3,1fr)` → 모바일 `mobile-2col`)
- 상품 상세 2단: `frontend/src/pages/ProductDetailPage.jsx` (`1.1fr 1fr` → 모바일 `mobile-1col`)

**Flexbox와의 차이.** Flex는 "한 줄로 늘어놓기"(1차원), Grid는 "행·열 격자"(2차원).
상품 카드처럼 여러 줄에 걸쳐 규칙적으로 배치할 땐 Grid가 편하다.

**자주 하는 실수.** 모바일에서 열 수를 안 줄이는 것.
→ 데스크톱 3열을 폰에서 그대로 두면 칸이 좁아 글자가 세로로 쪼개진다. `mobile-2col`/`mobile-1col`로 접었다. (§3)

---

## 6. position — relative/absolute

**개념.** 요소를 원래 흐름에서 떼어내 **다른 요소 위에 겹쳐 올릴** 때 쓴다.
- `position: relative` — 자기 자신은 그대로 있지만, **자식 absolute의 위치 기준점**이 된다.
- `position: absolute` — "가장 가까운 relative 조상"을 기준으로 `top/right/left/bottom`만큼 이동.

**프로젝트 위치.** `frontend/src/components/Navbar.jsx`의 `CartIconBtn` — 카트 아이콘 위 개수 배지
```jsx
<button style={{ position: 'relative' }}>   {/* 기준점 */}
  <svg>...카트 아이콘...</svg>
  <span style={{ position: 'absolute', top: -2, right: -4 }}>{count}</span>  {/* 우측 위 모서리 */}
</button>
```

**자주 하는 실수.** 부모에 `relative`를 안 주는 것.
→ 그러면 배지가 페이지 전체(또는 엉뚱한 조상)를 기준으로 떠서 이상한 위치에 나타난다.
"absolute 자식을 쓸 거면 부모에 relative"가 세트다.

---

## 7. CSS 단위

| 단위 | 뜻 | 이 프로젝트 예 |
|------|-----|----------------|
| `px` | 고정 픽셀 | `fontSize: 14`, `padding: 12` (대부분) |
| `%` | 부모 기준 비율 | 이미지 `width: '100%'` |
| `vh` | 화면 높이의 1% (`100vh` = 화면 전체 높이) | 상세 이미지 `height: 'calc(100vh - 72px)'`, 모바일 `44vh` |
| `clamp(a, b, c)` | 최소 a, 기본 b, 최대 c 사이에서 자동 조절 | `padding: 'clamp(20px,5vw,72px)'` |

**`clamp`가 왜 좋은가.** `clamp(20px, 5vw, 72px)` = "화면 폭의 5%로 하되, 20px보다 작지도 72px보다 크지도 않게".
미디어 쿼리 없이도 여백이 화면 크기에 따라 부드럽게 늘었다 줄었다 한다. 이 프로젝트가 여백에 즐겨 쓴다.

**자주 하는 실수.** 모든 걸 `px` 고정으로만 짜서, 큰 화면에선 답답하고 작은 화면에선 넘치는 것.
→ 여백/폰트는 `clamp`, 높이는 `vh`, 폭은 `%`를 섞으면 화면 크기에 유연해진다.

---

## 8. 시맨틱 HTML과 접근성

**개념.** 같은 모양이라도 **의미에 맞는 태그**를 써야 한다. 겉모습(`onClick` 달린 `div`)이 아니라 역할로 고른다.

| 하는 일 | 올바른 태그 | 이 프로젝트 |
|---------|-------------|-------------|
| 다른 페이지로 **이동** | `<a>` (React Router `<Link>`/`<NavLink>`) | 네비 메뉴, 상품 카드 링크 |
| **동작 실행**(열기/제출/로그아웃) | `<button>` | 장바구니 열기, 로그아웃, 담기 |
| ❌ 이동인데 `<span onClick>` | 잘못 | (과거 코드 — `Link`로 교체함) |

**왜 중요한가.** `<a>`로 만들면 공짜로 따라오는 것들:
- 마우스 우클릭 → "새 탭에서 열기"
- 키보드 Tab 이동·Enter 작동
- 스크린리더가 "링크"로 인식
`<div onClick>`은 이게 전부 안 된다(마우스 클릭만 됨).

**접근성: `aria-label`.** 아이콘만 있고 글자가 없는 버튼은 스크린리더에 "버튼"으로만 읽힌다.
→ `aria-label="장바구니에 담기"`를 붙여 스크린리더가 읽을 텍스트를 따로 준다.

**프로젝트 위치.**
- `frontend/src/components/Navbar.jsx` — `NavItem`(`NavLink` 기반), `CartIconBtn`(`aria-label`)
- `frontend/src/pages/ProductListPage.jsx` — 카트 아이콘 버튼 `aria-label="장바구니에 담기"`

**자주 하는 실수.** 아이콘 버튼에 `aria-label`을 빠뜨리는 것 — 웹 접근성에서 가장 흔한 실수 1위.

**활성 표시(현재 페이지).** `<NavLink>`는 현재 URL과 자기 `to`가 같으면 자동으로 알려준다:
```jsx
<NavLink style={({ isActive }) => ({ borderBottom: isActive ? '1px solid #333' : '...' })}>
```
`isActive`를 라우터가 판정해주므로 "지금 어느 페이지지?"를 직접 계산할 필요가 없다.
(활성 링크엔 `aria-current="page"`도 자동으로 붙어 접근성까지 챙겨진다.)

---

## 9. SVG 아이콘과 currentColor

**개념.** 아이콘은 이모지(🔍) 대신 **인라인 SVG**를 쓰면 색·두께를 CSS로 제어할 수 있다.
- 이모지: OS/브라우저마다 모양이 다르고 색을 못 바꿈
- SVG: `stroke`(선 색), `strokeWidth`(두께)를 자유롭게 지정 → 사이트 톤과 통일

**`stroke="currentColor"`의 마법.** SVG의 색을 `currentColor`로 두면 **부모 요소의 `color` 값을 그대로 따라간다.**
그래서 버튼 색이 hover로 바뀌면 아이콘 색도 같이 바뀐다 — 색을 두 번 관리할 필요가 없다.

**프로젝트 위치.** `frontend/src/components/Navbar.jsx`
- `SearchIconBtn` — 돋보기 아이콘 (circle + line)
- `CartIconBtn` — 카트 아이콘 (circle×2 + path)
- `frontend/src/pages/ProductListPage.jsx` — 상품 카드 카트 아이콘

```jsx
<button style={{ color: '#333330' }}>
  <svg stroke="currentColor">...</svg>  {/* 버튼 color(#333330)를 따라감 */}
</button>
```

**자주 하는 실수.** SVG에 색을 하드코딩(`stroke="#333330"`)해서, hover 시 버튼만 색이 바뀌고 아이콘은 그대로인 것.
→ `currentColor`로 두면 자동으로 함께 바뀐다.

---

# B. React

> React는 "화면을 **컴포넌트(function)** 로 쪼개고, **state(상태)** 가 바뀌면 화면을 자동으로 다시 그리는" 라이브러리다.
> 아래는 이 프로젝트에서 실제로 쓴 것들만.

## 10. 컴포넌트와 props

**개념.** 컴포넌트 = **화면 한 조각을 반환하는 함수**. 대문자로 시작하고, JSX(HTML처럼 생긴 문법)를 반환한다.
**props** = 부모가 자식 컴포넌트에 넘겨주는 **입력값**(함수의 매개변수와 같은 개념).

```jsx
// 정의: props로 to, children을 받는다
function NavItem({ to, onClick, children }) {
  return <NavLink to={to} onClick={onClick}>{children}</NavLink>
}

// 사용: to="/shop" 이 props로 전달됨
<NavItem to="/shop">쇼핑</NavItem>
```

**프로젝트 위치.** `frontend/src/components/Navbar.jsx` — `NavItem`, `OutlineBtn`, `CartIconBtn`, `SearchIconBtn`
`frontend/src/pages/ProductListPage.jsx` — `ProductCard`, `CatBtn`, `PageBtn`

**변형(variant) 패턴 — 한 컴포넌트를 여러 모양으로.** `NavItem`은 `variant` prop으로
데스크톱은 밑줄, 모바일은 왼쪽 세로 막대를 그린다. **로직(현재 페이지 판별)은 공유하고 겉모습만 분기**한 예:
```jsx
function NavItem({ to, variant = 'underline', children }) {
  // variant === 'bar' 면 세로 막대, 아니면 밑줄 (Navbar.jsx 참고)
}
```

**자주 하는 실수.** props를 자식이 직접 바꾸려는 것. props는 **읽기 전용**이다 (부모가 준 값).
바꿔야 하는 값은 state(§12)로 관리한다.

---

## 11. children

**개념.** 여는 태그와 닫는 태그 **사이에 넣은 내용**이 `children` prop으로 전달된다.
컴포넌트로 다른 내용을 "감싸는" 패턴.

```jsx
function OutlineBtn({ onClick, children }) {
  return <button onClick={onClick}>{children}</button>
}

<OutlineBtn onClick={handleLogout}>로그아웃</OutlineBtn>
//                                  ↑ 이 "로그아웃"이 children
```

**프로젝트 위치.** `frontend/src/components/Navbar.jsx` — `OutlineBtn`, `NavItem`이 `children`으로
버튼/링크 안의 글자나 아이콘을 받는다.

**자주 하는 실수.** 감싸는 컴포넌트에서 `{children}`을 안 써서, 안에 넣은 내용이 화면에 안 나오는 것.

---

## 12. useState

**개념.** `useState`는 **"화면에서 바뀌는 값"을 기억**하는 도구(훅). 값이 바뀌면 React가 화면을 다시 그린다.

```jsx
const [menuOpen, setMenuOpen] = useState(false)
//     ↑현재값     ↑바꾸는함수        ↑초깃값

setMenuOpen(true)              // 값 교체
setMenuOpen((open) => !open)   // 이전 값 기반으로 토글
```

**프로젝트 위치.**
- `frontend/src/components/Navbar.jsx` — `menuOpen`(모바일 메뉴 열림 여부)
- `frontend/src/pages/ProductListPage.jsx` — `ProductCard`의 `hovered`(마우스 올림 여부)
- `frontend/src/pages/LoginPage.jsx` — `form`(입력값), `error`(에러 메시지)

**자주 하는 실수.** `menuOpen = true`처럼 값을 직접 바꾸는 것.
→ 반드시 `setMenuOpen(...)`로 바꿔야 React가 "아, 바뀌었네" 하고 화면을 다시 그린다. 직접 대입하면 화면이 안 변한다.

---

## 13. useEffect

**개념.** `useEffect`는 **"화면이 뜬 뒤(또는 특정 값이 바뀐 뒤)에 할 일"**을 적는 곳.
서버에서 데이터를 불러오는 작업이 대표적.

```jsx
useEffect(() => {
  getProducts().then((res) => setProducts(res.data.content))
}, [])   // ← 의존성 배열: []면 "처음 한 번만" 실행
```

의존성 배열에 값을 넣으면(`[cat, page]`) 그 값이 바뀔 때마다 다시 실행된다.

**프로젝트 위치.**
- `frontend/src/pages/ProductListPage.jsx` — `[cat, page]`가 바뀌면 상품을 다시 불러옴
- `frontend/src/pages/ProductDetailPage.jsx` — `[id]`가 바뀌면 해당 상품/리뷰 로드
- `frontend/src/pages/OrderListPage.jsx` — 처음 한 번 주문 목록 로드

**자주 하는 실수.** 의존성 배열을 아예 빼먹는 것 → 렌더링될 때마다 매번 실행되어 무한 호출에 빠질 수 있다.
"처음 한 번만"이면 `[]`, "이 값 바뀔 때마다"면 `[값]`을 반드시 명시한다.

---

## 14. 조건부 렌더링과 리스트 렌더링

**조건부 렌더링.** `조건 && (JSX)` — 조건이 참일 때만 그린다.
```jsx
{user && <span>{user.name}님</span>}          {/* 로그인했을 때만 이름 */}
{count > 0 && <span className="badge">{count}</span>}  {/* 담긴 게 있을 때만 배지 */}
{menuOpen && <div>...모바일 메뉴...</div>}     {/* 메뉴 열렸을 때만 */}
```

**리스트 렌더링.** 배열을 `.map()`으로 돌려 여러 요소를 그린다. 각 요소엔 **`key`(고유값)** 필수.
```jsx
{products.map((p) => (
  <ProductCard key={p.id} product={p} />   /* key=p.id 로 각 항목 구분 */
))}
```

**프로젝트 위치.**
- 조건부: `frontend/src/components/Navbar.jsx`(user/menuOpen/count), `LoginPage.jsx`(error, from)
- 리스트: `ProductListPage.jsx`(상품 카드), `Navbar.jsx`(`navLinks.map`), `OrderListPage.jsx`(주문 행)

**자주 하는 실수.**
- `key`를 빠뜨리거나 `key={index}`로 대충 주는 것 → 목록이 바뀔 때 React가 항목을 헷갈려 버그가 생긴다. `id` 같은 **고유값**을 쓴다.
- `조건 && ...`에서 조건이 숫자 `0`이면 화면에 `0`이 그대로 찍힌다 → `count > 0 &&`처럼 **불리언으로** 만든다.

---

## 15. React Router

**개념.** 여러 "페이지"를 URL에 따라 갈아끼우는 라이브러리(새로고침 없는 화면 전환 = SPA).

| 도구 | 하는 일 | 이 프로젝트 |
|------|---------|-------------|
| `<Routes>`/`<Route>` | URL ↔ 화면 매핑 | `App.jsx` (`path="*"` = 404) |
| `<Link>` | 클릭 이동(단순) | 브랜드 로고, 상품 카드 |
| `<NavLink>` | 이동 + 현재 페이지 표시(`isActive`) | 네비 메뉴 (§8 활성 표시) |
| `useNavigate()` | 코드로 이동(`navigate('/login')`) | 로그인/로그아웃 후 이동 |
| `useLocation()` | 현재 위치 정보 | 로그인 후 원래 페이지 복귀 |
| `useParams()` | URL의 `:id` 값 꺼내기 | 상품 상세 `/products/:id` |

**로그인 후 원래 자리로 — `state`로 위치 전달.**
```jsx
// 보낼 때: 지금 위치를 몰래 들려 보냄
navigate('/login', { state: { from: location } })
// 로그인 페이지에서: 돌아갈 위치를 꺼냄
const from = location.state?.from
navigate(from || '/', { replace: true })
```
URL에 안 드러나는 "숨겨진 짐"으로 위치를 넘기는 방식.

**프로젝트 위치.** `frontend/src/App.jsx`(라우트 정의), `frontend/src/components/Navbar.jsx`(Link/NavLink),
`frontend/src/pages/LoginPage.jsx`·`components/PrivateRoute.jsx`(로그인 복귀 흐름)

**보호된 페이지(로그인 필요).** `PrivateRoute`가 로그인 안 했으면 `/login`으로 돌려보낸다:
```jsx
if (!user) return <Navigate to="/login" replace state={{ from: location }} />
```

**자주 하는 실수.** 페이지 이동에 `<a href>`(진짜 새로고침)를 써서 SPA 장점을 날리는 것.
→ 앱 내부 이동은 `<Link>`/`navigate`, 외부 사이트만 `<a href>`.

---

## 16. Context

**개념.** props를 계층마다 손으로 넘기지 않고, **여러 컴포넌트가 공유하는 전역 값**을 두는 도구.
"로그인한 사용자"나 "장바구니"처럼 앱 곳곳에서 필요한 값에 쓴다.

```jsx
const { user, logout } = useAuth()          // 로그인 정보 어디서든 꺼내기
const { cartCount, openCart } = useCart()   // 장바구니 정보/동작
```

**프로젝트 위치.**
- `frontend/src/context/AuthContext.jsx` — `useAuth()` (로그인 사용자, login/logout)
- `frontend/src/context/CartContext.jsx` — `useCart()` (장바구니 개수, 열기, showToast)
- `frontend/src/App.jsx` — `<AuthProvider><CartProvider>`로 앱 전체를 감싸 공급

**자주 하는 실수.** 모든 값을 Context에 넣으려는 것 → 진짜 "여러 곳에서 공유하는" 값(로그인·장바구니)만.
한 컴포넌트 안에서만 쓰는 값은 그냥 `useState`(§12)로 충분하다.

---

## 부록 — 이 가이드가 나온 실제 작업

이 문서의 예시는 전부 실제 커밋에서 나왔다. 코드로 더 파고들 때 참고:

| 주제 | 관련 커밋(키워드) |
|------|-------------------|
| 시맨틱 교체 (span/div → Link/button) | `refactor: Navbar/OrderListPage 네비게이션을 span·div에서 Link·button으로` |
| 현재 페이지 활성 표시 | `feat: 네비게이션 현재 페이지에 활성 표시(밑줄)` |
| 모바일 반응형 그리드 | `fix: 모바일에서 다단 그리드가 안 접혀 화면이 잘리던 문제 수정` |
| SVG 아이콘 | `style: 검색 아이콘을 이모지에서 인라인 SVG로 교체` |
| 카트 아이콘 + 배지 | `style: 네비게이션 장바구니 버튼을 카트 아이콘 + 개수 배지로 변경` |
| 로그인 후 복귀 (Router state) | `feat: 로그인 후 원래 페이지로 돌아오는 리다이렉트 흐름 추가` |
| 컴포넌트 variant 패턴 | `style: 모바일 메뉴 활성 표시를 밑줄에서 왼쪽 세로 막대로 변경` |

> - HTML·CSS 이론: 강의 #9 "html 강의"의 CSS Flexbox/Grid, Media Query 섹션
> - JS(map/filter, async, 구조분해): 강의 #10 "자바스크립트 강의" ([`CONCEPT_INDEX.md`](./CONCEPT_INDEX.md) §6)
> - **React**: 커리큘럼 외 → 공식 문서 [react.dev](https://react.dev) + 위 §10~16을 실전 진입점으로.
