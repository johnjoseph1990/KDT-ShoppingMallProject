import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import PrivateRoute from './components/PrivateRoute'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import ProductListPage from './pages/ProductListPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CartPage from './pages/CartPage'
import OrderListPage from './pages/OrderListPage'
import OrderDetailPage from './pages/OrderDetailPage'
import AdminPage from './pages/AdminPage'

export default function App() {
  return (
    // AuthProvider: 로그인 상태를 앱 전체에 공급
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <main style={{ padding: '1rem' }}>
          <Routes>
            {/* 공개 라우트 */}
            <Route path="/" element={<ProductListPage />} />
            <Route path="/products/:id" element={<ProductDetailPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />

            {/* 로그인 필요 */}
            <Route
              path="/cart"
              element={
                <PrivateRoute>
                  <CartPage />
                </PrivateRoute>
              }
            />
            <Route
              path="/orders"
              element={
                <PrivateRoute>
                  <OrderListPage />
                </PrivateRoute>
              }
            />
            <Route
              path="/orders/:id"
              element={
                <PrivateRoute>
                  <OrderDetailPage />
                </PrivateRoute>
              }
            />

            {/* 관리자 전용 */}
            <Route
              path="/admin"
              element={
                <PrivateRoute adminOnly>
                  <AdminPage />
                </PrivateRoute>
              }
            />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  )
}
