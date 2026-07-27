import { Component } from 'react'

/* 렌더 중 발생하는 JS 에러를 잡는 클래스 컴포넌트.
   React의 try/catch는 이벤트 핸들러 에러만 잡고, 렌더 에러는 잡지 못한다.
   ErrorBoundary만이 렌더 사이클 에러를 포착해 앱 전체 화이트스크린을 막을 수 있다. */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  /* 렌더 중 에러가 발생하면 React가 이 메서드를 호출해 state를 업데이트한다 */
  static getDerivedStateFromError() {
    return { hasError: true }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            gap: 20,
            minHeight: '60vh',
          }}
        >
          <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
            예기치 않은 오류가 발생했습니다.
          </p>
          <button
            onClick={() => this.setState({ hasError: false })}
            style={{
              cursor: 'pointer',
              border: '1px solid #333330',
              background: 'transparent',
              padding: '10px 22px',
              fontSize: 13,
              color: '#333330',
            }}
          >
            다시 시도
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
