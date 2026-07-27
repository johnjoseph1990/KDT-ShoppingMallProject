// Toast는 CartContext의 useCart()로 toast 상태를 읽는다.
// 실제 Context Provider 없이 테스트하려면 모듈 자체를 mock으로 교체해야 한다.
import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'

// vi.mock: import 구문이 실행되기 전에 모듈을 교체한다 (hoisting 처리됨).
// 팩토리 함수 안에서 useCart를 vi.fn()으로 만든다 → 각 테스트에서 반환값을 제어 가능.
vi.mock('../context/CartContext', () => ({
  useCart: vi.fn(),
}))

// mock 선언 이후에 import해야 교체된 버전을 받는다
import Toast from './Toast'
import { useCart } from '../context/CartContext'

describe('Toast', () => {
  // beforeEach: 각 it 블록 실행 전마다 호출된다.
  // vi.clearAllMocks(): 이전 테스트의 mock 호출 기록을 초기화해 테스트 간 격리를 보장한다.
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('toast가 null이면 아무것도 렌더링하지 않는다', () => {
    // mockReturnValue: useCart()가 호출될 때 반환할 값을 지정한다.
    useCart.mockReturnValue({ toast: null })
    const { container } = render(<Toast />)
    // container.firstChild가 null이면 렌더링 결과가 없다는 뜻이다.
    expect(container.firstChild).toBeNull()
  })

  it('error 타입이면 에러 토스트 배경 변수로 렌더링된다', () => {
    useCart.mockReturnValue({ toast: { text: '오류 발생', type: 'error' } })
    render(<Toast />)
    const el = screen.getByText('오류 발생')
    // CSS 변수는 jsdom에서 실제 값으로 resolve되지 않으므로 var() 문자열로 검증한다.
    // 실제 렌더링 색상은 브라우저 확인으로 검증한다.
    expect(el).toHaveStyle({ background: 'var(--color-danger-dark)' })
  })

  it('success 타입이면 다크 배경 변수로 렌더링된다', () => {
    useCart.mockReturnValue({ toast: { text: '장바구니에 담겼습니다', type: 'success' } })
    render(<Toast />)
    const el = screen.getByText('장바구니에 담겼습니다')
    expect(el).toHaveStyle({ background: 'var(--color-bg-dark)' })
  })

  it('toast 텍스트가 화면에 표시된다', () => {
    useCart.mockReturnValue({ toast: { text: '테스트 메시지', type: 'success' } })
    render(<Toast />)
    expect(screen.getByText('테스트 메시지')).toBeInTheDocument()
  })
})
