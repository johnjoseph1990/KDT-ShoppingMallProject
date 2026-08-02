import { useEffect, useState } from 'react'
import { Badge, Button, Tabs, Table, Textarea, TextInput } from '@vapor-ui/core'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api/products'
import { getAdminOrders, updateOrderStatus, uploadImage } from '../api/admin'
// 이미지 업로드 검증 규칙과 에러 문구는 utils/imageUpload 한 곳에서만 관리한다
import { validateImageFile, uploadErrorMessage } from '../utils/imageUpload'
// 주문 상태 표기는 utils/orderStatus 한 곳에서만 관리한다.
// (예전엔 이 파일에 라벨을 직접 복사해뒀다가 WAITING_FOR_DEPOSIT를 빠뜨려
//  입금대기 주문이 "주문완료"로 잘못 표시되는 버그가 있었다)
import { ADMIN_FILTER_STATUSES, orderStatusLabel, adminStatusOptions } from '../utils/orderStatus'

// 배지 색상은 이 화면 전용 표현이라 여기 남긴다 (라벨과 달리 다른 화면과 공유하지 않음)
const STATUS_COLOR = {
  ORDERED: 'warning',
  WAITING_FOR_DEPOSIT: 'warning',
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
  // 업로드 진행 중 여부 — true인 동안 파일 입력과 저장 버튼을 잠가서
  // URL이 아직 안 채워진 상태로 상품이 저장되는 것을 막는다
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('') // 빈 문자열이면 에러 없음
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

  // 파일 선택 → 검증 → 서버 업로드 → 받은 공개 URL을 imageUrl 칸에 자동 입력.
  // (지금까지는 백엔드/Blob이 다 동작하는데도 화면에 파일 선택 UI가 없어서
  //  uploadImage()가 아무 데서도 호출되지 않는 죽은 코드였다 — 2026-08-02 DEF-5)
  const handleFileChange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return // 사용자가 파일 선택 창을 그냥 닫은 경우

    // 같은 파일을 다시 고를 수 있게 input 값을 비운다.
    // (비우지 않으면 값이 안 바뀌어 onChange 자체가 안 걸린다 — 업로드 실패 후 재시도가 막힘)
    e.target.value = ''

    const error = validateImageFile(file)
    if (error) {
      setUploadError(error)
      return
    }

    setUploadError('')
    setUploading(true)
    try {
      const res = await uploadImage(file)
      // await 뒤의 setForm은 함수형으로 쓴다. 업로드를 기다리는 동안 사용자가
      // 다른 칸(상품명 등)을 고쳤을 수 있는데, {...form}을 쓰면 오래된 스냅샷으로
      // 덮어써서 그 입력이 사라진다.
      setForm((prev) => ({ ...prev, imageUrl: res.data.url }))
    } catch (err) {
      setUploadError(uploadErrorMessage(err))
    } finally {
      // 성공이든 실패든 잠금은 반드시 풀어야 하므로 finally에 둔다
      setUploading(false)
    }
  }

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
      setUploadError('') // 폼을 비웠으니 이전 업로드 에러 문구도 같이 지운다
      loadProducts()
    } catch (err) {
      alert(err.response?.data?.message || '저장 실패')
    }
  }

  const handleEdit = (product) => {
    // 폼 내용을 통째로 갈아끼우는 곳에서는 에러 문구도 함께 초기화한다.
    // 안 그러면 A 상품에서 난 업로드 에러가, B 상품 "수정"을 눌러 폼이 바뀐 뒤에도
    // 그대로 남아 지금 상품의 문제인 것처럼 보인다.
    // (폼 상태를 교체하는 진입점은 handleSubmit·handleCancel·handleEdit 셋뿐이고,
    //  앞의 둘은 이미 같은 처리를 하고 있다)
    setUploadError('')
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
    setUploadError('')
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

        {/* 이미지 업로드 — 파일을 고르면 Azure Blob에 올리고 받은 공개 URL이
            위 "이미지 URL" 칸에 자동으로 채워진다. URL 직접 입력도 그대로 가능하다. */}
        <div style={styles.uploadRow}>
          {/* htmlFor+id로 label과 input을 연결해야 label 클릭·스크린리더 읽기가 동작한다 */}
          <label htmlFor="product-image-file" style={styles.uploadLabel}>
            이미지 파일 업로드
          </label>
          <input
            id="product-image-file"
            type="file"
            // accept: 파일 선택 창에서 이미지만 보이게 하는 "편의" 기능일 뿐,
            // 사용자가 '모든 파일'로 바꿔 고를 수 있으므로 진짜 검증은 validateImageFile이 한다
            accept="image/*"
            onChange={handleFileChange}
            disabled={uploading}
            style={styles.fileInput}
          />
          {uploading && <span style={styles.uploadingText}>업로드 중…</span>}
        </div>

        {/* 에러 문구 — role="alert"를 주면 스크린리더가 나타나는 즉시 읽어준다 */}
        {uploadError && (
          <p role="alert" style={styles.uploadError}>
            {uploadError}
          </p>
        )}

        {/* 미리보기 — URL을 직접 입력한 경우에도 그대로 보이므로 오타를 바로 알아챌 수 있다 */}
        {form.imageUrl && (
          <img src={form.imageUrl} alt="상품 이미지 미리보기" style={styles.preview} />
        )}
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
          {/* 업로드 중 저장을 막는다 — 안 막으면 URL이 채워지기 전에 상품이 저장돼
              이미지 없는 상품이 만들어진다 */}
          <Button type="submit" colorPalette="primary" disabled={uploading}>
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
          {/* 필터는 조회 조건일 뿐이라 입금대기도 골라볼 수 있어야 한다 */}
          {ADMIN_FILTER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {orderStatusLabel(s)}
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
                  {orderStatusLabel(o.status)}
                </Badge>
              </Table.Cell>
              <Table.Cell>
                {/* select로 상태 직접 변경 (단순 선택지라 native select 유지).
                    adminStatusOptions는 현재 상태가 수동 지정 불가한 값(입금대기)이어도
                    목록에 포함시킨다 — 매칭되는 option이 없으면 브라우저가 첫 항목을
                    선택된 것처럼 보여줘서 실제 상태를 오인하게 되기 때문이다. */}
                <select
                  value={o.status}
                  onChange={(e) => handleStatusChange(o.id, e.target.value)}
                  style={styles.select}
                >
                  {adminStatusOptions(o.status).map((opt) => (
                    <option key={opt.value} value={opt.value} disabled={opt.disabled}>
                      {opt.label}
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
  uploadRow: { display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' },
  uploadLabel: { fontSize: '0.85rem', color: '#555' },
  fileInput: { fontSize: '0.85rem' },
  uploadingText: { fontSize: '0.85rem', color: '#666' },
  uploadError: { margin: 0, fontSize: '0.85rem', color: '#c0392b' },
  // objectFit: 'contain' — 비율을 유지한 채 상자 안에 맞춘다(잘리지 않음)
  preview: {
    width: '120px',
    height: '120px',
    objectFit: 'contain',
    border: '1px solid #ddd',
    borderRadius: '4px',
  },
}
