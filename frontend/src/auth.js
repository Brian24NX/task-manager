const STORAGE_KEY = 'tm_auth_v1'
const API_BASE = import.meta.env.VITE_API_URL || ''

function encodeBasic(username, password) {
  return 'Basic ' + btoa(unescape(encodeURIComponent(`${username}:${password}`)))
}

export function getAuthHeader() {
  try {
    return sessionStorage.getItem(STORAGE_KEY) || ''
  } catch (_) {
    return ''
  }
}

export function isAuthed() {
  return Boolean(getAuthHeader())
}

export function clearAuth() {
  try { sessionStorage.removeItem(STORAGE_KEY) } catch (_) {}
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
  try { sessionStorage.setItem(STORAGE_KEY, header) } catch (_) {}
  return data
}

export async function authedFetch(url, options = {}) {
  const header = getAuthHeader()
  const headers = { ...(options.headers || {}) }
  if (header) headers.Authorization = header
  const res = await fetch(url, { ...options, headers })
  if (res.status === 401) {
    clearAuth()
    window.dispatchEvent(new CustomEvent('tm:unauthorized'))
  }
  return res
}
