import { useState } from 'react'
import TextLink from '../components/TextLink'

const STATS = [
  {
    num: '50',
    unit: 'km',
    text: '모든 상품은 물류센터 기준 50km 이내의 밭에서 옵니다. 이동 거리가 짧을수록 맛은 온전하고, 탄소는 적게 남습니다.',
  },
  {
    num: '24',
    unit: '시간',
    text: '수확에서 문 앞까지 하루를 넘기지 않습니다. 냉장 창고 대신 시간표를 촘촘하게 짜는 쪽을 택했습니다.',
  },
  {
    num: '23',
    unit: '농가',
    text: '규모보다 원칙을 보고 함께할 농가를 정합니다. 스물세 곳 모두 이름과 얼굴을 걸고 농사를 짓습니다.',
  },
]

export default function AboutPage() {
  // useNavigate가 없어진 이유: 이 페이지의 유일한 클릭이 페이지 이동인데,
  // 이제 TextLink(<a>)가 그 일을 맡아 프로그래밍 방식 이동이 필요 없다 (DEF-7)
  return (
    <main style={{ animation: 'fadeUp .4s ease both', flex: 1 }}>
      {/* 헤드라인 */}
      <section
        style={{
          padding: 'clamp(56px,9vw,120px) clamp(20px,5vw,72px)',
          borderBottom: '1px solid var(--color-border)',
          textAlign: 'center',
        }}
      >
        <h1
          style={{
            margin: '0 auto',
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 300,
            fontSize: 'clamp(28px,3.2vw,44px)',
            lineHeight: 1.6,
            maxWidth: '22ch',
          }}
        >
          가까운 땅에서 온 것이
          <br />
          가장 멀리까지 우리를 돌봅니다.
        </h1>
      </section>

      {/* 통계 3열. mobile-1col: 768px 이하에서 1열로 쌓임 (index.css) */}
      <section
        className="mobile-1col"
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr 1fr',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        {STATS.map((s, i) => (
          <div
            key={i}
            style={{
              padding: 'clamp(32px,4vw,56px)',
              borderRight: i < 2 ? '1px solid var(--color-border)' : 'none',
              display: 'flex',
              flexDirection: 'column',
              gap: 14,
            }}
          >
            <p
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontSize: 30,
                fontWeight: 300,
              }}
            >
              {s.num}
              <span style={{ fontSize: 16 }}>{s.unit}</span>
            </p>
            <p
              style={{
                margin: 0,
                fontSize: 14,
                lineHeight: 1.9,
                color: 'var(--color-fg-muted)',
                fontWeight: 300,
              }}
            >
              {s.text}
            </p>
          </div>
        ))}
      </section>

      {/* 브랜드 스토리 */}
      <section
        style={{
          padding: 'clamp(48px,7vw,96px) clamp(20px,5vw,72px)',
          maxWidth: 760,
          display: 'flex',
          flexDirection: 'column',
          gap: 22,
        }}
      >
        <p
          style={{
            margin: 0,
            fontSize: 13,
            letterSpacing: '0.14em',
            color: 'var(--color-fg-accent)',
          }}
        >
          우리의 약속
        </p>
        <p
          style={{
            margin: 0,
            fontSize: 15,
            lineHeight: 2.1,
            color: 'var(--color-fg-subtle)',
            fontWeight: 300,
          }}
        >
          민스 파머스 마켓은 2019년 대전의 작은 직거래 장터에서 시작했습니다. 좋은 먹거리는 광고가
          아니라 관계에서 온다고 믿습니다. 우리는 농부에게 제값을 치르고, 흠집 난 과일을 버리지
          않으며, 계절에 없는 것을 팔지 않습니다. 느리지만 정직한 이 방식이, 당신의 식탁과 이 지역의
          땅을 함께 건강하게 만든다고 생각합니다.
        </p>
        <ShopBtn to="/shop">이번 주 수확물 보기 →</ShopBtn>
      </section>
    </main>
  )
}

/* 이름은 Btn이지만 하는 일은 페이지 이동뿐이라 <button>이 아니라 링크가 맞다 (DEF-7) */
function ShopBtn({ to, children }) {
  const [hovered, setHovered] = useState(false)
  return (
    <TextLink
      to={to}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: 'inline-flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        border: '1px solid var(--color-fg)',
        padding: '16px 22px',
        maxWidth: 340,
        fontSize: 14,
        background: hovered ? 'var(--color-fg)' : 'transparent',
        color: hovered ? 'var(--color-bg)' : 'var(--color-fg)',
        userSelect: 'none',
      }}
    >
      {children}
    </TextLink>
  )
}
