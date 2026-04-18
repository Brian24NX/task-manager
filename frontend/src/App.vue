<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import TaskForm from './components/TaskForm.vue'

const API = (import.meta.env.VITE_API_URL || '') + '/api/tasks'

const CACHE_KEY = 'tm_tasks_cache_v1'
const STATS_CACHE_KEY = 'tm_stats_cache_v1'

function loadCachedTasks() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch (_) {
    return []
  }
}

function loadCachedStats() {
  try {
    const raw = localStorage.getItem(STATS_CACHE_KEY)
    return raw ? JSON.parse(raw) : { total: 0, todo: 0, inProgress: 0, done: 0, overdue: 0 }
  } catch (_) {
    return { total: 0, todo: 0, inProgress: 0, done: 0, overdue: 0 }
  }
}

const tasks = ref(loadCachedTasks())
const loading = ref(false)
const showCompleted = ref(false)
const filterDate = ref('')
const filterStatus = ref('')
const searchQuery = ref('')
const editingTask = ref(null)
const showForm = ref(false)
const showDeleteConfirm = ref(false)
const taskToDelete = ref(null)
const toasts = ref([])
const stats = ref(loadCachedStats())
const showNotifySettings = ref(false)
const showNameEdit = ref(false)
const userName = ref(localStorage.getItem('tm_user_name') || 'Brian')
const nameDraft = ref(userName.value)

// --- Theme ---
const theme = ref(resolveInitialTheme())
function resolveInitialTheme() {
  try {
    const saved = localStorage.getItem('tm_theme')
    if (saved === 'light' || saved === 'dark') return saved
  } catch (_) {}
  if (typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }
  return 'light'
}
function applyTheme(t) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', t)
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', t === 'dark' ? '#0c0c14' : '#e0f2fe')
}
function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  try { localStorage.setItem('tm_theme', theme.value) } catch (_) {}
  applyTheme(theme.value)
}
applyTheme(theme.value)

// --- Weather (Open-Meteo + BigDataCloud reverse geocoding) ---
const weather = ref(null)
const weatherError = ref('')
const WEATHER_CACHE_KEY = 'tm_weather_cache_v1'
const WEATHER_TTL_MS = 30 * 60 * 1000

const isImperial = (() => {
  try {
    const region = new Intl.Locale(navigator.language || 'en-US').maximize().region
    return ['US', 'LR', 'MM', 'BS', 'BZ', 'KY', 'PW'].includes(region)
  } catch (_) {
    return (navigator.language || '').toLowerCase().startsWith('en-us')
  }
})()

function weatherVisual(code) {
  if (code === 0) return { label: 'Sunny', icon: 'wb_sunny', class: '' }
  if (code === 1 || code === 2) return { label: 'Partly cloudy', icon: 'partly_cloudy_day', class: 'cloudy' }
  if (code === 3) return { label: 'Cloudy', icon: 'cloud', class: 'cloudy' }
  if (code === 45 || code === 48) return { label: 'Foggy', icon: 'foggy', class: 'fog' }
  if (code >= 51 && code <= 57) return { label: 'Drizzle', icon: 'rainy', class: 'rainy' }
  if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return { label: 'Rainy', icon: 'rainy', class: 'rainy' }
  if ((code >= 71 && code <= 77) || code === 85 || code === 86) return { label: 'Snowy', icon: 'ac_unit', class: 'snow' }
  if (code >= 95) return { label: 'Stormy', icon: 'thunderstorm', class: 'storm' }
  return { label: 'Weather', icon: 'cloud', class: 'cloudy' }
}

