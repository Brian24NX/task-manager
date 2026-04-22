const STORAGE_KEY = 'tm_auth_v1'
const SESSION_TTL_MS = 30 * 60 * 1000
const API_BASE = import.meta.env.VITE_API_URL || ''

function encodeBasic(username, password) {
  return 'Basic ' + btoa(unescape(encodeURIComponent(`${username}:${password}`)))
}

function readSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed?.header || !parsed?.expiresAt) return null
    if (Date.now() >= parsed.expiresAt) return null
    return parsed
  } catch (_) {
    return null
  }
}

function writeSession(header) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      header,
      expiresAt: Date.now() + SESSION_TTL_MS
    }))
  } catch (_) {}
}

export function getAuthHeader() {
  const s = readSession()
  if (!s) {
    try { localStorage.removeItem(STORAGE_KEY) } catch (_) {}
    return ''
  }
  return s.header
}

export function isAuthed() {
  return Boolean(getAuthHeader())
}

export function clearAuth() {
  try { localStorage.removeItem(STORAGE_KEY) } catch (_) {}
}

export async function login(username, password) {
  const header = encodeBasic(username, password)
  const res = await fetch(`${API_BASE}/api/login`, {
    method: 'POST',
    headers: { Authorization: header }
  })
  if (res.status === 401) {
    throw new Error('Invalid username or password')
  }
  if (!res.ok) {
    throw new Error('Login failed — please try again')
  }
  const data = await res.json().catch(() => ({}))
  writeSession(header)
  return data
}

export async function authedFetch(url, options = {}) {
  const header = getAuthHeader()
  if (!header) {
    window.dispatchEvent(new CustomEvent('tm:unauthorized'))
    return new Response(null, { status: 401 })
  }
  const headers = { ...(options.headers || {}) }
  headers.Authorization = header
  const res = await fetch(url, { ...options, headers })
  if (res.status === 401) {
    clearAuth()
    window.dispatchEvent(new CustomEvent('tm:unauthorized'))
  } else {
    writeSession(header)
  }
  return res
}
