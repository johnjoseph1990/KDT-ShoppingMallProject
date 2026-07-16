import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
/* AdminPage가 Vapor UI 컴포넌트를 사용하므로 스타일 유지 */
import '@vapor-ui/core/styles.css'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
