import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

/* 정적 농부 데이터 (실제 서비스에서는 백엔드 API로 교체) */
const FARMERS = [
  {
    region: '충남 금산',
    farm: '흙내음농원',
    name: '이정순',
    quote: '농사는 서두른다고 되는 일이 아니에요. 기다리는 게 일의 절반입니다.',
    story:
      '삼십 년째 같은 밭에서 당근과 뿌리채소를 기릅니다. 화학비료 대신 직접 만든 퇴비를 쓰고, 수확은 언제나 주문이 들어온 다음에 합니다.',
    detail:
      '처음 밭을 물려받았을 때는 관행농이었습니다. 십 년쯤 지나 몸이 먼저 신호를 보냈고, 그때부터 흙을 다시 살리는 데 시간을 썼습니다. 지금은 지렁이가 돌아온 밭에서, 당근이 스스로 단맛을 낼 때까지 기다리는 법을 압니다.',
  },
  {
    region: '충남 논산',
    farm: '별빛농장',
    name: '김현우',
    quote: '딸기는 새벽에 따야 해요. 해가 뜨면 향이 절반은 날아갑니다.',
    story:
      '귀농 팔 년 차. 유통 기한 대신 맛을 기준으로 수확 시기를 정합니다. 덜 익은 딸기는 어떤 값을 쳐줘도 따지 않는 것이 원칙입니다.',
    detail:
      '도시에서 회사를 다니다 아버지의 밭을 물려받으며 농사를 시작했습니다. 처음 몇 해는 수확량에 쫓겨 딸기를 일찍 땄고, 그게 늘 마음에 걸렸습니다. 지금은 새벽 네 시에 나가 완전히 익은 것만 고릅니다.',
  },
  {
    region: '충남 서천',
    farm: '바다들녘',
    name: '정미영',
    quote: '잎을 보면 알아요. 급하게 키운 잎은 빛깔부터 다릅니다.',
    story:
      '바닷바람이 닿는 노지에서 잎채소를 기릅니다. 천천히 자란 잎은 조직이 단단해 쉽게 무르지 않고, 씹는 맛이 살아 있습니다.',
    detail:
      '서천 앞바다가 보이는 밭에서 이십 년째 잎채소를 기릅니다. 비료를 많이 쓰면 잎이 크고 빠르게 자라지만, 대신 잎이 얇고 금세 물러집니다. 정미영 농부는 대신 시간을 들입니다.',
  },
]

