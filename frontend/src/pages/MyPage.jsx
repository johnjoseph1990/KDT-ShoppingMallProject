import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { updateMe, deleteMe, logout as logoutApi } from '../api/auth'
import { getOrders } from '../api/orders'
import { getAddresses, createAddress, updateAddress, deleteAddress } from '../api/addresses'
// 주문 상태 한글 표기는 utils/orderStatus 한 곳에서 관리한다
import { orderStatusLabel } from '../utils/orderStatus'

// 상태별 색상 정의 (색상은 이 화면 전용 표현이라 라벨과 달리 여기 남긴다)
const STATUS_STYLE = {
  ORDERED: { color: 'var(--color-fg-muted)', bg: '#f0eedf' },
  // 입금대기는 "아직 결제 안 됨"이므로 주문완료와 같은 톤으로 표시한다
  WAITING_FOR_DEPOSIT: { color: 'var(--color-fg-muted)', bg: '#f0eedf' },
  PAID: { color: 'var(--color-success)', bg: 'var(--color-success-bg)' },
  SHIPPING: { color: 'var(--color-shipping)', bg: 'var(--color-shipping-bg)' },
  DELIVERED: { color: 'var(--color-fg)', bg: 'var(--color-text-on-dark)' },
  CANCELED: { color: '#aaa', bg: '#f4f4f4' },
}

const fmt = (n) => Number(n).toLocaleString('ko-KR') + '원'

// 폼 입력 공통 스타일
const inputStyle = {
  border: '1px solid var(--color-border)',
  background: 'transparent',
  padding: '12px 14px',
  fontSize: 14,
  outline: 'none',
  width: '100%',
  fontFamily: "'Noto Sans KR', sans-serif",
  fontWeight: 300,
}

