// 컴포넌트 테스트 — RTL(React Testing Library)로 "사용자 관점"에서 검증한다.
// RTL 철학: DOM 구조나 state를 직접 보지 말고, 화면에 보이는 텍스트·역할(role)로 찾는다.
// → 리팩토링(내부 구현 변경)해도 테스트가 깨지지 않는 장점이 있다.
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import ProductCard from './ProductCard'

// 테스트용 최소 상품 데이터 — 실제 API 호출 없이 props로 주입한다
const baseProduct = {
  id: 1,
  name: '테스트 상품',
  description: '테스트 상품 설명입니다',
  price: 15000,
  imageUrl: null, // 이미지 없는 상태로 렌더링 여부 검증
  stockQuantity: 10,
  averageRating: 4.5,
}

describe('ProductCard — 기본 렌더링', () => {
  it('상품명과 가격이 화면에 표시된다', () => {
    // render(): jsdom에 컴포넌트를 마운트한다. 실제 브라우저 없이 DOM을 만든다.
    render(<ProductCard product={baseProduct} onOpen={() => {}} onAdd={() => {}} />)

    // screen.getByText(): 텍스트로 DOM 요소를 찾는다. 없으면 테스트 즉시 실패.
    expect(screen.getByText('테스트 상품')).toBeInTheDocument()
    expect(screen.getByText('15,000원')).toBeInTheDocument()
  })

  it('별점이 있으면 ★ 형식으로 표시된다', () => {
    render(<ProductCard product={baseProduct} onOpen={() => {}} onAdd={() => {}} />)
    expect(screen.getByText('★ 4.5')).toBeInTheDocument()
  })

  it('별점이 0이면 "리뷰 없음"으로 표시된다', () => {
    const noRatingProduct = { ...baseProduct, averageRating: 0 }
    render(<ProductCard product={noRatingProduct} onOpen={() => {}} onAdd={() => {}} />)
    expect(screen.getByText('리뷰 없음')).toBeInTheDocument()
  })
})

describe('ProductCard — featured 모드 분기', () => {
  it('featured=false(기본값): 장바구니 버튼이 표시된다', () => {
    render(<ProductCard product={baseProduct} onOpen={() => {}} onAdd={() => {}} />)
    // getByRole: 접근성 역할(role)로 요소를 찾는다. aria-label을 name으로 검색.
    // 이 방식은 스크린리더 사용자도 접근 가능한지를 동시에 검증한다.
    expect(screen.getByRole('button', { name: '장바구니에 담기' })).toBeInTheDocument()
  })

  it('featured=true: 장바구니 버튼이 없다', () => {
    render(<ProductCard product={baseProduct} onOpen={() => {}} onAdd={() => {}} featured />)
    // queryByRole: 없으면 null 반환 (getByRole은 없으면 throw).
    // 존재하지 않아야 하는 요소를 검증할 때 query* 계열을 쓴다.
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('featured=true: 별점이 표시되지 않는다', () => {
    render(<ProductCard product={baseProduct} onOpen={() => {}} onAdd={() => {}} featured />)
    expect(screen.queryByText(/★/)).not.toBeInTheDocument()
  })
})

describe('ProductCard — 품절 처리', () => {
  const soldOutProduct = { ...baseProduct, stockQuantity: 0 }

  it('품절 상품은 버튼이 disabled 처리된다', () => {
    render(<ProductCard product={soldOutProduct} onOpen={() => {}} onAdd={() => {}} />)
    const btn = screen.getByRole('button', { name: '품절된 상품' })
    // toBeDisabled(): disabled 속성이 있는지 확인한다.
    expect(btn).toBeDisabled()
  })

  it('품절 상품의 버튼을 클릭해도 onAdd가 호출되지 않는다', () => {
    // vi.fn(): Vitest의 mock 함수. 호출 횟수·인자를 추적할 수 있다.
    const onAdd = vi.fn()
    render(<ProductCard product={soldOutProduct} onOpen={() => {}} onAdd={onAdd} />)
    fireEvent.click(screen.getByRole('button', { name: '품절된 상품' }))
    // toHaveBeenCalledTimes(0): disabled 버튼은 이벤트를 발생시키지 않는다.
    expect(onAdd).toHaveBeenCalledTimes(0)
  })
})

describe('ProductCard — 인터랙션', () => {
  it('장바구니 버튼 클릭 시 onAdd가 1회 호출된다', () => {
    const onAdd = vi.fn()
    render(<ProductCard product={baseProduct} onOpen={() => {}} onAdd={onAdd} />)
    fireEvent.click(screen.getByRole('button', { name: '장바구니에 담기' }))
    expect(onAdd).toHaveBeenCalledOnce()
  })
})
