import { useEffect, useState } from 'react'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api/products'
import { getAdminOrders, updateOrderStatus } from '../api/admin'

// 관리자가 선택 가능한 주문 상태 목록
const ORDER_STATUSES = ['ORDERED', 'PAID', 'SHIPPING', 'DELIVERED', 'CANCELED']
const STATUS_LABEL = {
  ORDERED: '주문완료',
  PAID: '결제완료',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소됨',
}

const EMPTY_FORM = {
  name: '',
  description: '',
  price: '',
  stockQuantity: '',
  imageUrl: '',
  tags: '',
}

export default function AdminPage() {
  // tab: 'products' | 'orders'
  const [tab, setTab] = useState('products')

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <h2>관리자 페이지</h2>
      <div style={styles.tabRow}>
        <button onClick={() => setTab('products')} style={tabStyle(tab === 'products')}>
          상품 관리
        </button>
        <button onClick={() => setTab('orders')} style={tabStyle(tab === 'orders')}>
          주문 관리
        </button>
      </div>
      {tab === 'products' ? <ProductManager /> : <OrderManager />}
    </div>
  )
}

// ─── 상품 관리 패널 ────────────────────────────────────────────
function ProductManager() {
  const [products, setProducts] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null) // null이면 신규 등록 모드

  useEffect(() => {
    loadProducts()
  }, [])

  const loadProducts = () => {
    getProducts().then((res) => setProducts(res.data))
  }

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    // tags: 쉼표 구분 문자열 → 배열로 변환
    const payload = {
      ...form,
      price: Number(form.price),
      stockQuantity: Number(form.stockQuantity),
      tags: form.tags ? form.tags.split(',').map((t) => t.trim()) : [],
    }
    try {
      if (editingId) {
        await updateProduct(editingId, payload)
      } else {
        await createProduct(payload)
      }
      setForm(EMPTY_FORM)
      setEditingId(null)
      loadProducts()
    } catch (err) {
      alert(err.response?.data?.message || '저장 실패')
    }
  }

  const handleEdit = (product) => {
    setEditingId(product.id)
    setForm({
      name: product.name,
      description: product.description ?? '',
      price: product.price,
      stockQuantity: product.stockQuantity,
      imageUrl: product.imageUrl ?? '',
      tags: product.tags?.join(', ') ?? '',
    })
  }

  const handleDelete = async (id) => {
    if (!confirm('상품을 삭제하시겠습니까?')) return
    try {
      await deleteProduct(id)
      loadProducts()
    } catch {
      alert('삭제 실패')
    }
  }

  const handleCancel = () => {
    setForm(EMPTY_FORM)
    setEditingId(null)
  }

  return (
    <>
      {/* 상품 등록/수정 폼 */}
      <form onSubmit={handleSubmit} style={styles.form}>
        <h3>{editingId ? '상품 수정' : '상품 등록'}</h3>
        <div style={styles.formGrid}>
          <input
            name="name"
            placeholder="상품명"
            value={form.name}
            onChange={handleChange}
            style={styles.input}
            required
          />
          <input
            name="price"
            placeholder="가격"
            type="number"
            value={form.price}
            onChange={handleChange}
            style={styles.input}
            required
          />
          <input
            name="stockQuantity"
            placeholder="재고 수량"
            type="number"
            value={form.stockQuantity}
            onChange={handleChange}
            style={styles.input}
            required
          />
          <input
            name="imageUrl"
            placeholder="이미지 URL"
            value={form.imageUrl}
            onChange={handleChange}
            style={styles.input}
          />
        </div>
        <textarea
          name="description"
          placeholder="상품 설명"
          value={form.description}
          onChange={handleChange}
          rows={2}
          style={{ ...styles.input, width: '100%' }}
        />
        <input
          name="tags"
          placeholder="태그 (쉼표로 구분)"
          value={form.tags}
          onChange={handleChange}
          style={{ ...styles.input, width: '100%' }}
        />
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="submit" style={styles.saveBtn}>
            {editingId ? '수정 완료' : '등록'}
          </button>
          {editingId && (
            <button type="button" onClick={handleCancel} style={styles.cancelBtn}>
              취소
            </button>
          )}
        </div>
      </form>

      {/* 상품 목록 테이블 */}
      <table style={styles.table}>
        <thead>
          <tr>
            <th>ID</th>
            <th>상품명</th>
            <th>가격</th>
            <th>재고</th>
            <th>별점</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.name}</td>
              <td>{p.price.toLocaleString()}원</td>
              <td>{p.stockQuantity}</td>
              <td>{p.averageRating?.toFixed(1) ?? '-'}</td>
              <td>
                <button onClick={() => handleEdit(p)} style={styles.editBtn}>
                  수정
                </button>
                <button onClick={() => handleDelete(p.id)} style={styles.deleteBtn}>
                  삭제
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}

