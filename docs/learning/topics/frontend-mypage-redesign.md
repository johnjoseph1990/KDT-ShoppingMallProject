# 프론트엔드 마이페이지 리디자인

> 작성 배경: MyPage.jsx 전면 리디자인 작업 (2026-07-22)

---

## 전체 흐름도

```
사용자가 /mypage 접속
   ↓
PrivateRoute → 로그인 안 됐으면 /login으로 이동
   ↓
MyPage 컴포넌트 렌더링
   ├── [사이드바] 이니셜 아바타 + 이름/이메일 + 탭 내비
   │
   ├── [주문 내역 탭 - 기본]
   │     ↓
   │   getOrders() API 호출 → 백엔드 GET /api/orders
   │     ↓
   │   상태별 카운트(결제완료/배송중/배송완료) + 주문 목록 렌더링
   │
   ├── [계정 설정 탭]
   │     ↓
   │   이름 수정 행 클릭 → 폼 펼침(disclosure)
   │     ↓
   │   updateMe() → PUT /api/members/me → AuthContext user 갱신
   │
   └── [회원 탈퇴]
         ↓
       사이드바 최하단 링크 클릭 → DeleteModal 표시
         ↓
       "탈퇴하겠습니다" 버튼 → deleteMe() → DELETE /api/members/me
         ↓
       logout() → navigate('/')
```

---

## 핵심 개념

### 1. Context API — 전역 상태 관리

**초보자 설명**
"여러 컴포넌트가 같은 데이터를 공유해야 할 때, 일일이 props로 전달하지 않아도 되게 해주는 공용 창고"

**React 어느 부분**
React 내장 기능. `createContext` → `Provider` → `useContext` 세 단계.
이 프로젝트에서는 `AuthContext`(로그인 상태)와 `CartContext`(장바구니)에 사용.

**실무 활용**
로그인 정보, 테마, 언어 설정처럼 앱 전체에서 쓰이는 데이터에 사용.
규모가 커지면 Redux, Zustand 같은 외부 라이브러리로 교체.

```jsx
// AuthContext에서 user 정보와 갱신 함수를 꺼내 쓰는 패턴
const { user, login: setUser, logout } = useAuth()

// 이름 변경 성공 후 Context 업데이트 → Navbar의 이름도 자동으로 바뀜
const res = await updateMe({ name: '새이름' })
setUser(res.data)  // Context 갱신 → 구독 중인 모든 컴포넌트 리렌더링
```

---

### 2. Disclosure 패턴 — 접힘/펼침 UI

**초보자 설명**
"클릭하기 전까지는 내용을 숨겨두고, 클릭하면 펼쳐지는 UI 패턴"
HTML의 `<details>/<summary>` 태그와 같은 개념.

**React 어느 부분**
`useState`로 열림/닫힘 상태 관리. 조건부 렌더링(`{open && <Form />}`)으로 구현.

**실무 활용**
FAQ 페이지, 설정 화면, 필터 옵션처럼 "모든 내용을 한 번에 보여주면 복잡한 UI"에서 사용.
폼이 항상 펼쳐져 있으면 페이지가 길어지고 인지 부하가 커짐.

```jsx
// 한 번에 하나만 열리게 — 'name' | 'password' | null
const [openSection, setOpenSection] = useState(null)

const toggle = (section) =>
  setOpenSection((prev) => (prev === section ? null : section))

// 폼은 열렸을 때만 렌더링
{open && <NameForm />}
```

---

### 3. 2컬럼 레이아웃 — CSS Flexbox

**초보자 설명**
"화면을 사이드바(고정 너비)와 콘텐츠(나머지 전부)로 나누는 방법"
신문처럼 왼쪽은 목차, 오른쪽은 본문 형태.

**CSS 어느 부분**
Flexbox의 `flex-shrink: 0`(사이드바 고정)과 `flex: 1`(콘텐츠 가변) 조합.
미디어 쿼리로 모바일에서는 `flex-direction: column`으로 세로 전환.

**실무 활용**
대부분의 어드민 대시보드, 이커머스 마이페이지, 설정 화면에서 사용하는 표준 레이아웃.

```css
/* index.css */
.mypage-layout { display: flex; }
.mypage-sidebar { width: 220px; flex-shrink: 0; }  /* 고정 */
/* 콘텐츠는 style={{ flex: 1 }} 인라인으로 */

@media (max-width: 768px) {
  .mypage-layout { flex-direction: column; }  /* 모바일: 세로 전환 */
}
```

---

### 4. UX 관점 — 정보 우선순위

**초보자 설명**
"사용자가 가장 자주 하는 일을 가장 먼저 보여주는 것"

**실무 활용 (시니어 UX 관점)**

| Before (리디자인 전) | After (리디자인 후) | 이유 |
|---------------------|-------------------|------|
| 이름 변경 폼이 첫 화면 | 주문 내역이 기본 탭 | 주문 조회가 압도적 1위 사용 빈도 |
| 풀와이드 버튼 3개 | 행 클릭 시만 폼 열림 | 인지 부하 감소 |
| 탈퇴 버튼이 화면 중앙 | 사이드바 최하단 작은 링크 | 실수 클릭 방지 |
| 인라인 텍스트 피드백 | 토스트 알림 | 기존 Toast 컴포넌트와 일관성 |

---

## 파일 구조

```
frontend/src/
├── api/
│   └── auth.js          ← updateMe(), deleteMe() 추가
├── components/
│   └── Navbar.jsx       ← /mypage, /orders 링크 분리
├── pages/
│   └── MyPage.jsx       ← 전면 리디자인 (신규)
└── index.css            ← .mypage-layout, .mypage-sidebar 추가
```

---

## 자주 하는 실수

1. **Context 갱신 누락**: 이름 변경 후 `setUser(res.data)` 안 하면 Navbar에 이전 이름이 그대로 남음.
2. **disclosure에서 두 섹션이 동시에 열림**: `toggle` 함수에서 현재와 같은 섹션이면 `null`로 닫아줘야 함.
3. **모달에서 버블링**: 모달 내부 클릭이 오버레이 클릭(`onClose`)으로 전파됨 → `e.stopPropagation()` 필수.
