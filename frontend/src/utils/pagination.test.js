// isLastPage의 계약을 명시하는 테스트.
//
// 이 파일이 "구현보다 먼저" 있는 이유(TDD): 경계 판정은 off-by-one 실수가
// 가장 나기 쉬운 곳이라, 어떤 입력에 어떤 답이 나와야 하는지를 코드로 먼저
// 못 박아두고 그걸 만족시키는 식을 쓰는 편이 안전하다.
//
// 실제로 이 프로젝트에서 같은 종류의 실수를 두 번 했다:
//   - DEF-9: `orders.length === 0` (한 칸 늦게 잠김)
//   - 상품 목록: `(page + 1) * 10 >= totalElements` (페이지 크기 하드코딩)
import { describe, it, expect } from 'vitest'
import { isLastPage } from './pagination'

describe('isLastPage — 마지막 페이지 판정', () => {
  describe('일반적인 경우', () => {
    it('마지막 페이지면 true (5페이지 중 5번째 = page 4)', () => {
      // page는 0부터 시작하므로 totalPages 5의 마지막은 page 4다.
      // 여기서 4 대신 5를 기준으로 잡는 off-by-one이 가장 흔한 실수다.
      expect(isLastPage(4, 5)).toBe(true)
    })

    it('마지막 직전 페이지면 false — 아직 넘길 곳이 남았다', () => {
      expect(isLastPage(3, 5)).toBe(false)
    })

    it('첫 페이지이고 뒤에 더 있으면 false', () => {
      expect(isLastPage(0, 3)).toBe(false)
    })

    it('페이지가 1개뿐이면 첫 페이지가 곧 마지막 페이지다', () => {
      // DEF-9의 실제 재현 조건 — 주문 3건, totalPages 1.
      // 목록에 내용은 있으므로 옛 조건(length === 0)으로는 안 잠겼다.
      expect(isLastPage(0, 1)).toBe(true)
    })
  })

  describe('결과가 0건인 경우', () => {
    it('totalPages가 0이면 true — 넘길 페이지가 없다', () => {
      // Spring은 조회 결과가 아예 없으면 totalPages를 0으로 준다.
      // 검색·필터로 결과가 0건이 나온 빈 화면에서 "다음"이 열려 있으면 안 된다.
      expect(isLastPage(0, 0)).toBe(true)
    })
  })

  describe('현재 페이지가 범위를 벗어난 경우', () => {
    it('필터 때문에 페이지 수가 줄어 page가 범위를 넘어가도 true', () => {
      // 5페이지를 보다가 필터를 걸어 결과가 1페이지로 줄어든 순간.
      // page(3)가 유효 범위(0)를 넘어섰다 — 더 넘길 수 있게 두면 안 된다.
      expect(isLastPage(3, 1)).toBe(true)
    })

    it('범위를 한참 벗어나도 true', () => {
      expect(isLastPage(99, 2)).toBe(true)
    })
  })

  describe('아직 응답이 오지 않았거나 값이 없는 경우', () => {
    // 방어적 처리 — utils/imageUpload.js의 validateImageFile이 !file을 먼저
    // 걸러내는 것과 같은 이유다. 순수 함수는 누가 어떻게 부르든 터지지 않는 게 좋고,
    // 특히 "모를 때는 잠그는 쪽"이 안전하다. 잘못 열어두면 사용자가 빈 화면을 보지만,
    // 잘못 잠그면 다음 렌더에서 곧 풀린다.
    it('totalPages가 undefined면 true (판단 불가 → 잠근다)', () => {
      expect(isLastPage(0, undefined)).toBe(true)
    })

    it('totalPages가 null이면 true', () => {
      expect(isLastPage(0, null)).toBe(true)
    })

    it('totalPages가 숫자가 아니면 true', () => {
      expect(isLastPage(0, NaN)).toBe(true)
    })
  })
})
