// @testing-library/jest-dom: toBeInTheDocument(), toBeDisabled(), toHaveStyle() 같은
// DOM 전용 매처를 vitest의 expect에 추가한다.
// setupFiles에 등록해두면 모든 테스트 파일에서 import 없이 바로 쓸 수 있다.
import '@testing-library/jest-dom'
