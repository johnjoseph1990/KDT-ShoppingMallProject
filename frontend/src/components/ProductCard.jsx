import { useState } from 'react'
import { Link } from 'react-router-dom'
import { fmt, stockBadge } from '../utils/product'

// ProductCard: 상품 목록(featured=false)과 홈 베스트 섹션(featured=true) 두 가지 모드를 지원한다.
// featured prop 하나로 분기해 코드 중복을 줄인 것이 이 컴포넌트의 핵심 설계다.
//
// 시맨틱 구조 (2026-08-02 DEF-7에서 개편):
//   <article> — 독립적으로 의미가 성립하는 콘텐츠 조각. 목록의 한 항목에 알맞은 태그다.
//     └ 상품명 안의 <Link> 하나만 진짜 링크이고, 그 링크가 CSS ::after로 카드 전체를
//       덮어 마우스 클릭 영역을 넓힌다(index.css의 .product-card-link 참고).
//       예전에는 div/span에 onClick을 달아둬서 키보드로는 아예 쓸 수 없었다.
//
// props:
//   product  — 상품 데이터 객체 (id, name, description, price, imageUrl, stockQuantity, averageRating)
//   to       — 상품 상세 경로. onOpen 콜백을 대신한다 (진짜 <a>가 되려면 경로가 필요하기 때문)
//   onAdd    — 장바구니 버튼 클릭 핸들러 (featured=true이면 버튼이 없으므로 실제로 호출되지 않음)
//   featured — true면 홈 베스트 카드(배지·별점·장바구니 없음), false면 목록 카드(기본값)
export default function ProductCard({ product, to, onAdd, featured = false }) {
  // JSX에서 boolean prop은 값 없이 이름만 써도 true가 된다. <ProductCard featured />는 featured={true}와 같다.
  // 기본값을 false로 두면 featured를 생략하면 목록 카드로 동작한다.
  const [hovered, setHovered] = useState(false)

  // 훅(useState 등)은 조건부로 호출하면 React "Rules of Hooks" 위반이다.
  // hovered는 항상 선언하되, soldOut/badge 계산만 featured에 따라 분기한다.
  const soldOut = !featured && product.stockQuantity === 0
  const badge = !featured ? stockBadge(product.stockQuantity) : null

  // featured 모드별 설명 글자 수 차이를 상수로 분리해 가독성을 높인다
  const descLimit = featured ? 30 : 40

  return (
    <article
      // className="product-card": position:relative를 줘서 상품명 링크의 ::after가
      // 이 카드를 기준으로 펼쳐지게 한다 (index.css)
      className="product-card"
      style={{
        background: hovered ? 'var(--color-bg-hover-light)' : 'var(--color-bg)',
        padding: 28,
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {/* 이미지 영역
          featured=false일 때만 position:'relative'가 필요하다.
          재고 배지(position:'absolute')의 기준점 역할을 해야 하기 때문이다.
          featured=true는 배지 자체가 없으므로 relative 설정이 불필요하다. */}
      {/* 이미지에는 onClick을 달지 않는다 — 상품명 링크의 ::after가 이 위를 덮고 있어
          여기를 눌러도 링크가 눌린다. 클릭 핸들러가 중복으로 필요 없다. */}
      <div
        style={{
          aspectRatio: '4/5',
          background: 'var(--color-bg-hover)',
          overflow: 'hidden',
          position: !featured ? 'relative' : undefined,
        }}
      >
        {product.imageUrl && (
          <img
            src={product.imageUrl}
            alt={product.name}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              // featured=false이고 품절일 때만 흑백+반투명 처리한다.
              // featured 카드에서는 재고 상태를 표시하지 않는 기획이므로 조건을 분리한다.
              filter: soldOut ? 'grayscale(1)' : 'none',
              opacity: soldOut ? 0.5 : 1,
            }}
          />
        )}

        {/* 재고 배지: featured=false이고 badge가 있을 때만 렌더링 */}
        {!featured && badge && (
          <span
            style={{
              position: 'absolute',
              top: 10,
              left: 10,
              background: badge.color,
              color: 'var(--color-bg)',
              fontSize: 11,
              letterSpacing: '0.06em',
              padding: '4px 9px',
              fontWeight: 400,
            }}
          >
            {badge.text}
          </span>
        )}
      </div>

      {/* 상품 정보 텍스트 영역 */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: 6,
          // featured=false일 때 flex:1을 주는 이유:
          // 카드 높이가 달라도 장바구니 버튼이 항상 카드 아래에 고정되게 하기 위함이다.
          flex: !featured ? 1 : undefined,
        }}
      >
        {/* 상품명: 두 모드의 폰트 크기가 1px 다르다 (featured=true는 16px, false는 17px)
            이 카드에서 유일한 "진짜 링크"다. className="product-card-link"가 붙은 덕에
            보이지 않는 ::after가 카드 전체로 늘어나 어디를 눌러도 이 링크가 눌린다.
            스크린리더도 "링크: 충남 금산 당근"처럼 상품명을 링크 이름으로 읽어준다. */}
        <h3
          style={{
            margin: 0,
            fontFamily: "'Noto Serif KR', serif",
            fontWeight: 400,
            fontSize: featured ? 16 : 17,
          }}
        >
          <Link
            to={to}
            className="product-card-link"
            style={{ color: 'inherit', textDecoration: 'none' }}
          >
            {product.name}
          </Link>
        </h3>
        <p
          style={{
            margin: 0,
            fontSize: 13,
            color: 'var(--color-fg-muted)',
            fontWeight: 300,
            lineHeight: 1.7,
          }}
        >
          {product.description?.slice(0, descLimit)}
        </p>

        {/* 평균 별점: featured=false일 때만 표시.
            toFixed(1): 4.333... 같은 값을 소수점 한 자리로 반올림해 문자열로 만든다. */}
        {!featured && (
          <p
            style={{
              margin: '2px 0 0',
              fontSize: 12,
              color: 'var(--color-fg-muted)',
              fontWeight: 300,
            }}
          >
            {product.averageRating > 0 ? `★ ${product.averageRating.toFixed(1)}` : '리뷰 없음'}
          </p>
        )}

        <p style={{ margin: '6px 0 0', fontSize: 14 }}>{fmt(product.price)}</p>
      </div>

      {/* 장바구니 버튼: featured=false일 때만 렌더링
          disabled를 쓰면 키보드 탭 이동에서도 건너뛰어져 접근성 측면에서 더 정확하다.
          SVG는 stroke="currentColor"라 버튼 color를 따라가고, hover 시 함께 반전된다. */}
      {!featured && (
        <button
          onClick={onAdd}
          disabled={soldOut}
          aria-label={soldOut ? '품절된 상품' : '장바구니에 담기'}
          // className="product-card-action": 상품명 링크의 투명한 ::after 판이
          // 이 버튼 위를 덮고 있으므로, z-index로 버튼을 그 위로 올려야 클릭이 먹는다
          className="product-card-action"
          style={{
            cursor: soldOut ? 'not-allowed' : 'pointer',
            border: `1px solid ${soldOut ? 'var(--color-border)' : 'var(--color-fg)'}`,
            background: 'transparent',
            color: soldOut ? 'var(--color-text-disabled)' : 'var(--color-fg)',
            padding: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          onMouseEnter={(e) => {
            if (soldOut) return // 품절 버튼은 hover 반전을 하지 않는다
            e.currentTarget.style.background = 'var(--color-fg)'
            e.currentTarget.style.color = 'var(--color-bg)'
          }}
          onMouseLeave={(e) => {
            if (soldOut) return
            e.currentTarget.style.background = 'transparent'
            e.currentTarget.style.color = 'var(--color-fg)'
          }}
        >
          {/* 카트 아이콘: 바퀴 두 개(circle) + 카트 몸통(path) */}
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="9" cy="20" r="1" />
            <circle cx="18" cy="20" r="1" />
            <path d="M2 3h3l2.4 12.4a1 1 0 0 0 1 .8h9.2a1 1 0 0 0 1-.8L21 7H6" />
          </svg>
        </button>
      )}
    </article>
  )
}
