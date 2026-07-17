import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import PrivateRoute from './components/PrivateRoute'
import Navbar from './components/Navbar'
import CartDrawer from './components/CartDrawer'
import Footer from './components/Footer'
import Toast from './components/Toast'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import ProductListPage from './pages/ProductListPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CartPage from './pages/CartPage'
import OrderListPage from './pages/OrderListPage'
import OrderDetailPage from './pages/OrderDetailPage'
import AdminPage from './pages/AdminPage'
import StoryPage from './pages/StoryPage'
import AboutPage from './pages/AboutPage'
import NotFoundPage from './pages/NotFoundPage'

export default function App() {
  return (
    /* AuthProvider → CartProvider → BrowserRouter 순으로 중첩
       CartProvider가 useAuth를 내부에서 사용하므로 AuthProvider 안에 있어야 한다 */
    <AuthProvider>
      <CartProvider>
        <BrowserRouter>
          <Navbar />
          {/* flex: 1 로 컨텐츠 영역이 늘어나 푸터가 항상 하단에 위치 */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/shop" element={<ProductListPage />} />
              <Route path="/products/:id" element={<ProductDetailPage />} />
              <Route path="/story" element={<StoryPage />} />
              <Route path="/about" element={<AboutPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />
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
              <Route
                path="/admin"
                element={
                  <PrivateRoute adminOnly>
                    <AdminPage />
                  </PrivateRoute>
                }
              />
              {/* path="*": 위 어떤 라우트와도 일치하지 않는 나머지 모든 경로 — 항상 마지막에 둔다 */}
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </div>
          <Footer />
          <CartDrawer />
          <Toast />
        </BrowserRouter>
      </CartProvider>
    </AuthProvider>
  )
}
