import TextLink from './TextLink'

/* 모든 페이지 하단에 표시되는 공통 푸터 */
export default function Footer() {
  // useNavigate를 쓰지 않는다 — "둘러보기" 항목은 이동이므로 진짜 <a>(TextLink)여야 한다.
  // 예전엔 span+onClick+navigate였는데 키보드로 도달할 수 없었다 (DEF-7).
  return (
    <footer
      style={{
        background: 'var(--color-bg-dark)',
        color: 'var(--color-text-footer)',
        padding: 'clamp(40px,6vw,72px) clamp(20px,5vw,72px)',
        marginTop: 'auto',
      }}
    >
      {/* mobile-1col: 뉴스레터+메뉴 3단 → 모바일에서 1단으로 접힘 (index.css) */}
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
          <p
            style={{ margin: 0, fontSize: 14, color: 'var(--color-text-on-dark)', fontWeight: 400 }}
          >
            계절 소식 받기
          </p>
          <p style={{ margin: 0, fontSize: 13, fontWeight: 300, lineHeight: 1.8 }}>
            매주 한 통, 밭의 소식과 이번 주 수확물을 알려드립니다.
          </p>
          <div
            style={{
              display: 'flex',
              borderBottom: '1px solid var(--color-fg-muted)',
              maxWidth: 360,
            }}
          >
            <input
              placeholder="이메일 주소"
              style={{
                flex: 1,
                border: 'none',
                background: 'transparent',
                color: 'var(--color-text-on-dark)',
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
                color: 'var(--color-text-on-dark)',
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
          <p style={{ margin: '0 0 6px', color: 'var(--color-text-on-dark)', fontWeight: 400 }}>
            둘러보기
          </p>
          {[
            { label: '쇼핑', to: '/shop' },
            { label: '생산자', to: '/story' },
            { label: '브랜드', to: '/about' },
          ].map(({ label, to }) => (
            <FooterLink key={to} to={to}>
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
          <p style={{ margin: '0 0 6px', color: 'var(--color-text-on-dark)', fontWeight: 400 }}>
            안내
          </p>
          <p style={{ margin: 0 }}>화·금 수확 및 발송</p>
          <p style={{ margin: 0 }}>4만 원 이상 무료 배송</p>
          <p style={{ margin: 0 }}>대전 유성구 어은로 52</p>
        </div>
      </div>
      <p
        style={{
          margin: '48px 0 0',
          fontSize: 12,
          color: 'var(--color-text-disabled)',
          fontWeight: 300,
        }}
      >
        © 2026 MINS Farmers Market — 대전충남 로컬푸드 직거래
      </p>
    </footer>
  )
}

// span+onClick이 아니라 TextLink(=<a>)를 쓴다. 겉모습은 그대로지만
// Tab 이동·Enter·새 탭으로 열기·스크린리더 인식이 전부 동작하게 된다.
function FooterLink({ to, children }) {
  return (
    <TextLink
      to={to}
      style={{ color: 'var(--color-text-footer)' }}
      onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-bg)')}
      onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-text-footer)')}
    >
      {children}
    </TextLink>
  )
}