// ─── 주문 관리 패널 ────────────────────────────────────────────
function OrderManager() {
  const [orders, setOrders] = useState([])
  const [page, setPage] = useState(0)

  useEffect(() => {
    loadOrders()
  }, [page])

  const loadOrders = () => {
    getAdminOrders(page).then((res) => setOrders(res.data))
  }

  const handleStatusChange = async (orderId, newStatus) => {
    try {
      await updateOrderStatus(orderId, newStatus)
      loadOrders()
    } catch (err) {
      alert(err.response?.data?.message || '상태 변경 실패')
    }
  }

  return (
    <>
      <table style={styles.table}>
        <thead>
          <tr>
            <th>ID</th>
            <th>총액</th>
            <th>주문일시</th>
            <th>상태</th>
            <th>변경</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td>{o.id}</td>
              <td>{o.totalPrice.toLocaleString()}원</td>
              <td style={{ fontSize: '0.8rem' }}>{new Date(o.createdAt).toLocaleString()}</td>
              <td>{STATUS_LABEL[o.status] ?? o.status}</td>
              <td>
                {/* select로 상태 직접 변경 */}
                <select
                  value={o.status}
                  onChange={(e) => handleStatusChange(o.id, e.target.value)}
                  style={styles.select}
                >
                  {ORDER_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {STATUS_LABEL[s]}
                    </option>
                  ))}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {/* 페이지네이션 */}
      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
        <button
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={page === 0}
          style={styles.pageBtn}
        >
          이전
        </button>
        <span>페이지 {page + 1}</span>
        <button
          onClick={() => setPage((p) => p + 1)}
          disabled={orders.length === 0}
          style={styles.pageBtn}
        >
          다음
        </button>
      </div>
    </>
  )
}

function tabStyle(active) {
  return {
    padding: '0.5rem 1.5rem',
    background: active ? '#1a1a2e' : '#eee',
    color: active ? '#fff' : '#333',
    border: 'none',
    cursor: 'pointer',
    borderRadius: '4px 4px 0 0',
  }
}

const styles = {
  tabRow: { display: 'flex', gap: '0.25rem', marginBottom: '1rem' },
  form: {
    border: '1px solid #ddd',
    borderRadius: '8px',
    padding: '1rem',
    marginBottom: '1.5rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
  },
  formGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' },
  input: { padding: '0.5rem', borderRadius: '4px', border: '1px solid #ccc', fontSize: '0.95rem' },
  saveBtn: {
    padding: '0.5rem 1.2rem',
    background: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  cancelBtn: {
    padding: '0.5rem 1.2rem',
    background: '#ccc',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  editBtn: {
    marginRight: '0.3rem',
    padding: '0.2rem 0.5rem',
    background: '#457b9d',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  deleteBtn: {
    padding: '0.2rem 0.5rem',
    background: '#e63946',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  select: { padding: '0.2rem', fontSize: '0.85rem' },
  pageBtn: {
    padding: '0.4rem 0.8rem',
    background: '#eee',
    border: '1px solid #ccc',
    borderRadius: '4px',
    cursor: 'pointer',
  },
}