export default function StoryPage() {
  const navigate = useNavigate()
  /* 선택된 농부 인덱스 (null이면 목록 뷰) */
  const [selectedIdx, setSelectedIdx] = useState(null)

  /* 농부 상세 뷰 */
  if (selectedIdx !== null) {
    const f = FARMERS[selectedIdx]
    return (
      <main style={{ animation: 'fadeUp .4s ease both', flex: 1 }}>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            borderBottom: '1px solid #dddaca',
            minHeight: '76vh',
          }}
        >
          {/* 이미지 자리 (실제 사진으로 교체 가능) */}
          <div
            style={{
              background: '#edeadb',
              position: 'sticky',
              top: 72,
              height: 'calc(100vh - 72px)',
            }}
          />
          <div
            style={{
              padding: 'clamp(32px,5vw,72px)',
              display: 'flex',
              flexDirection: 'column',
              gap: 22,
            }}
          >
            <span
              onClick={() => setSelectedIdx(null)}
              style={{ cursor: 'pointer', fontSize: 13, color: '#6d6c61' }}
            >
              ← 농부 이야기로 돌아가기
            </span>
            <p style={{ margin: 0, fontSize: 12, letterSpacing: '0.14em', color: '#75775e' }}>
              {f.region}
            </p>
            <h1
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontWeight: 300,
                fontSize: 'clamp(26px,2.6vw,34px)',
              }}
            >
              {f.farm} · {f.name}
            </h1>
            <p
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontStyle: 'italic',
                fontWeight: 300,
                fontSize: 18,
                lineHeight: 1.8,
                color: '#4a4a3a',
              }}
            >
              "{f.quote}"
            </p>
            <p
              style={{
                margin: 0,
                fontSize: 15,
                lineHeight: 2,
                color: '#6d6c61',
                fontWeight: 300,
              }}
            >
              {f.story}
            </p>
            <p
              style={{
                margin: 0,
                fontSize: 15,
                lineHeight: 2,
                color: '#6d6c61',
                fontWeight: 300,
              }}
            >
              {f.detail}
            </p>
            <span
              onClick={() => navigate('/shop')}
              style={{
                cursor: 'pointer',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                border: '1px solid #333330',
                padding: '16px 22px',
                maxWidth: 340,
                fontSize: 14,
                marginTop: 8,
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = '#333330'
                e.currentTarget.style.color = '#fffef2'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = ''
                e.currentTarget.style.color = ''
              }}
            >
              이 농가의 상품 보기 <span>→</span>
            </span>
          </div>
        </div>
      </main>
    )
  }

  /* 농부 목록 뷰 */
  return (
    <main style={{ animation: 'fadeUp .4s ease both', flex: 1 }}>
      <section
        style={{
          padding: 'clamp(48px,7vw,96px) clamp(20px,5vw,72px)',
          borderBottom: '1px solid #dddaca',
          maxWidth: 820,
        }}
      >
        <p style={{ margin: '0 0 16px', fontSize: 13, letterSpacing: '0.14em', color: '#75775e' }}>
          생산자
        </p>
        <h1
          style={{
            margin: '0 0 24px',
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 300,
            fontSize: 'clamp(28px,3vw,40px)',
            lineHeight: 1.5,
          }}
        >
          얼굴을 아는 사람의 농사
        </h1>
        <p style={{ margin: 0, fontSize: 15, lineHeight: 2, color: '#6d6c61', fontWeight: 300 }}>
          민스 파머스 마켓의 모든 상품에는 기른 사람의 이름이 적혀 있습니다. 우리는 대전과 충남의
          스물세 농가를 계절마다 직접 찾아가고, 밭의 상태와 농부의 원칙을 눈으로 확인한 뒤에야
          함께합니다.
        </p>
      </section>

      {FARMERS.map((f, i) => (
        <section
          key={i}
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            borderBottom: '1px solid #dddaca',
          }}
        >
          {/* 이미지 자리 */}
          <div style={{ aspectRatio: '4/3', background: '#edeadb' }} />

          <div
            style={{
              padding: 'clamp(32px,5vw,72px)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              gap: 18,
            }}
          >
            <p style={{ margin: 0, fontSize: 12, letterSpacing: '0.14em', color: '#75775e' }}>
              {f.region}
            </p>
            <h2
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontWeight: 400,
                fontSize: 24,
              }}
            >
              {f.farm} · {f.name}
            </h2>
            <p
              style={{
                margin: 0,
                fontFamily: "'Noto Serif KR', serif",
                fontStyle: 'italic',
                fontWeight: 300,
                fontSize: 17,
                lineHeight: 1.8,
                color: '#4a4a3a',
              }}
            >
              "{f.quote}"
            </p>
            <p
              style={{
                margin: 0,
                fontSize: 14,
                lineHeight: 1.9,
                color: '#6d6c61',
                fontWeight: 300,
              }}
            >
              {f.story}
            </p>
            <span
              onClick={() => setSelectedIdx(i)}
              style={{
                cursor: 'pointer',
                alignSelf: 'flex-start',
                fontSize: 13,
                letterSpacing: '0.05em',
                borderBottom: '1px solid #333330',
                paddingBottom: 2,
              }}
            >
              이야기 더 보기 →
            </span>
          </div>
        </section>
      ))}
    </main>
  )
}
