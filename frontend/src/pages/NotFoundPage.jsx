import { Link } from 'react-router-dom'

/* 존재하지 않는 경로로 접근했을 때 보여주는 404 페이지.
   App.jsx의 <Routes> 맨 마지막 <Route path="*" .../>가 이 컴포넌트로 연결된다. */
export default function NotFoundPage() {
  return (
    <main
      style={{
        animation: 'fadeUp .4s ease both',
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'clamp(48px,8vw,110px) 20px',
      }}
    >
      <div style={{ textAlign: 'center', maxWidth: 400 }}>
        <p style={{ margin: '0 0 12px', fontSize: 12, letterSpacing: '0.14em', color: '#75775e' }}>
          404
        </p>
        <h1
          style={{
            margin: '0 0 16px',
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 300,
            fontSize: 'clamp(28px,3vw,36px)',
            lineHeight: 1.4,
          }}
        >
          페이지를 찾을 수 없습니다
        </h1>
        <p
          style={{
            margin: '0 0 32px',
            fontSize: 14,
            color: '#6d6c61',
            fontWeight: 300,
            lineHeight: 1.8,
          }}
        >
          주소가 잘못되었거나 삭제된 페이지일 수 있어요.
        </p>
        <div style={{ display: 'flex', gap: 24, justifyContent: 'center', fontSize: 13 }}>
          <Link
            to="/"
            style={{ color: '#333330', borderBottom: '1px solid #333330', paddingBottom: 1 }}
          >
            홈으로
          </Link>
          <Link
            to="/shop"
            style={{ color: '#333330', borderBottom: '1px solid #333330', paddingBottom: 1 }}
          >
            쇼핑하기
          </Link>
        </div>
      </div>
    </main>
  )
}
