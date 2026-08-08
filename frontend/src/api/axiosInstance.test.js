// axiosInstance의 401 응답 인터셉터 테스트.
// 이 인터셉터가 조용히 망가지면 "서버 세션은 끊겼는데 프론트만 로그인 상태로 남는" 불일치가
// 복구되지 않는다. 사용자는 계속 로그인된 화면을 보면서 모든 요청이 실패하는 상태에 빠진다.
import { describe, it, expect, vi, beforeEach } from 'vitest'
import axiosInstance, { setUnauthorizedHandler } from './axiosInstance'

// adapter: axios가 실제로 네트워크를 호출하는 마지막 부품.
// 이걸 "항상 지정한 상태코드로 거절하는 함수"로 바꿔치면, 서버 없이도 인터셉터 경로를
// 그대로 태울 수 있다. config를 그대로 돌려줘야 인터셉터가 error.config.url을 읽을 수 있다.
const 실패응답으로_바꾸기 = (status) => {
  axiosInstance.defaults.adapter = (config) => Promise.reject({ config, response: { status } })
}

describe('axiosInstance — 401 자동 로그아웃 인터셉터', () => {
  let onUnauthorized

  beforeEach(() => {
    // vi.fn(): 호출 여부·횟수를 기록하는 가짜 함수(스파이).
    onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
  })

  it('일반 API에서 401이 오면 자동 로그아웃 콜백을 호출한다', async () => {
    실패응답으로_바꾸기(401)

    // rejects: 프로미스가 거절될 것을 기대한다. 인터셉터가 에러를 삼키지 않고
    // 호출부(try/catch)까지 계속 전파하는지도 여기서 함께 확인된다.
    await expect(axiosInstance.get('/cart')).rejects.toBeDefined()

    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('로그인 요청의 401은 자동 로그아웃을 일으키지 않는다', async () => {
    // 비밀번호를 틀렸을 뿐인데 로그아웃 처리가 돌면, 로그인 화면이 새로고침되며
    // 사용자가 입력하던 내용이 날아간다. /auth/ 로 시작하는 경로를 제외하는 이유다.
    실패응답으로_바꾸기(401)

    await expect(axiosInstance.post('/auth/login')).rejects.toBeDefined()

    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('세션 확인(/auth/me)의 401도 자동 로그아웃을 일으키지 않는다', async () => {
    // 앱을 처음 열 때 비로그인 상태면 /auth/me 는 401이 나는 게 정상이다.
    // 이걸 로그아웃으로 처리하면 새로고침마다 불필요한 상태 초기화가 돈다.
    실패응답으로_바꾸기(401)

    await expect(axiosInstance.get('/auth/me')).rejects.toBeDefined()

    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('401이 아닌 오류(403)에는 반응하지 않는다', async () => {
    // 403은 "로그인은 됐지만 권한이 없다"는 뜻이다. 세션은 멀쩡하므로 로그아웃시키면 안 된다.
    실패응답으로_바꾸기(403)

    await expect(axiosInstance.get('/admin/orders')).rejects.toBeDefined()

    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('콜백이 등록되지 않은 상태의 401도 오류 없이 지나간다', async () => {
    // AuthProvider가 마운트되기 전에 요청이 나갈 수 있다. 이때 onUnauthorized는 null이라
    // 그대로 호출하면 TypeError가 난다. 인터셉터의 세 번째 AND 조건이 이걸 막는다.
    setUnauthorizedHandler(null)
    실패응답으로_바꾸기(401)

    await expect(axiosInstance.get('/cart')).rejects.toBeDefined()
  })
})
