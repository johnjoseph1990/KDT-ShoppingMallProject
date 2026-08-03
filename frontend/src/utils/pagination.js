// 페이징 "경계 판정" 규칙을 모아둔 모듈.
//
// 왜 화면 코드에서 분리했나 (2026-08-02에 같은 종류의 결함을 두 번 만났다):
//
//  ① 관리자 페이저 (DEF-9) — 조건이 `orders.length === 0`이었다.
//     "이미 빈 페이지에 도착했을 때"만 잠기는 한 칸 늦은 판정이라,
//     사용자는 끝을 지나쳐 빈 표를 본 뒤에야 끝인 걸 알았다.
//
//  ② 상품 목록 페이저 — 조건이 `(page + 1) * 10 >= totalElements`였다.
//     결과는 맞았지만 페이지 크기 10이 화면 코드에 박혀 있었다.
//     진짜 출처는 백엔드 ProductController의 @PageableDefault(size = 10)인데,
//     그 값이 Java 파일과 JSX 파일 두 곳에 따로 적혀 있으니
//     서버에서 size를 20으로 바꾸면 화면만 조용히 틀려진다. (잠재 결함)
//
// 해결: 백엔드 Page 응답이 **이미 계산해서 보내주는 totalPages**만 쓴다.
// 그러면 프론트는 페이지 크기를 아예 알 필요가 없어지고,
// 서버가 크기를 바꿔도 화면 코드는 손댈 곳이 없다.
//
// 참고 — Spring이 Page 객체로 내려주는 값들:
//   { content: [...], totalElements: 23, totalPages: 3, number: 0, last: false, ... }
// 예전에는 프론트가 content만 꺼내 쓰고 나머지를 버리는 바람에
// "지금이 마지막 페이지인지"를 알 방법이 없었다.

/**
 * 지금 보고 있는 페이지가 마지막 페이지인지 판정한다.
 * (= "다음" 버튼을 잠가야 하는가?)
 *
 * @param {number} page 현재 페이지 번호 (0부터 시작 — Spring과 같은 기준)
 * @param {number} totalPages 백엔드가 알려준 전체 페이지 수
 * @returns {boolean} 마지막 페이지이거나 판단할 수 없으면 true
 */
export const isLastPage = (page, totalPages) => {
  // ① 먼저 totalPages를 믿을 수 있는지 검사한다.
  //    Number.isFinite는 undefined·null·NaN·Infinity를 한 번에 걸러낸다.
  //    (전역 isFinite와 다르다 — 전역 isFinite(null)은 null을 0으로 강제변환해서
  //     true가 나와버린다. Number.isFinite는 변환 없이 "숫자인가"부터 보므로 false다.)
  //    판단할 수 없으면 잠그는 쪽(true)을 택한다 — 잘못 잠기면 다음 렌더에서 풀리지만,
  //    잘못 열리면 사용자가 빈 화면을 보게 된다.
  if (!Number.isFinite(totalPages)) return true

  // ② 정상 경계 판정. page는 0부터 시작하므로 totalPages 5의 마지막은 page 4다.
  //    === 대신 >= 를 쓰면 "결과 0건(totalPages 0)"과 "필터로 페이지 수가 줄어
  //    page가 범위를 넘어선 경우"까지 같은 식 하나로 덮인다. (위 주석 ①②③ 참고)
  return page >= totalPages - 1
}
