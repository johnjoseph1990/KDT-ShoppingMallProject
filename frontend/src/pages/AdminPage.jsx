import { useEffect, useState } from 'react'
import { Badge, Button, Tabs, Table, Textarea, TextInput } from '@vapor-ui/core'
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
const STATUS_COLOR = {
  ORDERED: 'warning',
  PAID: 'success',
  SHIPPING: 'primary',
  DELIVERED: 'hint',
  CANCELED: 'danger',
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
      {/* Tabs.Panel은 기본적으로 비활성 탭의 내용을 DOM에서 제거(keepMounted=false)하므로,
          기존의 삼항 조건부 렌더링과 동일하게 선택된 탭의 패널만 마운트된다 */}
      <Tabs.Root value={tab} onValueChange={setTab}>
        <Tabs.List>
          <Tabs.Button value="products">상품 관리</Tabs.Button>
          <Tabs.Button value="orders">주문 관리</Tabs.Button>
        </Tabs.List>
        <Tabs.Panel value="products">
          <ProductManager />
        </Tabs.Panel>
        <Tabs.Panel value="orders">
          <OrderManager />
        </Tabs.Panel>
      </Tabs.Root>
    </div>
  )
}

// ─── 상품 관리 패널 ────────────────────────────────────────────
function ProductManager() {
  const [products, setProducts] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null) // null이면 신규 등록 모드
  const [page, setPage] = useState(0)

  useEffect(() => {
    loadProducts()
  }, [page])

  const loadProducts = () => {
    // 백엔드가 Page 객체로 응답하므로 실제 배열은 res.data.content에 들어있다
    getProducts({ page }).then((res) => setProducts(res.data.content))
  }

  // native input(가격/재고, type="number")용: 이벤트에서 값을 꺼냄
  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })
  // Vapor TextInput/Textarea(문자열 필드)용: 값이 바로 전달됨
  const setField = (field) => (value) => setForm({ ...form, [field]: value })

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
          <TextInput
            placeholder="상품명"
            value={form.name}
            onValueChange={setField('name')}
            required
          />
          {/* 가격/재고는 숫자 입력이라 Vapor TextInput(type="number" 미지원) 대신 native input 유지 */}
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
          <TextInput
            placeholder="이미지 URL"
            value={form.imageUrl}
            onValueChange={setField('imageUrl')}
          />
        </div>
        <Textarea
          placeholder="상품 설명"
          value={form.description}
          onValueChange={setField('description')}
          rows={2}
        />
        <TextInput
          placeholder="태그 (쉼표로 구분)"
          value={form.tags}
          onValueChange={setField('tags')}
        />
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Button type="submit" colorPalette="primary">
            {editingId ? '수정 완료' : '등록'}
          </Button>
          {editingId && (
            <Button type="button" onClick={handleCancel} variant="outline">
              취소
            </Button>
          )}
        </div>
      </form>

      {/* 상품 목록 테이블 */}
      <Table.Root style={styles.table}>
        <Table.Header>
          <Table.Row>
            <Table.Heading>ID</Table.Heading>
            <Table.Heading>상품명</Table.Heading>
            <Table.Heading>가격</Table.Heading>
            <Table.Heading>재고</Table.Heading>
            <Table.Heading>별점</Table.Heading>
            <Table.Heading>작업</Table.Heading>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {products.map((p) => (
            <Table.Row key={p.id}>
              <Table.Cell>{p.id}</Table.Cell>
              <Table.Cell>{p.name}</Table.Cell>
              <Table.Cell>{p.price.toLocaleString()}원</Table.Cell>
              <Table.Cell>{p.stockQuantity}</Table.Cell>
              <Table.Cell>{p.averageRating?.toFixed(1) ?? '-'}</Table.Cell>
              <Table.Cell>
                <Button onClick={() => handleEdit(p)} size="sm" style={{ marginRight: '0.3rem' }}>
                  수정
                </Button>
                <Button onClick={() => handleDelete(p.id)} size="sm" colorPalette="danger">
                  삭제
                </Button>
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
      {/* 페이지네이션 */}
      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', alignItems: 'center' }}>
        <Button
          variant="outline"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={page === 0}
        >
          이전
        </Button>
        <span>페이지 {page + 1}</span>
        <Button
          variant="outline"
          onClick={() => setPage((p) => p + 1)}
          disabled={products.length === 0}
        >
          다음
        </Button>
      </div>
    </>
  )
}

// ─── 주문 관리 패널 ────────────────────────────────────────────
function OrderManager() {
  const [orders, setOrders] = useState([])
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState(null) // null이면 전체 조회

  useEffect(() => {
    loadOrders()
  }, [page, statusFilter])

  const loadOrders = () => {
    // 백엔드가 Page 객체로 응답하므로 실제 배열은 res.data.content에 들어있다
    getAdminOrders(page, statusFilter).then((res) => setOrders(res.data.content))
  }

  // 필터 변경 시 첫 페이지로 리셋해서 이전 페이지 번호가 남는 부작용을 방지
  const handleFilterChange = (e) => {
    setStatusFilter(e.target.value || null)
    setPage(0)
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
      {/* 상태 필터 — 선택한 상태의 주문만 표시, 전체 선택 시 필터 없이 조회 */}
      <div style={{ marginBottom: '0.75rem' }}>
        <select value={statusFilter ?? ''} onChange={handleFilterChange} style={styles.select}>
          <option value="">전체</option>
          {ORDER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABEL[s]}
            </option>
          ))}
        </select>
      </div>
      <Table.Root style={styles.table}>
        <Table.Header>
          <Table.Row>
            <Table.Heading>ID</Table.Heading>
            <Table.Heading>총액</Table.Heading>
            <Table.Heading>주문일시</Table.Heading>
            <Table.Heading>상태</Table.Heading>
            <Table.Heading>변경</Table.Heading>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {orders.map((o) => (
            <Table.Row key={o.id}>
              <Table.Cell>{o.id}</Table.Cell>
              <Table.Cell>{o.totalPrice.toLocaleString()}원</Table.Cell>
              <Table.Cell style={{ fontSize: '0.8rem' }}>
                {new Date(o.createdAt).toLocaleString()}
              </Table.Cell>
              <Table.Cell>
                <Badge colorPalette={STATUS_COLOR[o.status] ?? 'hint'}>
                  {STATUS_LABEL[o.status] ?? o.status}
                </Badge>
              </Table.Cell>
              <Table.Cell>
                {/* select로 상태 직접 변경 (5개뿐인 단순 선택지라 native select 유지) */}
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
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
      {/* 페이지네이션 */}
      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', alignItems: 'center' }}>
        <Button
          variant="outline"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={page === 0}
        >
          이전
        </Button>
        <span>페이지 {page + 1}</span>
        <Button
          variant="outline"
          onClick={() => setPage((p) => p + 1)}
          disabled={orders.length === 0}
        >
          다음
        </Button>
      </div>
    </>
  )
}

const styles = {
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
  table: { width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' },
  select: { padding: '0.2rem', fontSize: '0.85rem' },
}
