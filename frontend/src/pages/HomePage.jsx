import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { getBestProducts } from '../api/products'
import { addToCart } from '../api/cart'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'

const fmt = (n) => n.toLocaleString('ko-KR') + '원'

export default function HomePage() {
  const [featured, setFeatured] = useState([])
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const { refreshCart, showToast } = useCart()

  /* 베스트 상품 상위 4개를 홈 히어로 그리드에 표시 */
  useEffect(() => {
    getBestProducts()
      .then((res) => setFeatured((res.data.content || []).slice(0, 4)))
      .catch(() => {})
  }, [])

  const handleAddToCart = async (product) => {
    if (!user) {
      alert('로그인 후 담을 수 있어요')
      navigate('/login', { state: { from: location } })
      return
    }
    try {
      await addToCart({ productId: product.id, quantity: 1 })
      refreshCart()
      showToast(product.name)
    } catch {}
  }

  return (
    <div style={{ animation: 'fadeUp .5s ease both', flex: 1 }}>
      {/* ─── 히어로 섹션 ─── */}
      <section
        style={{
          position: 'relative',
          minHeight: '88vh',
          borderBottom: '1px solid #dddaca',
          overflow: 'hidden',
          background: '#2a2a20',
        }}
      >
        <img
          src="/uploads/assets-1784160881665.png"
          alt=""
          style={{
            position: 'absolute',
            inset: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: 'linear-gradient(180deg,rgba(30,30,22,0.15),rgba(30,30,22,0.5))',
          }}
        />
        <div
          style={{
            position: 'relative',
            minHeight: '88vh',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            padding: 'clamp(32px,6vw,96px)',
            gap: 28,
            maxWidth: 640,
          }}
        >
          <p
            style={{
              margin: 0,
              fontSize: 13,
              letterSpacing: '0.14em',
              color: '#d9d6bc',
              fontWeight: 400,
            }}
          >
            제철 · 대전충남 · 소농직거래
          </p>
          <h1
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              color: '#fffef2',
              fontSize: 'clamp(30px,3.8vw,50px)',
              lineHeight: 1.45,
              letterSpacing: '-0.01em',
            }}
          >
            땅이 허락한 만큼만,
            <br />
            계절이 내어준 순서대로.
          </h1>
          <p
            style={{
              margin: 0,
              fontSize: 15,
              lineHeight: 1.9,
              color: '#e5e3d3',
              fontWeight: 300,
              maxWidth: '44ch',
            }}
          >
            민스 파머스 마켓은 대전과 충남의 스물세 곳 소농과 함께합니다. 수확한 지 하루가 지나지
            않은 채소와 과일, 그날 구운 빵을 문 앞까지 전합니다.
          </p>
          <HoverBtn light onClick={() => navigate('/shop')}>
            이번 주 수확물 보기 <span>→</span>
          </HoverBtn>
        </div>
      </section>

      {/* ─── 이번 주의 수확 ─── */}
      <section style={{ padding: 'clamp(40px,6vw,88px) clamp(20px,5vw,72px)' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'baseline',
            marginBottom: 40,
          }}
        >
          <h2
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 26,
            }}
          >
            이번 주의 수확
          </h2>
          <span
            onClick={() => navigate('/shop')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              letterSpacing: '0.05em',
              borderBottom: '1px solid #333330',
              paddingBottom: 2,
            }}
          >
            전체 보기
          </span>
        </div>

        <div
          className="mobile-2col"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4,1fr)',
            gap: 1,
            background: '#dddaca',
            border: '1px solid #dddaca',
          }}
        >
          {featured.length > 0
            ? featured.map((p) => (
                <FeaturedCard
                  key={p.id}
                  product={p}
                  onOpen={() => navigate(`/products/${p.id}`)}
                  onAdd={() => handleAddToCart(p)}
                />
              ))
            : Array.from({ length: 4 }).map((_, i) => (
                <div key={i} style={{ background: '#fffef2', padding: 28 }}>
                  <div style={{ aspectRatio: '4/5', background: '#edeadb' }} />
                </div>
              ))}
        </div>
      </section>

      {/* ─── 정기배송 배너 ─── */}
      <section
        className="mobile-1col"
        style={{
          background: '#333326',
          color: '#e5e3d3',
          padding: 'clamp(48px,8vw,110px) clamp(20px,5vw,72px)',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 'clamp(32px,5vw,80px)',
          alignItems: 'center',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <p style={{ margin: 0, fontSize: 13, letterSpacing: '0.14em', color: '#a9a98f' }}>
            꾸러미 정기배송
          </p>
          <h2
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 'clamp(26px,2.6vw,36px)',
              lineHeight: 1.5,
            }}
          >
            장 볼 필요 없이,
            <br />
            계절이 알아서 식탁을 차립니다.
          </h2>
          <p
            style={{
              margin: 0,
              fontSize: 15,
              lineHeight: 1.9,
              fontWeight: 300,
              color: '#c9c7b2',
              maxWidth: '46ch',
            }}
          >
            매주 그 시기에 가장 좋은 것들로 꾸린 제철 꾸러미. 어떤 채소가 올지 모르는 설렘과, 무엇이
            와도 좋다는 믿음을 함께 담았습니다.
          </p>
          <HoverBtn onClick={() => navigate('/shop')}>
            꾸러미 살펴보기 <span>→</span>
          </HoverBtn>
        </div>
        <div style={{ aspectRatio: '5/4', background: '#3f3f30' }} />
      </section>

      {/* ─── 생산자 인용구 ─── */}
      <section
        style={{
          padding: 'clamp(48px,7vw,100px) clamp(20px,5vw,72px)',
          borderBottom: '1px solid #dddaca',
        }}
      >
        <div
          style={{
            maxWidth: 680,
            margin: '0 auto',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            gap: 24,
            alignItems: 'center',
          }}
        >
          <p style={{ margin: 0, fontSize: 13, letterSpacing: '0.14em', color: '#75775e' }}>
            생산자
          </p>
          <h2
            style={{
              margin: 0,
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 'clamp(24px,2.4vw,32px)',
              lineHeight: 1.6,
            }}
          >
            "농사는 서두른다고 되는 일이 아니에요.
            <br />
            기다리는 게 일의 절반입니다."
          </h2>
          <p style={{ margin: 0, fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
            금산 · 흙내음농원 · 이정순 농부
          </p>
          <span
            onClick={() => navigate('/story')}
            style={{
              cursor: 'pointer',
              fontSize: 13,
              letterSpacing: '0.05em',
              borderBottom: '1px solid #333330',
              paddingBottom: 2,
            }}
          >
            스물세 농가의 이야기 읽기
          </span>
        </div>
      </section>
    </div>
  )
}

