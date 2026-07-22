import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { updateMe, deleteMe, logout as logoutApi } from '../api/auth'
import { getOrders } from '../api/orders'

// 주문 상태 한글 표기
const STATUS_LABEL = {
  ORDERED: '주문완료',
  PAID: '결제완료',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소됨',
}

// 상태별 색상 정의
const STATUS_STYLE = {
  ORDERED: { color: '#6d6c61', bg: '#f0eedf' },
  PAID: { color: '#4a5e3a', bg: '#eef3e8' },
  SHIPPING: { color: '#2d5a8e', bg: '#e8eef6' },
  DELIVERED: { color: '#333330', bg: '#e5e3d3' },
  CANCELED: { color: '#aaa', bg: '#f4f4f4' },
}

const fmt = (n) => Number(n).toLocaleString('ko-KR') + '원'

// 폼 입력 공통 스타일
const inputStyle = {
  border: '1px solid #dddaca',
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
  const [tab, setTab] = useState('orders') // 'orders' | 'settings'
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
      /* 서버가 세션을 먼저 끊을 수 있으므로 무시 */
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
                background: '#e5e3d3',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: "'Noto Serif KR', serif",
                fontSize: 17,
                color: '#333330',
                marginBottom: 14,
                userSelect: 'none',
              }}
            >
              {user?.name?.[0]}
            </div>
            <p style={{ fontSize: 15, fontWeight: 400, marginBottom: 3 }}>{user?.name}</p>
            <p style={{ fontSize: 12, color: '#6d6c61' }}>{user?.email}</p>
          </div>

          {/* 내비게이션 탭 */}
          <nav style={{ display: 'flex', flexDirection: 'column' }}>
            <SidebarItem active={tab === 'orders'} onClick={() => setTab('orders')}>
              주문 내역
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
              onMouseEnter={(e) => (e.currentTarget.style.color = '#e63946')}
              onMouseLeave={(e) => (e.currentTarget.style.color = '#bbb')}
            >
              회원 탈퇴
            </button>
          </div>
        </aside>

        {/* ── 콘텐츠 영역 ────────────────────────────────── */}
        <div style={{ flex: 1, minWidth: 0, padding: 'clamp(32px,4vw,56px) clamp(24px,4vw,56px)' }}>
          {tab === 'orders' && <OrdersPanel />}
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
            background: notice.error ? '#c0392b' : '#333326',
            color: '#e5e3d3',
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
        borderLeft: active ? '2px solid #333330' : '2px solid transparent',
        padding: '12px 0 12px 16px',
        fontSize: 14,
        color: active ? '#333330' : '#6d6c61',
        fontWeight: active ? 400 : 300,
        fontFamily: "'Noto Sans KR', sans-serif",
        cursor: 'pointer',
        textAlign: 'left',
        letterSpacing: '0.02em',
      }}
      onMouseEnter={(e) => {
        if (!active) e.currentTarget.style.color = '#333330'
      }}
      onMouseLeave={(e) => {
        if (!active) e.currentTarget.style.color = '#6d6c61'
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
            border: '1px solid #dddaca',
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
                borderRight: i < arr.length - 1 ? '1px solid #dddaca' : 'none',
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
              <span style={{ fontSize: 11, color: '#75775e', letterSpacing: '0.06em' }}>
                {label}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* 주문 목록 */}
      {loading ? (
        <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>불러오는 중...</p>
      ) : orders.length === 0 ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontSize: 14, color: '#6d6c61', fontWeight: 300 }}>
            아직 주문 내역이 없습니다.
          </p>
          <Link
            to="/shop"
            style={{
              fontSize: 13,
              borderBottom: '1px solid #333330',
              paddingBottom: 1,
              display: 'inline-block',
            }}
          >
            쇼핑하러 가기
          </Link>
        </div>
      ) : (
        <div style={{ borderTop: '1px solid #dddaca' }}>
          {orders.map((order) => (
            <Link
              key={order.id}
              to={`/orders/${order.id}`}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '20px 0',
                borderBottom: '1px solid #dddaca',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.background = '#f6f4e6')}
              onMouseLeave={(e) => (e.currentTarget.style.background = '')}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
                <span style={{ fontSize: 14, fontFamily: "'Noto Serif KR', serif" }}>
                  주문 #{order.id}
                </span>
                <span style={{ fontSize: 12, color: '#6d6c61', fontWeight: 300 }}>
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
      {STATUS_LABEL[status] ?? status}
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

      <div style={{ borderTop: '1px solid #dddaca' }}>
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
    <div style={{ borderBottom: '1px solid #dddaca' }}>
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
              color: '#75775e',
              letterSpacing: '0.08em',
              minWidth: 64,
              textTransform: 'uppercase',
            }}
          >
            {label}
          </span>
          <span style={{ fontSize: 14, fontWeight: 300 }}>{value}</span>
        </div>
        <span style={{ fontSize: 12, color: '#6d6c61', letterSpacing: '0.04em' }}>
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
        border: '1px solid #333330',
        background: outline ? 'transparent' : '#333330',
        color: outline ? '#333330' : '#fffef2',
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
          background: '#fffef2',
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
          <p style={{ fontSize: 14, color: '#6d6c61', lineHeight: 1.8, fontWeight: 300 }}>
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
              border: '1px solid #dddaca',
              background: 'transparent',
              color: '#333330',
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
              border: '1px solid #e63946',
              background: '#e63946',
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
