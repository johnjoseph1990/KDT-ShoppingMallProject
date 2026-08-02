// 관리자 상품 이미지 업로드의 "정책"을 모아둔 모듈.
//
// 왜 컴포넌트(AdminPage)에서 분리했나:
//  1) 검증 규칙과 에러 문구는 화면이 아니라 "규칙"이다. 규칙이 컴포넌트 안에 있으면
//     테스트하려고 브라우저·렌더링을 통째로 띄워야 한다. 순수 함수로 빼두면
//     vitest에서 함수만 호출해 바로 검증할 수 있다.
//  2) DEF-6(주문 상태 라벨이 4개 파일에 복사돼 있던 사고)과 같은 교훈 —
//     같은 지식이 여러 곳에 흩어지면 하나를 고칠 때 나머지를 잊는다.
//
// 백엔드 대응: POST /api/admin/upload (UploadController) → Azure Blob → { url: "https://..." }

// 업로드 허용 최대 크기 (바이트).
// 반드시 백엔드 application.properties의 spring.servlet.multipart.max-file-size와 같은 값이어야 한다.
// 프론트가 더 관대하면 사용자가 업로드를 다 기다린 뒤 서버에서 413으로 거절당하고,
// 더 엄격하면 서버가 받을 수 있는 파일을 괜히 막게 된다.
export const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB

// 허용할 이미지 MIME 타입.
// File 객체의 type은 브라우저가 확장자·매직넘버를 보고 채워준다(비어 있을 수도 있음).
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

/**
 * 선택한 파일이 업로드 가능한 이미지인지 검사한다.
 *
 * 계약(contract) — AdminPage와 테스트가 이 규칙에 의존한다:
 *  - 업로드해도 되는 파일이면 `null`을 반환한다
 *  - 문제가 있으면 **사용자에게 그대로 보여줄 한국어 문구(string)**를 반환한다
 *
 * 즉 "예외를 던지지 않고 에러 메시지를 값으로 돌려주는" 방식이다.
 * 호출부가 try/catch 없이 `const error = validateImageFile(file)` 한 줄로 분기할 수 있다.
 *
 * @param {File | null | undefined} file 사용자가 <input type="file">에서 고른 파일
 * @returns {string | null} 에러 메시지, 문제없으면 null
 */
export const validateImageFile = (file) => {
  // 호출부(AdminPage)가 이미 걸러주지만, 이 함수만 따로 쓰는 곳이 생겨도
  // 안전하도록 방어한다. 순수 함수는 "누가 어떻게 부르든 터지지 않는" 게 좋다.
  if (!file) return '파일을 선택해주세요.'

  // ① 타입을 크기보다 먼저 본다.
  //    PDF를 골랐다면 그건 "너무 큰 파일"이 아니라 "애초에 이미지가 아닌 파일"이다.
  //    사용자가 파일을 줄여볼 필요 없이 다른 파일을 고르면 된다는 걸 바로 알려주는 게 낫다.
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    // file.type이 빈 문자열인 경우(브라우저가 확장자로 종류를 못 알아낸 파일)도
    // 여기서 함께 걸린다. 통과시켜 서버에 맡길 수도 있지만, 서버 응답을 기다렸다
    // 실패하는 것보다 즉시 알려주는 편이 낫다고 판단했다.
    return '이미지 파일만 업로드할 수 있습니다. (JPG, PNG, WebP, GIF)'
  }

  // ② 크기 검사 — 한계값과 실제값을 함께 알려준다.
  //    "파일이 너무 큽니다"만 있으면 얼마나 줄여야 하는지 알 수 없다.
  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return `${formatMegabytes(MAX_IMAGE_SIZE_BYTES)}MB 이하만 업로드할 수 있습니다. (선택한 파일: ${formatMegabytes(file.size)}MB)`
  }

  return null // 문제 없음
}

// 바이트를 MB 문자열로 바꾼다 (소수점 1자리).
// toFixed(1)은 문자열을 돌려주므로 '10.0'처럼 남는 .0을 Number로 한 번 걷어낸다.
const formatMegabytes = (bytes) => Number((bytes / 1024 / 1024).toFixed(1))

/**
 * 업로드 API 호출이 실패했을 때, axios 에러를 사용자용 한국어 문구로 바꾼다.
 *
 * 상태코드별로 나누는 이유: 원인마다 사용자가 취할 행동이 다르다.
 * "실패했습니다" 한 줄로 뭉뚱그리면 관리자가 파일을 바꿔봐야 하는지,
 * 개발자를 불러야 하는지 알 수 없다. (DEF-1에서 결제가 "취소됨" 한 마디로만
 * 보이는 바람에 원인 파악이 늦어졌던 것과 같은 문제)
 *
 * @param {unknown} err axios가 throw한 에러 객체
 * @returns {string} 사용자에게 보여줄 문구
 */
export const uploadErrorMessage = (err) => {
  const status = err?.response?.status

  switch (status) {
    case 503:
      // UploadController가 AzureBlobService 빈 없이 기동된 상태
      // (AZURE_STORAGE_CONNECTION_STRING 환경변수 미주입)
      return '이미지 업로드 기능이 서버에 구성되지 않았습니다. 관리자에게 문의하세요.'
    case 403:
      return '업로드 권한이 없습니다. 관리자 계정으로 로그인했는지 확인하세요.'
    case 413:
      // 프론트 검증을 통과했는데 413이면 프론트/백엔드 크기 제한이 어긋난 것이다
      return '파일이 너무 커서 서버가 거부했습니다.'
    default:
      // 서버가 내려준 메시지가 있으면 우선 사용 (백엔드 GlobalExceptionHandler 형식: { message })
      return err?.response?.data?.message || '이미지 업로드에 실패했습니다.'
  }
}