/* 4열 그리드 상품 카드 */
function FeaturedCard({ product, onOpen }) {
  const [hovered, setHovered] = useState(false)
  return (
    <div
      onClick={onOpen}
      style={{
        background: hovered ? '#f6f4e6' : '#fffef2',
        cursor: 'pointer',
        padding: 28,
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <div style={{ aspectRatio: '4/5', background: '#edeadb', overflow: 'hidden' }}>
        {product.imageUrl && (
          <img
            src={product.imageUrl}
            alt={product.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        )}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        <h3
          style={{
            margin: 0,
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 400,
            fontSize: 16,
          }}
        >
          {product.name}
        </h3>
        <p style={{ margin: 0, fontSize: 13, color: '#6d6c61', fontWeight: 300 }}>
          {product.description?.slice(0, 30)}
        </p>
        <p style={{ margin: '6px 0 0', fontSize: 14 }}>{fmt(product.price)}</p>
      </div>
    </div>
  )
}

/* 호버 시 색상 반전되는 버튼 */
function HoverBtn({ onClick, children, light = false }) {
  const [hovered, setHovered] = useState(false)
  const border = light ? '#fffef2' : '#e5e3d3'
  const textColor = light ? '#fffef2' : '#e5e3d3'
  const hoverBg = light ? '#fffef2' : '#e5e3d3'
  const hoverText = light ? '#333330' : '#333326'

  return (
    <span
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        cursor: 'pointer',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        border: `1px solid ${border}`,
        color: hovered ? hoverText : textColor,
        background: hovered ? hoverBg : 'transparent',
        padding: '16px 22px',
        maxWidth: 340,
        fontSize: 14,
        letterSpacing: '0.04em',
        userSelect: 'none',
      }}
    >
      {children}
    </span>
  )
}
