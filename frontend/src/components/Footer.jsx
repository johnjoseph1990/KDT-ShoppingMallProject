import { useNavigate } from 'react-router-dom'

/* 모든 페이지 하단에 표시되는 공통 푸터 */
export default function Footer() {
  const navigate = useNavigate()

  return (
    <footer
      style={{
        background: '#333326',
        color: '#c9c7b2',
        padding: 'clamp(40px,6vw,72px) clamp(20px,5vw,72px)',
        marginTop: 'auto',
      }}
    >
      <div
        className="mobile-1col"
        style={{
          display: 'grid',
          gridTemplateColumns: '1.4fr 1fr 1fr',
          gap: 'clamp(28px,5vw,72px)',
        }}
      >
        {/* 뉴스레터 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <p style={{ margin: 0, fontSize: 14, color: '#e5e3d3', fontWeight: 400 }}>
            계절 소식 받기
          </p>
          <p style={{ margin: 0, fontSize: 13, fontWeight: 300, lineHeight: 1.8 }}>
            매주 한 통, 밭의 소식과 이번 주 수확물을 알려드립니다.
          </p>
          <div
            style={{
              display: 'flex',
              borderBottom: '1px solid #6d6c61',
              maxWidth: 360,
            }}
          >
            <input
              placeholder="이메일 주소"
              style={{
                flex: 1,
                border: 'none',
                background: 'transparent',
                color: '#e5e3d3',
                padding: '12px 0',
                fontSize: 14,
                outline: 'none',
              }}
            />
            <button
              style={{
                cursor: 'pointer',
                border: 'none',
                background: 'transparent',
                color: '#e5e3d3',
                fontSize: 14,
              }}
            >
              →
            </button>
          </div>
        </div>

        {/* 네비게이션 */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 12,
            fontSize: 13,
            fontWeight: 300,
          }}
        >
          <p style={{ margin: '0 0 6px', color: '#e5e3d3', fontWeight: 400 }}>둘러보기</p>
          {[
            { label: '쇼핑', to: '/shop' },
            { label: '생산자', to: '/story' },
            { label: '브랜드', to: '/about' },
          ].map(({ label, to }) => (
            <FooterLink key={to} onClick={() => navigate(to)}>
              {label}
            </FooterLink>
          ))}
        </div>

        {/* 안내 */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 12,
            fontSize: 13,
            fontWeight: 300,
          }}
        >
          <p style={{ margin: '0 0 6px', color: '#e5e3d3', fontWeight: 400 }}>안내</p>
          <p style={{ margin: 0 }}>화·금 수확 및 발송</p>
          <p style={{ margin: 0 }}>4만 원 이상 무료 배송</p>
          <p style={{ margin: 0 }}>대전 유성구 어은로 52</p>
        </div>
      </div>
      <p style={{ margin: '48px 0 0', fontSize: 12, color: '#8a8973', fontWeight: 300 }}>
        © 2026 MINS Farmers Market — 대전충남 로컬푸드 직거래
      </p>
    </footer>
  )
}

function FooterLink({ onClick, children }) {
  return (
    <span
      onClick={onClick}
      style={{ cursor: 'pointer', color: '#c9c7b2' }}
      onMouseEnter={(e) => (e.currentTarget.style.color = '#fffef2')}
      onMouseLeave={(e) => (e.currentTarget.style.color = '#c9c7b2')}
    >
      {children}
    </span>
  )
}