export default function MyPage() {
  const { user, login: setUser, logout } = useAuth()
  const navigate = useNavigate()
  const [tab, setTab] = useState('orders') // 'orders' | 'addresses' | 'settings'
  const [showDeleteModal, setShowDeleteModal] = useState(false)

  // 이 페이지 전용 토스트 (CartContext의 Toast와 별개로 동작)
  const [notice, setNotice] = useState(null) // { msg, error }
  const noticeTimer = useRef(null)
  const showNotice = (msg, error = false) => {
    setNotice({ msg, error })
    clearTimeout(noticeTimer.current)
    noticeTimer.current = setTimeout(() => setNotice(null), 3000)
  }

  // 탈퇴 처리: 서버 삭제 → 세션 만료 → 로컬 상태 제거 → 홈 이동
  const handleDelete = async () => {
    try {
      await deleteMe()
    } catch {
      // 서버 삭제 실패 시 탈퇴를 중단하고 에러를 사용자에게 알린다.
      // 성공한 척 로그아웃하면 계정이 서버에 남은 채 사용자가 인지하지 못하는 불일치 상태가 된다.
      showNotice('회원 탈퇴에 실패했습니다. 잠시 후 다시 시도해주세요.', true)
      setShowDeleteModal(false)
      return
    }
    try {
      await logoutApi()
    } catch {}
    logout()
    navigate('/')
  }

  return (
    <main style={{ animation: 'fadeUp .4s ease both', flex: 1 }}>
      <div className="mypage-layout">
        {/* ── 사이드바 ───────────────────────────────────── */}
        <aside className="mypage-sidebar">
          {/* 사용자 정보 */}
          <div style={{ marginBottom: 36 }}>
            {/* 이름 첫 글자를 이니셜 아바타로 표시 */}
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: '50%',
                background: 'var(--color-text-on-dark)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: "'Noto Serif KR', serif",
                fontSize: 17,
                color: 'var(--color-fg)',
                marginBottom: 14,
                userSelect: 'none',
              }}
            >
              {user?.name?.[0]}
            </div>
            <p style={{ fontSize: 15, fontWeight: 400, marginBottom: 3 }}>{user?.name}</p>
            <p style={{ fontSize: 12, color: 'var(--color-fg-muted)' }}>{user?.email}</p>
          </div>

          {/* 내비게이션 탭 */}
          <nav style={{ display: 'flex', flexDirection: 'column' }}>
            <SidebarItem active={tab === 'orders'} onClick={() => setTab('orders')}>
              주문 내역
            </SidebarItem>
            <SidebarItem active={tab === 'addresses'} onClick={() => setTab('addresses')}>
              배송지 관리
            </SidebarItem>
            <SidebarItem active={tab === 'settings'} onClick={() => setTab('settings')}>
              계정 설정
            </SidebarItem>
          </nav>

          {/* 회원 탈퇴 — 시각적 위계를 낮게 유지해 실수 클릭 방지 */}
          <div style={{ marginTop: 'auto', paddingTop: 48 }}>
            <button
              onClick={() => setShowDeleteModal(true)}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                padding: 0,
                fontSize: 12,
                color: '#bbb',
                fontFamily: "'Noto Sans KR', sans-serif",
              }}
              onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-danger)')}
              onMouseLeave={(e) => (e.currentTarget.style.color = '#bbb')}
            >
              회원 탈퇴
            </button>
          </div>
        </aside>

        {/* ── 콘텐츠 영역 ────────────────────────────────── */}
        <div style={{ flex: 1, minWidth: 0, padding: 'clamp(32px,4vw,56px) clamp(24px,4vw,56px)' }}>
          {tab === 'orders' && <OrdersPanel />}
          {tab === 'addresses' && <AddressesPanel showNotice={showNotice} />}
          {tab === 'settings' && (
            <SettingsPanel user={user} setUser={setUser} showNotice={showNotice} />
          )}
        </div>
      </div>

      {/* 성공/에러 토스트 알림 */}
      {notice && (
        <div
          style={{
            position: 'fixed',
            bottom: 28,
            left: '50%',
            transform: 'translateX(-50%)',
            background: notice.error ? '#c0392b' : 'var(--color-bg-dark)',
            color: 'var(--color-text-on-dark)',
            padding: '13px 26px',
            fontSize: 13,
            letterSpacing: '0.03em',
            zIndex: 200,
            animation: 'fadeUp .3s ease both',
            whiteSpace: 'nowrap',
          }}
        >
          {notice.msg}
        </div>
      )}

      {/* 회원 탈퇴 확인 모달 */}
      {showDeleteModal && (
        <DeleteModal onClose={() => setShowDeleteModal(false)} onConfirm={handleDelete} />
      )}
    </main>
  )
}

// ── 사이드바 내비게이션 항목 ──────────────────────────────
// active 상태일 때 왼쪽 테두리 + 색상으로 현재 위치를 표시한다
function SidebarItem({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      style={{
        background: 'none',
        border: 'none',
        borderLeft: active ? '2px solid var(--color-fg)' : '2px solid transparent',
        padding: '12px 0 12px 16px',
        fontSize: 14,
        color: active ? 'var(--color-fg)' : 'var(--color-fg-muted)',
        fontWeight: active ? 400 : 300,
        fontFamily: "'Noto Sans KR', sans-serif",
        cursor: 'pointer',
        textAlign: 'left',
        letterSpacing: '0.02em',
      }}
      onMouseEnter={(e) => {
        if (!active) e.currentTarget.style.color = 'var(--color-fg)'
      }}
      onMouseLeave={(e) => {
        if (!active) e.currentTarget.style.color = 'var(--color-fg-muted)'
      }}
    >
      {children}
    </button>
  )
}

