import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// Vapor UI 컴포넌트가 참조하는 디자인 토큰(색상/간격 등) CSS. 앱 전체에 한 번만 불러오면 됨
import '@vapor-ui/core/styles.css'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
