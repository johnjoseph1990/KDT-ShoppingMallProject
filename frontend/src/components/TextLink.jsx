import { Link } from 'react-router-dom'

// 글자 형태의 내부 링크를 만드는 공통 컴포넌트.
//
// 왜 필요한가 (2026-08-02 DEF-7 / P2-7):
// 이 프로젝트 곳곳에 `<span onClick={() => navigate('/shop')}>`처럼 span에 클릭을 단
// "가짜 링크"가 있었다. 화면상으론 링크처럼 보이지만 브라우저에게는 그냥 글자라서
//   - Tab 키로 도달할 수 없다 (키보드만 쓰는 사용자는 아예 사용 불가)
//   - Enter 키가 먹지 않는다
//   - 우클릭 "새 탭에서 열기" / Ctrl+클릭이 동작하지 않는다
//   - 스크린리더가 "링크"라고 읽어주지 않아 존재 자체를 모른다
// 이 문제들은 <a>(react-router의 <Link>)를 쓰면 전부 공짜로 해결된다.
//
// 왜 <Link>를 직접 쓰지 않고 한 겹 감쌌나:
// <a>는 브라우저 기본 스타일(파란 글씨 + 밑줄)이 붙어서, 교체할 때마다
// color/textDecoration 초기화를 매번 손으로 적어야 한다. 그 "초기화 규칙"을
// 한 곳에 모아두면 나중에 링크 스타일 기준이 바뀔 때 여기만 고치면 된다.
//
// props:
//   to       — 이동할 경로 (react-router 경로)
//   style    — 추가 스타일. 기본 초기화 뒤에 펼쳐지므로 필요하면 덮어쓸 수 있다
//   ...rest  — onMouseEnter 등 나머지 속성은 그대로 <Link>에 전달
export default function TextLink({ to, style, children, ...rest }) {
  return (
    <Link
      to={to}
      style={{
        // color: 'inherit' — 부모 글자색을 그대로 따라간다.
        // 기존 span들이 부모 색을 쓰고 있었으므로 이게 시각적으로 동일한 결과다.
        color: 'inherit',
        textDecoration: 'none',
        cursor: 'pointer',
        ...style,
      }}
      {...rest}
    >
      {children}
    </Link>
  )
}