// ── 주문 내역 패널 ────────────────────────────────────────
function OrdersPanel() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getOrders()
      .then((res) => setOrders(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  // 상태별 주문 수 집계
  const counts = orders.reduce((acc, o) => {
    acc[o.status] = (acc[o.status] || 0) + 1
    return acc
  }, {})

  return (
    <div>
      <h2
        style={{
          fontFamily: "'Noto Serif KR', serif",
          fontWeight: 300,
          fontSize: 24,
          marginBottom: 28,
        }}
      >
        주문 내역
      </h2>

      {/* 배송 단계별 주문 수 요약 — 사용자가 가장 자주 확인하는 정보 */}
      {!loading && orders.length > 0 && (
        <div
          style={{
            display: 'flex',
            border: '1px solid var(--color-border)',
            marginBottom: 36,
          }}
        >
          {[
            { key: 'PAID', label: '결제완료' },
            { key: 'SHIPPING', label: '배송중' },
            { key: 'DELIVERED', label: '배송완료' },
          ].map(({ key, label }, i, arr) => (
            <div
              key={key}
              style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '20px 12px',
                gap: 8,
                borderRight: i < arr.length - 1 ? '1px solid var(--color-border)' : 'none',
              }}
            >
              <span
                style={{
                  fontFamily: "'Noto Serif KR', serif",
                  fontSize: 28,
                  fontWeight: 300,
                  lineHeight: 1,
                }}
              >
                {counts[key] || 0}
              </span>
              <span
                style={{ fontSize: 11, color: 'var(--color-fg-accent)', letterSpacing: '0.06em' }}
              >
                {label}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* 주문 목록 */}
      {loading ? (
        <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
          불러오는 중...
        </p>
      ) : orders.length === 0 ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
            아직 주문 내역이 없습니다.
          </p>
          <Link
            to="/shop"
            style={{
              fontSize: 13,
              borderBottom: '1px solid var(--color-fg)',
              paddingBottom: 1,
              display: 'inline-block',
            }}
          >
            쇼핑하러 가기
          </Link>
        </div>
      ) : (
        <div style={{ borderTop: '1px solid var(--color-border)' }}>
          {orders.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '20px 0',
                borderBottom: '1px solid var(--color-border)',
              }}
              onMouseEnter={(e) =>
                (e.currentTarget.style.background = 'var(--color-bg-hover-light)')
              }
              onMouseLeave={(e) => (e.currentTarget.style.background = '')}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <span style={{ fontSize: 14, fontFamily: "'Noto Serif KR', serif" }}>
                  주문 #{order.id}
                </span>
                <span style={{ fontSize: 12, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
                  {new Date(order.createdAt).toLocaleDateString('ko-KR', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </span>
              </div>
              <div
                style={{ display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-end' }}
              >
                <span style={{ fontSize: 14 }}>{fmt(order.totalPrice)}</span>
                <StatusBadge status={order.status} />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

// 주문 상태를 컬러 뱃지로 표시
function StatusBadge({ status }) {
  const { color, bg } = STATUS_STYLE[status] ?? STATUS_STYLE.ORDERED
  return (
    <span
      style={{ fontSize: 11, letterSpacing: '0.04em', color, background: bg, padding: '3px 8px' }}
    >
      {orderStatusLabel(status)}
    </span>
  )
}

// ── 계정 설정 패널 ────────────────────────────────────────
function SettingsPanel({ user, setUser, showNotice }) {
  // 현재 열려 있는 설정 섹션 — 한 번에 하나만 열리게 한다
  const [openSection, setOpenSection] = useState(null) // 'name' | 'password'

  const toggle = (section) => setOpenSection((prev) => (prev === section ? null : section))

  return (
    <div>
      <h2
        style={{
          fontFamily: "'Noto Serif KR', serif",
          fontWeight: 300,
          fontSize: 24,
          marginBottom: 28,
        }}
      >
        계정 설정
      </h2>

      <div style={{ borderTop: '1px solid var(--color-border)' }}>
        {/* 이름 수정 행 */}
        <SettingRow
          label="이름"
          value={user?.name}
          open={openSection === 'name'}
          onToggle={() => toggle('name')}
        >
          <NameForm
            user={user}
            setUser={setUser}
            showNotice={showNotice}
            onDone={() => setOpenSection(null)}
          />
        </SettingRow>

        {/* 비밀번호 변경 행 */}
        <SettingRow
          label="비밀번호"
          value="••••••••"
          open={openSection === 'password'}
          onToggle={() => toggle('password')}
        >
          <PasswordForm showNotice={showNotice} onDone={() => setOpenSection(null)} />
        </SettingRow>
      </div>
    </div>
  )
}

// ── 설정 행 (disclosure 패턴) ─────────────────────────────
// 헤더를 클릭하면 하위 폼이 펼쳐지고, 다시 클릭하면 접힌다
function SettingRow({ label, value, open, onToggle, children }) {
  return (
    <div style={{ borderBottom: '1px solid var(--color-border)' }}>
      <div
        onClick={onToggle}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '20px 0',
          cursor: 'pointer',
          userSelect: 'none',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.background = '#fafaf0')}
        onMouseLeave={(e) => (e.currentTarget.style.background = '')}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 28 }}>
          {/* 라벨: 소문자 대신 대문자+자간으로 "설정 항목명" 느낌을 줌 */}
          <span
            style={{
              fontSize: 11,
              color: 'var(--color-fg-accent)',
              letterSpacing: '0.08em',
              minWidth: 64,
              textTransform: 'uppercase',
            }}
          >
            {label}
          </span>
          <span style={{ fontSize: 14, fontWeight: 300 }}>{value}</span>
        </div>
        <span style={{ fontSize: 12, color: 'var(--color-fg-muted)', letterSpacing: '0.04em' }}>
          {open ? '닫기' : '수정'}
        </span>
      </div>

      {/* 폼 영역: open일 때만 렌더링 */}
      {open && <div style={{ paddingBottom: 28 }}>{children}</div>}
    </div>
  )
}

// ── 이름 변경 폼 ──────────────────────────────────────────
function NameForm({ user, setUser, showNotice, onDone }) {
  const [name, setName] = useState(user?.name ?? '')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    // 값이 같으면 API 호출 없이 닫기만 한다
    if (!name.trim() || name.trim() === user?.name) {
      onDone()
      return
    }
    setLoading(true)
    try {
      const res = await updateMe({ name: name.trim() })
      setUser(res.data)
      showNotice('이름이 변경되었습니다.')
      onDone()
    } catch {
      showNotice('이름 변경에 실패했습니다.', true)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
      <input
        type="text"
        required
        value={name}
        autoFocus
        onChange={(e) => setName(e.target.value)}
        style={{ ...inputStyle, flex: 1 }}
      />
      <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
        <SmallBtn type="button" onClick={onDone} outline>
          취소
        </SmallBtn>
        <SmallBtn type="submit" disabled={loading}>
          {loading ? '저장 중' : '저장'}
        </SmallBtn>
      </div>
    </form>
  )
}

// ── 비밀번호 변경 폼 ──────────────────────────────────────
function PasswordForm({ showNotice, onDone }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.newPassword !== form.confirm) {
      showNotice('새 비밀번호가 일치하지 않습니다.', true)
      return
    }
    setLoading(true)
    try {
      await updateMe({ currentPassword: form.currentPassword, newPassword: form.newPassword })
      showNotice('비밀번호가 변경되었습니다.')
      onDone()
    } catch (err) {
      const msg = err.response?.data?.message
      showNotice(msg || '현재 비밀번호를 확인해 주세요.', true)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <input
        type="password"
        placeholder="현재 비밀번호"
        required
        autoFocus
        value={form.currentPassword}
        onChange={(e) => setForm({ ...form, currentPassword: e.target.value })}
        style={inputStyle}
      />
      <input
        type="password"
        placeholder="새 비밀번호 (8자 이상)"
        required
        value={form.newPassword}
        onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
        style={inputStyle}
      />
      <input
        type="password"
        placeholder="새 비밀번호 확인"
        required
        value={form.confirm}
        onChange={(e) => setForm({ ...form, confirm: e.target.value })}
        style={inputStyle}
      />
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <SmallBtn type="button" onClick={onDone} outline>
          취소
        </SmallBtn>
        <SmallBtn type="submit" disabled={loading}>
          {loading ? '저장 중' : '저장'}
        </SmallBtn>
      </div>
    </form>
  )
}

// ── 소형 버튼 (설정 폼 내 저장/취소) ─────────────────────
// outline prop이 있으면 테두리만 있는 보조 버튼, 없으면 채워진 주 버튼
function SmallBtn({ children, outline, ...props }) {
  return (
    <button
      {...props}
      style={{
        cursor: 'pointer',
        border: '1px solid var(--color-fg)',
        background: outline ? 'transparent' : 'var(--color-fg)',
        color: outline ? 'var(--color-fg)' : 'var(--color-bg)',
        padding: '10px 18px',
        fontSize: 13,
        letterSpacing: '0.04em',
        fontFamily: "'Noto Sans KR', sans-serif",
        whiteSpace: 'nowrap',
      }}
    >
      {children}
    </button>
  )
}

// ── 배송지 관리 패널 ──────────────────────────────────────
// 목록 조회, 추가, 수정, 삭제 기능을 한 화면에서 처리한다.
function AddressesPanel({ showNotice }) {
  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(true)
  // 현재 수정 중인 배송지 ID ('new'면 추가 폼)
  const [editing, setEditing] = useState(null)

  // 컴포넌트가 마운트될 때 배송지 목록을 가져온다
  useEffect(() => {
    getAddresses()
      .then((res) => setAddresses(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const handleSave = async (formData, id) => {
    try {
      if (id === 'new') {
        const res = await createAddress(formData)
        setAddresses((prev) => {
          // 새 배송지가 기본이면 기존 기본 배송지 해제 반영
          const updated = formData.isDefault ? prev.map((a) => ({ ...a, isDefault: false })) : prev
          return [...updated, res.data]
        })
        showNotice('배송지가 추가되었습니다.')
      } else {
        const res = await updateAddress(id, formData)
        setAddresses((prev) => {
          const updated = formData.isDefault ? prev.map((a) => ({ ...a, isDefault: false })) : prev
          return updated.map((a) => (a.id === id ? res.data : a))
        })
        showNotice('배송지가 수정되었습니다.')
      }
      setEditing(null)
    } catch {
      showNotice('저장에 실패했습니다.', true)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteAddress(id)
      setAddresses((prev) => prev.filter((a) => a.id !== id))
      showNotice('배송지가 삭제되었습니다.')
    } catch {
      showNotice('삭제에 실패했습니다.', true)
    }
  }

  // 기본 배송지를 목록 맨 앞으로 정렬한다
  const sorted = [...addresses].sort((a, b) => (b.isDefault ? 1 : 0) - (a.isDefault ? 1 : 0))

  return (
    <div>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 28,
        }}
      >
        <h2 style={{ fontFamily: "'Noto Serif KR', serif", fontWeight: 300, fontSize: 24 }}>
          배송지 관리
        </h2>
        {editing !== 'new' && <SmallBtn onClick={() => setEditing('new')}>+ 배송지 추가</SmallBtn>}
      </div>

      {/* 배송지 추가 폼 */}
      {editing === 'new' && (
        <div style={{ border: '1px solid var(--color-border)', padding: '24px', marginBottom: 20 }}>
          <p
            style={{
              fontSize: 13,
              color: 'var(--color-fg-accent)',
              marginBottom: 18,
              letterSpacing: '0.04em',
            }}
          >
            새 배송지
          </p>
          <AddressForm
            onSave={(data) => handleSave(data, 'new')}
            onCancel={() => setEditing(null)}
          />
        </div>
      )}

      {loading ? (
        <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
          불러오는 중...
        </p>
      ) : sorted.length === 0 ? (
        <p style={{ fontSize: 14, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
          저장된 배송지가 없습니다.
        </p>
      ) : (
        <div style={{ borderTop: '1px solid var(--color-border)' }}>
          {sorted.map((addr) => (
            <div key={addr.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
              {/* 배송지 정보 행 */}
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  padding: '20px 0',
                }}
              >
                <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: 14 }}>{addr.recipientName}</span>
                    <span style={{ fontSize: 12, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
                      {addr.phone}
                    </span>
                    {addr.isDefault && (
                      <span
                        style={{
                          fontSize: 10,
                          letterSpacing: '0.06em',
                          color: 'var(--color-success)',
                          background: 'var(--color-success-bg)',
                          padding: '2px 7px',
                        }}
                      >
                        기본
                      </span>
                    )}
                  </div>
                  <span style={{ fontSize: 13, fontWeight: 300, color: 'var(--color-fg)' }}>
                    ({addr.zipCode}) {addr.address} {addr.addressDetail ?? ''}
                  </span>
                  {addr.note && (
                    <span style={{ fontSize: 12, color: 'var(--color-fg-muted)', fontWeight: 300 }}>
                      배송 메모: {addr.note}
                    </span>
                  )}
                </div>
                <div style={{ display: 'flex', gap: 8, flexShrink: 0, marginLeft: 16 }}>
                  <SmallBtn
                    outline
                    onClick={() => setEditing(editing === addr.id ? null : addr.id)}
                  >
                    {editing === addr.id ? '닫기' : '수정'}
                  </SmallBtn>
                  <SmallBtn
                    outline
                    onClick={() => handleDelete(addr.id)}
                    style={{ borderColor: '#ccc', color: '#999' }}
                  >
                    삭제
                  </SmallBtn>
                </div>
              </div>

              {/* 수정 폼 — disclosure 패턴 */}
              {editing === addr.id && (
                <div style={{ paddingBottom: 24 }}>
                  <AddressForm
                    initial={addr}
                    onSave={(data) => handleSave(data, addr.id)}
                    onCancel={() => setEditing(null)}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── 배송지 입력 폼 (추가/수정 공용) ──────────────────────
// initial prop이 있으면 수정 모드, 없으면 추가 모드
function AddressForm({ initial, onSave, onCancel }) {
  const [form, setForm] = useState({
    recipientName: initial?.recipientName ?? '',
    phone: initial?.phone ?? '',
    zipCode: initial?.zipCode ?? '',
    address: initial?.address ?? '',
    addressDetail: initial?.addressDetail ?? '',
    note: initial?.note ?? '',
    isDefault: initial?.isDefault ?? false,
  })
  const [loading, setLoading] = useState(false)

  // Daum 우편번호 API를 스크립트로 동적 로드해서 주소 검색을 열어준다
  const handleAddressSearch = () => {
    const openPostcode = () => {
      new window.daum.Postcode({
        oncomplete(data) {
          setForm((prev) => ({ ...prev, zipCode: data.zonecode, address: data.address }))
        },
      }).open()
    }

    if (window.daum?.Postcode) {
      // 이미 로드 완료
      openPostcode()
      return
    }

    // script 태그가 이미 삽입되어 있으면(=로딩 중) 중복 삽입 없이 onload 콜백만 추가한다.
    // window.daum만 체크하면 스크립트가 실행되기 전 두 번째 클릭에서 중복 삽입이 발생한다.
    const existing = document.querySelector('script[src*="postcode.v2.js"]')
    if (existing) {
      existing.addEventListener('load', openPostcode, { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
    script.onload = openPostcode
    document.head.appendChild(script)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.recipientName.trim() || !form.phone.trim() || !form.zipCode || !form.address) return
    setLoading(true)
    try {
      await onSave(form)
    } finally {
      setLoading(false)
    }
  }

  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', gap: 10 }}>
        <input
          type="text"
          placeholder="받는 분"
          required
          value={form.recipientName}
          onChange={set('recipientName')}
          style={{ ...inputStyle, flex: 1 }}
        />
        <input
          type="tel"
          placeholder="연락처"
          required
          value={form.phone}
          onChange={set('phone')}
          style={{ ...inputStyle, flex: 1 }}
        />
      </div>
      <div style={{ display: 'flex', gap: 10 }}>
        <input
          type="text"
          placeholder="우편번호"
          required
          readOnly
          value={form.zipCode}
          style={{ ...inputStyle, flex: '0 0 120px', background: 'var(--color-bg-input)' }}
        />
        <SmallBtn
          type="button"
          outline
          onClick={handleAddressSearch}
          style={{ whiteSpace: 'nowrap' }}
        >
          주소 검색
        </SmallBtn>
      </div>
      <input
        type="text"
        placeholder="주소"
        required
        readOnly
        value={form.address}
        style={{ ...inputStyle, background: 'var(--color-bg-input)' }}
      />
      <input
        type="text"
        placeholder="상세 주소"
        value={form.addressDetail}
        onChange={set('addressDetail')}
        style={inputStyle}
      />
      <input
        type="text"
        placeholder="배송 메모 (선택)"
        value={form.note}
        onChange={set('note')}
        style={inputStyle}
      />
      <label
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          fontSize: 13,
          fontWeight: 300,
          cursor: 'pointer',
        }}
      >
        <input
          type="checkbox"
          checked={form.isDefault}
          onChange={(e) => setForm((prev) => ({ ...prev, isDefault: e.target.checked }))}
        />
        기본 배송지로 설정
      </label>
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 4 }}>
        <SmallBtn type="button" outline onClick={onCancel}>
          취소
        </SmallBtn>
        <SmallBtn type="submit" disabled={loading}>
          {loading ? '저장 중' : '저장'}
        </SmallBtn>
      </div>
    </form>
  )
}

// ── 회원 탈퇴 확인 모달 ───────────────────────────────────
// 오버레이 클릭 시 닫힘. 내부 카드 클릭은 버블링 차단.
function DeleteModal({ onClose, onConfirm }) {
  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.45)',
        zIndex: 300,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
        animation: 'fadeUp .2s ease both',
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: 'var(--color-bg)',
          padding: 'clamp(32px,5vw,48px)',
          maxWidth: 420,
          width: '100%',
          display: 'flex',
          flexDirection: 'column',
          gap: 28,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div>
          <h2
            style={{
              fontFamily: "'Noto Serif KR', serif",
              fontWeight: 300,
              fontSize: 22,
              marginBottom: 14,
            }}
          >
            정말 탈퇴하시겠습니까?
          </h2>
          <p
            style={{
              fontSize: 14,
              color: 'var(--color-fg-muted)',
              lineHeight: 1.8,
              fontWeight: 300,
            }}
          >
            탈퇴하면 계정과 모든 주문 정보가 즉시 삭제됩니다.
            <br />이 작업은 되돌릴 수 없습니다.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button
            onClick={onClose}
            style={{
              flex: 1,
              cursor: 'pointer',
              border: '1px solid var(--color-border)',
              background: 'transparent',
              color: 'var(--color-fg)',
              padding: '14px',
              fontSize: 14,
              letterSpacing: '0.03em',
              fontFamily: "'Noto Sans KR', sans-serif",
            }}
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            style={{
              flex: 1,
              cursor: 'pointer',
              border: '1px solid var(--color-danger)',
              background: 'var(--color-danger)',
              color: '#fff',
              padding: '14px',
              fontSize: 14,
              letterSpacing: '0.03em',
              fontFamily: "'Noto Sans KR', sans-serif",
            }}
          >
            탈퇴하겠습니다
          </button>
        </div>
      </div>
    </div>
  )
}
