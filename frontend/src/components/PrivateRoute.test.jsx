// PrivateRoute — 프론트엔드의 유일한 인가(authorization) 로직인데 여태 테스트가 없었다.
// 여기가 깨지면 비로그인 사용자에게 주문 내역이, 일반 사용자에게 관리자 화면이 그대로 노출된다.
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PrivateRoute from './PrivateRoute'

// useAuth()를 가짜로 대체한다. 실제 로그인 API를 태우지 않고 "지금 누가 로그인해 있는가"만
// 테스트마다 원하는 값으로 바꿔치기하기 위해서다.
// vi.mock은 파일 최상단으로 끌어올려지므로(hoisting), 모듈 팩토리 안에서 mockUser를
// 직접 참조하지 말고 아래처럼 함수 호출 시점에 읽어야 한다.
let mockUser = null
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: mockUser }),
}))

// 보호 대상 화면. 이 텍스트가 보이면 "통과", 안 보이면 "차단"이다.
const 보호된내용 = <div>주문 내역 상세</div>

// PrivateRoute는 화면을 그리는 대신 <Navigate>로 다른 경로를 반환한다.
// 따라서 리다이렉트 도착지(/login, /)까지 Routes에 등록해둬야 "어디로 튕겼는지" 확인할 수 있다.
const renderGuard = (path, { adminOnly = false } = {}) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<div>로그인 화면</div>} />
        <Route path="/" element={<div>홈 화면</div>} />
        <Route
          path={path}
          element={<PrivateRoute adminOnly={adminOnly}>{보호된내용}</PrivateRoute>}
        />
      </Routes>
    </MemoryRouter>,
  )

beforeEach(() => {
  mockUser = null // 테스트 간 로그인 상태가 새지 않도록 초기화
})

describe('PrivateRoute — 로그인 여부', () => {
  it('비로그인 사용자는 로그인 화면으로 보낸다', () => {
    mockUser = null

    renderGuard('/orders')

    expect(screen.getByText('로그인 화면')).toBeInTheDocument()
    // 리다이렉트 확인만으로는 부족하다. 보호된 내용이 "새지 않았다"를 못박아야 인가 테스트가 된다.
    expect(screen.queryByText('주문 내역 상세')).not.toBeInTheDocument()
  })

  it('로그인한 사용자는 보호된 화면을 볼 수 있다', () => {
    mockUser = { email: 'test@shop.com', role: 'USER' }

    renderGuard('/orders')

    expect(screen.getByText('주문 내역 상세')).toBeInTheDocument()
  })
})

describe('PrivateRoute — 관리자 전용(adminOnly)', () => {
  it('일반 사용자가 관리자 화면에 접근하면 홈으로 보낸다', () => {
    // 가장 위험한 시나리오. 로그인은 되어 있으므로 첫 번째 가드(!user)는 통과하고,
    // 두 번째 가드(role !== 'ADMIN')만이 유일한 방어선이다.
    mockUser = { email: 'test@shop.com', role: 'USER' }

    renderGuard('/admin', { adminOnly: true })

    expect(screen.getByText('홈 화면')).toBeInTheDocument()
    expect(screen.queryByText('주문 내역 상세')).not.toBeInTheDocument()
  })

  it('관리자는 관리자 화면을 볼 수 있다', () => {
    mockUser = { email: 'admin@shop.com', role: 'ADMIN' }

    renderGuard('/admin', { adminOnly: true })

    expect(screen.getByText('주문 내역 상세')).toBeInTheDocument()
  })

  it('adminOnly가 아닌 화면은 일반 사용자도 볼 수 있다', () => {
    // adminOnly 기본값(false)이 실수로 true처럼 동작하면 모든 보호 화면이 관리자 전용이 된다.
    mockUser = { email: 'test@shop.com', role: 'USER' }

    renderGuard('/orders')

    expect(screen.getByText('주문 내역 상세')).toBeInTheDocument()
  })
})