async function loadWeather() {
  try {
    const cachedRaw = localStorage.getItem(WEATHER_CACHE_KEY)
    if (cachedRaw) {
      const cached = JSON.parse(cachedRaw)
      if (cached && Date.now() - cached.ts < WEATHER_TTL_MS) {
        weather.value = cached.data
        return
      }
    }
  } catch (_) {}

  if (!('geolocation' in navigator)) {
    weatherError.value = 'no-geo'
    return
  }

  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      const { latitude, longitude } = pos.coords
      try {
        const unit = isImperial ? 'fahrenheit' : 'celsius'
        const [wRes, gRes] = await Promise.all([
          fetch(`https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,weather_code&temperature_unit=${unit}`),
          fetch(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${latitude}&longitude=${longitude}&localityLanguage=en`).catch(() => null)
        ])
        if (!wRes.ok) throw new Error('weather fetch failed')
        const wData = await wRes.json()
        let city = ''
        if (gRes && gRes.ok) {
          const g = await gRes.json()
          city = g.city || g.locality || g.principalSubdivision || ''
        }
        const data = {
          temp: Math.round(wData.current?.temperature_2m ?? 0),
          unit: isImperial ? 'F' : 'C',
          code: wData.current?.weather_code ?? 0,
          city
        }
        weather.value = data
        try { localStorage.setItem(WEATHER_CACHE_KEY, JSON.stringify({ ts: Date.now(), data })) } catch (_) {}
      } catch (err) {
        weatherError.value = 'fetch-failed'
      }
    },
    () => { weatherError.value = 'denied' },
    { timeout: 8000, maximumAge: 10 * 60 * 1000 }
  )
}

// --- Daily quote ---
const QUOTES = [
  { text: 'Time is money, don\u2019t waste it.', author: 'Benjamin Franklin' },
  { text: 'The secret of getting ahead is getting started.', author: 'Mark Twain' },
  { text: 'It always seems impossible until it\u2019s done.', author: 'Nelson Mandela' },
  { text: 'Do the hard jobs first. The easy jobs will take care of themselves.', author: 'Dale Carnegie' },
  { text: 'Little by little, one travels far.', author: 'J.R.R. Tolkien' },
  { text: 'Action is the foundational key to all success.', author: 'Pablo Picasso' },
  { text: 'The way to get started is to quit talking and begin doing.', author: 'Walt Disney' },
  { text: 'Well done is better than well said.', author: 'Benjamin Franklin' },
  { text: 'You don\u2019t have to be great to start, but you have to start to be great.', author: 'Zig Ziglar' },
  { text: 'Focus on being productive instead of busy.', author: 'Tim Ferriss' },
  { text: 'What gets measured gets managed.', author: 'Peter Drucker' },
  { text: 'The best way out is always through.', author: 'Robert Frost' },
  { text: 'Small deeds done are better than great deeds planned.', author: 'Peter Marshall' },
  { text: 'Quality means doing it right when no one is looking.', author: 'Henry Ford' },
  { text: 'Motivation gets you going; discipline keeps you growing.', author: 'John C. Maxwell' },
  { text: 'One day, or day one. You decide.', author: 'Unknown' },
  { text: 'Great things are done by a series of small things brought together.', author: 'Vincent Van Gogh' },
  { text: 'Don\u2019t watch the clock; do what it does. Keep going.', author: 'Sam Levenson' },
  { text: 'A goal without a plan is just a wish.', author: 'Antoine de Saint-Exup\u00e9ry' },
  { text: 'Energy and persistence conquer all things.', author: 'Benjamin Franklin' },
  { text: 'Discipline is the bridge between goals and accomplishment.', author: 'Jim Rohn' },
  { text: 'You miss 100% of the shots you don\u2019t take.', author: 'Wayne Gretzky' },
  { text: 'Start where you are. Use what you have. Do what you can.', author: 'Arthur Ashe' },
  { text: 'Simplicity is the ultimate sophistication.', author: 'Leonardo da Vinci' },
  { text: 'Success is the sum of small efforts repeated day in and day out.', author: 'Robert Collier' },
  { text: 'Don\u2019t count the days, make the days count.', author: 'Muhammad Ali' },
  { text: 'The best time to plant a tree was 20 years ago. The second best time is now.', author: 'Chinese proverb' },
  { text: 'Make each day your masterpiece.', author: 'John Wooden' },
  { text: 'Whether you think you can, or you think you can\u2019t \u2013 you\u2019re right.', author: 'Henry Ford' },
  { text: 'Don\u2019t be afraid to give up the good to go for the great.', author: 'John D. Rockefeller' },
  { text: 'We are what we repeatedly do. Excellence, then, is not an act, but a habit.', author: 'Aristotle' },
  { text: 'The journey of a thousand miles begins with a single step.', author: 'Lao Tzu' },
  { text: 'If you can dream it, you can do it.', author: 'Walt Disney' },
  { text: 'Productivity is never an accident. It is always the result of planning and effort.', author: 'Paul J. Meyer' },
  { text: 'Perfection is the enemy of progress.', author: 'Winston Churchill' },
  { text: 'Take the first step in faith. You don\u2019t have to see the whole staircase.', author: 'Martin Luther King Jr.' }
]

const dailyQuote = computed(() => {
  const epochDay = Math.floor(Date.now() / 86400000)
  return QUOTES[epochDay % QUOTES.length]
})

// --- Name edit ---
function openNameEdit() {
  nameDraft.value = userName.value
  showNameEdit.value = true
}
function saveName() {
  const trimmed = (nameDraft.value || '').trim().slice(0, 40)
  if (trimmed) {
    userName.value = trimmed
    try { localStorage.setItem('tm_user_name', trimmed) } catch (_) {}
  }
  showNameEdit.value = false
}
const notifySettings = ref({
  email: localStorage.getItem('notify_email') || '',
  phone: localStorage.getItem('notify_phone') || '',
  onComplete: localStorage.getItem('notify_on_complete') === 'true',
  onCompleteEmail: localStorage.getItem('notify_complete_email') !== 'false',
  onCompleteSms: localStorage.getItem('notify_complete_sms') !== 'false'
})

// Pending notification info (set during save, used after API returns)
let pendingNotify = null

let searchTimeout = null

const today = new Date().toISOString().slice(0, 10)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
})

const todayFormatted = computed(() => {
  return new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })
})

const completionPercent = computed(() => {
  if (stats.value.total === 0) return 0
  return Math.round((stats.value.done / stats.value.total) * 100)
})

function getTomorrow() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}
const tomorrow = getTomorrow()

const priorityOrder = { URGENT: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }

const filteredAndSorted = computed(() => {
  let list = [...tasks.value]

  if (filterStatus.value) {
    list = list.filter(t => t.status === filterStatus.value)
  }

  list.sort((a, b) => {
    const aOverdue = a.dueDate < today && a.status !== 'DONE' ? 0 : 1
    const bOverdue = b.dueDate < today && b.status !== 'DONE' ? 0 : 1
    if (aOverdue !== bOverdue) return aOverdue - bOverdue

    const dateCompare = (a.dueDate || '').localeCompare(b.dueDate || '')
    if (dateCompare !== 0) return dateCompare

    return (priorityOrder[a.priority] ?? 3) - (priorityOrder[b.priority] ?? 3)
  })

  return list
})

const overdueTasks = computed(() =>
  filteredAndSorted.value.filter(t => t.status !== 'DONE' && t.dueDate < today)
)
const activeTasks = computed(() =>
  filteredAndSorted.value.filter(t => t.status !== 'DONE' && t.dueDate >= today)
)
const doneTasks = computed(() => {
  const done = filteredAndSorted.value.filter(t => t.status === 'DONE')
  done.sort((a, b) => (b.dueDate || '').localeCompare(a.dueDate || ''))
  return done
})

// --- Confetti burst on completion ---
const CONFETTI_COLORS = ['#a78bfa', '#22d3ee', '#34d399', '#fbbf24', '#f472b6', '#60a5fa']
function fireConfetti(anchorEl) {
  if (typeof window === 'undefined') return
  const rect = anchorEl.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const burst = document.createElement('div')
  burst.className = 'confetti-burst'
  burst.style.left = `${cx}px`
  burst.style.top = `${cy}px`
  const count = 22
  for (let i = 0; i < count; i++) {
    const piece = document.createElement('span')
    piece.className = 'confetti-piece'
    const angle = (Math.PI * 2 * i) / count + Math.random() * 0.4
    const dist = 80 + Math.random() * 90
    const dx = Math.cos(angle) * dist
    const dy = Math.sin(angle) * dist - 40
    const rot = (Math.random() * 720 - 360).toFixed(0) + 'deg'
    piece.style.setProperty('--dx', `${dx.toFixed(1)}px`)
    piece.style.setProperty('--dy', `${dy.toFixed(1)}px`)
    piece.style.setProperty('--rot', rot)
    piece.style.background = CONFETTI_COLORS[i % CONFETTI_COLORS.length]
    piece.style.animationDelay = `${Math.random() * 60}ms`
    burst.appendChild(piece)
  }
  document.body.appendChild(burst)
  setTimeout(() => burst.remove(), 1200)
}

// --- Toasts ---
let toastId = 0
function addToast(message, type = 'success') {
  const id = ++toastId
  toasts.value.push({ id, message, type, leaving: false })
  setTimeout(() => removeToast(id), 3500)
}

function removeToast(id) {
  const t = toasts.value.find(t => t.id === id)
  if (t) {
    t.leaving = true
    setTimeout(() => {
      toasts.value = toasts.value.filter(t => t.id !== id)
    }, 300)
  }
}

function toastIcon(type) {
  if (type === 'error') return 'error'
  if (type === 'warning') return 'warning'
  return 'check_circle'
}

// --- API ---
async function fetchTasks({ silent = false } = {}) {
  if (!silent) loading.value = true
  try {
    const query = filterDate.value ? `?dueDate=${filterDate.value}` : ''
    const res = await fetch(`${API}${query}`)
    if (!res.ok) throw new Error('Could not load tasks')
    tasks.value = await res.json()
    if (!filterDate.value) {
      try { localStorage.setItem(CACHE_KEY, JSON.stringify(tasks.value)) } catch (_) {}
    }
  } catch (err) {
    if (!silent) addToast(err.message, 'error')
  } finally {
    if (!silent) loading.value = false
  }
}

async function searchTasks(q) {
  if (!q.trim()) {
    await fetchTasks()
    return
  }
  loading.value = true
  try {
    const res = await fetch(`${API}/search?q=${encodeURIComponent(q)}`)
    if (!res.ok) throw new Error('Search failed')
    tasks.value = await res.json()
  } catch (err) {
    addToast(err.message, 'error')
  } finally {
    loading.value = false
  }
}

async function refreshStats() {
  try {
    const res = await fetch(`${API}/stats`)
    if (res.ok) {
      stats.value = await res.json()
      try { localStorage.setItem(STATS_CACHE_KEY, JSON.stringify(stats.value)) } catch (_) {}
    }
  } catch (_) {
    // silently fail
  }
}

function saveNotifySettings() {
  localStorage.setItem('notify_email', notifySettings.value.email)
  localStorage.setItem('notify_phone', notifySettings.value.phone)
  localStorage.setItem('notify_on_complete', notifySettings.value.onComplete)
  localStorage.setItem('notify_complete_email', notifySettings.value.onCompleteEmail)
  localStorage.setItem('notify_complete_sms', notifySettings.value.onCompleteSms)
  showNotifySettings.value = false
  addToast('Notification settings saved')
}

async function sendNotification(taskTitle, eventType, notifyEmail, notifySms) {
  if (!notifyEmail && !notifySms) return
  const NOTIFY_API = (import.meta.env.VITE_API_URL || '') + '/api/notifications/send'
  try {
    const res = await fetch(NOTIFY_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: notifySettings.value.email,
        phone: notifySettings.value.phone,
        notifyEmail,
        notifySms,
        taskTitle,
        eventType
      })
    })
    if (res.ok) {
      const data = await res.json()
      for (const r of data.results || []) {
        addToast(r, r.toLowerCase().includes('failed') ? 'warning' : 'success')
      }
    }
  } catch (err) {
    addToast('Notification error: ' + err.message, 'warning')
  }
}

async function saveTask(task) {
  const isEditing = Boolean(editingTask.value)
  const url = isEditing ? `${API}/${editingTask.value.id}` : API
  const method = isEditing ? 'PUT' : 'POST'

  const wantsEmail = task.notifyEmail
  const wantsSms = task.notifySms
  const { notifyEmail, notifySms, ...taskData } = task

  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(taskData)
    })
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      throw new Error(data.message || 'Could not save task')
    }
    addToast(isEditing ? 'Task updated successfully' : 'Task created successfully')
    cancelForm()
    await Promise.all([fetchTasks(), refreshStats()])

    if (!isEditing && (wantsEmail || wantsSms)) {
      await sendNotification(taskData.title, 'CREATED', wantsEmail, wantsSms)
    }
  } catch (err) {
    addToast(err.message, 'error')
  }
}

async function quickStatus(task, status, event) {
  try {
    if (status === 'DONE' && event?.currentTarget) {
      fireConfetti(event.currentTarget)
    }
    const res = await fetch(`${API}/${task.id}/status?status=${status}`, { method: 'PATCH' })
    if (!res.ok) throw new Error('Could not update status')
    const label = status === 'DONE' ? 'marked as done' : status === 'IN_PROGRESS' ? 'moved to in progress' : 'moved to to-do'
    addToast(`Task ${label}`)
    await Promise.all([fetchTasks(), refreshStats()])

    if (status === 'DONE' && notifySettings.value.onComplete) {
      await sendNotification(
        task.title,
        'COMPLETED',
        notifySettings.value.onCompleteEmail,
        notifySettings.value.onCompleteSms
      )
    }
  } catch (err) {
    addToast(err.message, 'error')
  }
}

function confirmDelete(task) {
  taskToDelete.value = task
  showDeleteConfirm.value = true
}

async function executeDelete() {
  if (!taskToDelete.value) return
  try {
    const res = await fetch(`${API}/${taskToDelete.value.id}`, { method: 'DELETE' })
    if (!res.ok) throw new Error('Could not delete task')
    addToast('Task deleted successfully')
    showDeleteConfirm.value = false
    taskToDelete.value = null
    await Promise.all([fetchTasks(), refreshStats()])
  } catch (err) {
    addToast(err.message, 'error')
  }
}

function cancelDelete() {
  showDeleteConfirm.value = false
  taskToDelete.value = null
}

// --- Form ---
function startCreate() {
  editingTask.value = null
  showForm.value = true
}

function startEdit(task) {
  editingTask.value = { ...task }
  showForm.value = true
}

function cancelForm() {
  editingTask.value = null
  showForm.value = false
}

// --- Search debounce ---
watch(searchQuery, (val) => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    searchTasks(val)
  }, 300)
})

// --- Date filter ---
watch(filterDate, () => {
  if (!searchQuery.value) {
    fetchTasks()
  }
})

// --- Helpers ---
function isOverdue(task) {
  return task.dueDate < today && task.status !== 'DONE'
}

function dueLabel(task) {
  if (task.dueDate === today) return 'Today'
  if (task.dueDate === tomorrow) return 'Tomorrow'
  if (isOverdue(task)) return 'Overdue'
  return task.dueDate
}

function dueLabelClass(task) {
  if (isOverdue(task)) return 'due-overdue'
  if (task.dueDate === today) return 'due-today'
  if (task.dueDate === tomorrow) return 'due-tomorrow'
  return ''
}

function priorityLabel(p) {
  return { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', URGENT: 'Urgent' }[p] || p
}

function statusLabel(s) {
  return { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' }[s] || s
}

onMounted(() => {
  const hasCache = tasks.value.length > 0
  Promise.all([fetchTasks({ silent: hasCache }), refreshStats()])
  loadWeather()
})
</script>

<template>
  <!-- Aurora background orbs -->
  <div class="aurora-orb a"></div>
  <div class="aurora-orb b"></div>
  <div class="aurora-orb c"></div>

  <!-- Toasts -->
  <div class="toast-container">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="toast"
      :class="[toast.type, { leaving: toast.leaving }]"
    >
      <span class="material-symbols-rounded">{{ toastIcon(toast.type) }}</span>
      <span class="toast-message">{{ toast.message }}</span>
    </div>
  </div>

  <!-- Delete Confirmation Modal -->
  <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="cancelDelete">
    <div class="modal-card">
      <div class="modal-icon">
        <span class="material-symbols-rounded">delete_forever</span>
      </div>
      <h3>Delete Task</h3>
      <p>Are you sure you want to delete "<strong>{{ taskToDelete?.title }}</strong>"? This action cannot be undone.</p>
      <div class="modal-actions">
        <button class="btn-cancel" @click="cancelDelete">Cancel</button>
        <button class="btn-danger" @click="executeDelete">
          <span class="material-symbols-rounded" style="font-size:18px">delete</span>
          Delete
        </button>
      </div>
    </div>
  </div>

  <!-- Notification Settings Modal -->
  <div v-if="showNotifySettings" class="modal-overlay" @click.self="showNotifySettings = false">
    <div class="modal-card notify-settings-card">
      <div class="modal-icon">
        <span class="material-symbols-rounded">notifications_active</span>
      </div>
      <h3>Notification Settings</h3>
      <p class="notify-hint">Configure where to receive notifications when tasks are created or completed.</p>

      <div class="notify-form">
        <div class="form-group">
          <label for="notify-email">
            <span class="material-symbols-rounded" style="font-size:16px">email</span>
            Email Address
          </label>
          <input id="notify-email" v-model="notifySettings.email" type="email" placeholder="you@example.com" />
        </div>

        <div class="form-group">
          <label for="notify-phone">
            <span class="material-symbols-rounded" style="font-size:16px">phone</span>
            Phone Number
          </label>
          <input id="notify-phone" v-model="notifySettings.phone" type="tel" placeholder="+1234567890" />
        </div>

        <div class="notify-completion-section">
          <label class="notify-check">
            <input type="checkbox" v-model="notifySettings.onComplete" />
            Notify me when tasks are completed
          </label>
          <div v-if="notifySettings.onComplete" class="notify-sub-options">
            <label class="notify-check">
              <input type="checkbox" v-model="notifySettings.onCompleteEmail" />
              <span class="material-symbols-rounded" style="font-size:14px">email</span> via Email
            </label>
            <label class="notify-check">
              <input type="checkbox" v-model="notifySettings.onCompleteSms" />
              <span class="material-symbols-rounded" style="font-size:14px">sms</span> via SMS
            </label>
          </div>
        </div>
      </div>

      <div class="modal-actions">
        <button class="btn-cancel" @click="showNotifySettings = false">Cancel</button>
        <button class="btn-submit" @click="saveNotifySettings">
          <span class="material-symbols-rounded" style="font-size:18px">save</span>
          Save Settings
        </button>
      </div>
    </div>
  </div>

  <!-- Name Edit Modal -->
  <div v-if="showNameEdit" class="modal-overlay" @click.self="showNameEdit = false">
    <div class="modal-card name-modal-card">
      <div class="modal-icon">
        <span class="material-symbols-rounded">waving_hand</span>
      </div>
      <h3>What should we call you?</h3>
      <p>This only lives on your device.</p>
      <input
        v-model="nameDraft"
        type="text"
        maxlength="40"
        placeholder="Your name"
        @keyup.enter="saveName"
      />
      <div class="modal-actions">
        <button class="btn-cancel" @click="showNameEdit = false">Cancel</button>
        <button class="btn-submit" @click="saveName">
          <span class="material-symbols-rounded" style="font-size:18px">save</span>
          Save
        </button>
      </div>
    </div>
  </div>

  <!-- Header -->
  <header class="app-header">
    <div class="header-content">
      <div class="header-top">
        <div class="header-left">
          <div class="header-logo">
            <span class="material-symbols-rounded">task_alt</span>
          </div>
          <div>
            <h1>Task Manager</h1>
            <p class="header-date">{{ todayFormatted }}</p>
          </div>
        </div>
        <div class="header-tools">
          <div
            v-if="weather"
            class="weather-widget"
            :title="`${weather.city ? weather.city + ' \u00B7 ' : ''}${weatherVisual(weather.code).label}`"
          >
            <div class="weather-icon-wrap" :class="weatherVisual(weather.code).class">
              <span class="material-symbols-rounded">{{ weatherVisual(weather.code).icon }}</span>
            </div>
            <div class="weather-info">
              <span class="weather-temp">{{ weather.temp }}°{{ weather.unit }}</span>
              <span v-if="weather.city" class="weather-loc">{{ weather.city }}</span>
              <span v-else class="weather-loc">{{ weatherVisual(weather.code).label }}</span>
            </div>
          </div>
          <div v-else-if="!weatherError" class="weather-widget loading">
            <div class="weather-icon-wrap">
              <span class="material-symbols-rounded">cloud</span>
            </div>
            <span class="weather-loc">Locating…</span>
          </div>
          <button class="theme-toggle" @click="toggleTheme" :title="theme === 'dark' ? 'Switch to light' : 'Switch to dark'">
            <span v-if="theme === 'dark'" class="material-symbols-rounded theme-sun">light_mode</span>
            <span v-else class="material-symbols-rounded theme-moon">dark_mode</span>
          </button>
          <button class="btn-notify-settings" @click="showNotifySettings = true" title="Notification Settings">
            <span class="material-symbols-rounded">notifications</span>
          </button>
        </div>
      </div>
      <div class="header-greeting">
        <h2>
          {{ greeting }},
          <span class="greeting-name name-edit" @click="openNameEdit" title="Click to change">{{ userName }}</span>!
        </h2>
        <p class="status-line" v-if="stats.total > 0">
          You have <strong>{{ stats.todo + stats.inProgress }}</strong> active {{ (stats.todo + stats.inProgress) === 1 ? 'task' : 'tasks' }}
          <span v-if="stats.overdue > 0" class="header-overdue">&middot; {{ stats.overdue }} overdue</span>
        </p>
        <p class="status-line" v-else>No tasks yet. Create one to get started!</p>
        <blockquote class="daily-quote">
          {{ dailyQuote.text }}
          <span class="quote-author">— {{ dailyQuote.author }}</span>
        </blockquote>
      </div>
      <div v-if="stats.total > 0" class="header-progress">
        <div class="progress-info">
          <span>Progress</span>
          <span>{{ completionPercent }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: completionPercent + '%' }"></div>
        </div>
      </div>
    </div>
  </header>

  <!-- Main Content -->
  <main class="container">
    <!-- Stats -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon total">
          <span class="material-symbols-rounded">inventory_2</span>
        </div>
        <div class="stat-info">
          <div class="stat-count">{{ stats.total }}</div>
          <div class="stat-label">Total</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon todo">
          <span class="material-symbols-rounded">radio_button_unchecked</span>
        </div>
        <div class="stat-info">
          <div class="stat-count">{{ stats.todo }}</div>
          <div class="stat-label">To Do</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon progress">
          <span class="material-symbols-rounded">pending</span>
        </div>
        <div class="stat-info">
          <div class="stat-count">{{ stats.inProgress }}</div>
          <div class="stat-label">In Progress</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon done">
          <span class="material-symbols-rounded">check_circle</span>
        </div>
        <div class="stat-info">
          <div class="stat-count">{{ stats.done }}</div>
          <div class="stat-label">Done</div>
          <div v-if="stats.overdue > 0" class="overdue-count">{{ stats.overdue }} overdue</div>
        </div>
      </div>
    </div>

    <!-- Controls -->
    <div class="controls-bar">
      <div class="search-wrapper">
        <span class="material-symbols-rounded">search</span>
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="Search tasks..."
        />
      </div>
      <input
        v-model="filterDate"
        type="date"
        class="filter-input"
      />
      <select v-model="filterStatus" class="filter-select">
        <option value="">All Statuses</option>
        <option value="TODO">To Do</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="DONE">Done</option>
      </select>
      <button class="btn-primary" @click="startCreate">
        <span class="material-symbols-rounded">add</span>
        New Task
      </button>
    </div>

    <!-- Form -->
    <div v-if="showForm" class="form-overlay">
      <TaskForm
        :model-value="editingTask"
        @save="saveTask"
        @cancel="cancelForm"
      />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Loading tasks...</p>
    </div>

    <!-- Overdue Section -->
    <section v-if="!loading && overdueTasks.length > 0" class="overdue-section">
      <div class="overdue-header">
        <span class="material-symbols-rounded overdue-icon">warning</span>
        <span>Overdue</span>
        <span class="overdue-count">{{ overdueTasks.length }}</span>
      </div>
      <TransitionGroup name="task" tag="div" class="task-list overdue-list">
        <article
          v-for="task in overdueTasks"
          :key="task.id"
          class="task-card overdue"
          :class="`priority-${task.priority || 'MEDIUM'}`"
        >
          <div class="task-card-header">
            <div class="task-title">{{ task.title }}</div>
            <span class="badge" :class="task.status.toLowerCase()">
              {{ statusLabel(task.status) }}
            </span>
          </div>

          <div v-if="task.description" class="task-description">
            {{ task.description }}
          </div>

          <div class="task-footer">
            <div class="task-meta">
              <span class="task-due due-overdue">
                <span class="material-symbols-rounded">calendar_today</span>
                {{ dueLabel(task) }}
              </span>
              <span class="priority-label">
                <span class="priority-dot" :class="task.priority || 'MEDIUM'"></span>
                {{ priorityLabel(task.priority) }}
              </span>
            </div>

            <div class="task-actions">
              <button class="icon-btn edit" title="Edit" @click="startEdit(task)">
                <span class="material-symbols-rounded">edit</span>
              </button>
              <button
                class="icon-btn done-btn"
                title="Mark done"
                @click="quickStatus(task, 'DONE', $event)"
              >
                <span class="material-symbols-rounded">check_circle</span>
              </button>
              <button
                v-if="task.status === 'TODO'"
                class="icon-btn progress-btn"
                title="Move to in progress"
                @click="quickStatus(task, 'IN_PROGRESS')"
              >
                <span class="material-symbols-rounded">play_circle</span>
              </button>
              <button class="icon-btn delete" title="Delete" @click="confirmDelete(task)">
                <span class="material-symbols-rounded">delete</span>
              </button>
            </div>
          </div>
        </article>
      </TransitionGroup>
    </section>

    <!-- Active Tasks -->
    <TransitionGroup v-if="!loading" name="task" tag="div" class="task-list">
      <article
        v-for="task in activeTasks"
        :key="task.id"
        class="task-card"
        :class="[
          `priority-${task.priority || 'MEDIUM'}`,
          { overdue: isOverdue(task) }
        ]"
      >
        <div class="task-card-header">
          <div class="task-title">{{ task.title }}</div>
          <span class="badge" :class="task.status.toLowerCase()">
            {{ statusLabel(task.status) }}
          </span>
        </div>

        <div v-if="task.description" class="task-description">
          {{ task.description }}
        </div>

        <div class="task-footer">
          <div class="task-meta">
            <span class="task-due" :class="dueLabelClass(task)">
              <span class="material-symbols-rounded">calendar_today</span>
              {{ dueLabel(task) }}
            </span>
            <span class="priority-label">
              <span class="priority-dot" :class="task.priority || 'MEDIUM'"></span>
              {{ priorityLabel(task.priority) }}
            </span>
          </div>

          <div class="task-actions">
            <button class="icon-btn edit" title="Edit" @click="startEdit(task)">
              <span class="material-symbols-rounded">edit</span>
            </button>
            <button
              class="icon-btn done-btn"
              title="Mark done"
              @click="quickStatus(task, 'DONE', $event)"
            >
              <span class="material-symbols-rounded">check_circle</span>
            </button>
            <button
              v-if="task.status === 'TODO'"
              class="icon-btn progress-btn"
              title="Move to in progress"
              @click="quickStatus(task, 'IN_PROGRESS')"
            >
              <span class="material-symbols-rounded">play_circle</span>
            </button>
            <button class="icon-btn delete" title="Delete" @click="confirmDelete(task)">
              <span class="material-symbols-rounded">delete</span>
            </button>
          </div>
        </div>
      </article>
    </TransitionGroup>

    <!-- Completed Section -->
    <section v-if="!loading && doneTasks.length > 0" class="completed-section">
      <button class="completed-toggle" @click="showCompleted = !showCompleted">
        <span class="material-symbols-rounded toggle-chevron" :class="{ open: showCompleted }">chevron_right</span>
        <span class="material-symbols-rounded completed-icon">check_circle</span>
        <span>Completed</span>
        <span class="completed-count">{{ doneTasks.length }}</span>
      </button>

      <TransitionGroup v-if="showCompleted" name="task" tag="div" class="task-list completed-list">
        <article
          v-for="task in doneTasks"
          :key="task.id"
          class="task-card done-card"
          :class="`priority-${task.priority || 'MEDIUM'}`"
        >
          <div class="task-card-header">
            <div class="task-title">{{ task.title }}</div>
            <span class="badge done">{{ statusLabel(task.status) }}</span>
          </div>

          <div v-if="task.description" class="task-description">
            {{ task.description }}
          </div>

          <div class="task-footer">
            <div class="task-meta">
              <span class="task-due">
                <span class="material-symbols-rounded">calendar_today</span>
                {{ task.dueDate }}
              </span>
              <span class="priority-label">
                <span class="priority-dot" :class="task.priority || 'MEDIUM'"></span>
                {{ priorityLabel(task.priority) }}
              </span>
            </div>

            <div class="task-actions">
              <button class="icon-btn edit" title="Edit" @click="startEdit(task)">
                <span class="material-symbols-rounded">edit</span>
              </button>
              <button
                class="icon-btn progress-btn"
                title="Reopen"
                @click="quickStatus(task, 'TODO')"
              >
                <span class="material-symbols-rounded">undo</span>
              </button>
              <button class="icon-btn delete" title="Delete" @click="confirmDelete(task)">
                <span class="material-symbols-rounded">delete</span>
              </button>
            </div>
          </div>
        </article>
      </TransitionGroup>
    </section>

    <!-- Empty States -->
    <div
      v-if="!loading && filteredAndSorted.length === 0"
      class="empty-state"
    >
      <div class="empty-state-icon">
        <span class="material-symbols-rounded">
          {{ searchQuery || filterDate || filterStatus ? 'search_off' : 'add_task' }}
        </span>
      </div>
      <h3 v-if="searchQuery || filterDate || filterStatus">No matching tasks</h3>
      <h3 v-else>No tasks yet</h3>
      <p v-if="searchQuery || filterDate || filterStatus">
        Try adjusting your search or filters.
      </p>
      <p v-else>
        Click "New Task" to create your first task and get started.
      </p>
      <button v-if="!(searchQuery || filterDate || filterStatus)" class="btn-primary empty-cta" @click="startCreate">
        <span class="material-symbols-rounded">add</span>
        Create Your First Task
      </button>
    </div>
  </main>

  <footer class="app-footer">
    <p>Task Manager &middot; Stay organized, stay productive</p>
  </footer>
</template>
