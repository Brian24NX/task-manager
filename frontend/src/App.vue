<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import TaskForm from './components/TaskForm.vue'

const API = '/api/tasks'

const tasks = ref([])
const loading = ref(false)
const filterDate = ref('')
const filterStatus = ref('')
const searchQuery = ref('')
const editingTask = ref(null)
const showForm = ref(false)
const showDeleteConfirm = ref(false)
const taskToDelete = ref(null)
const toasts = ref([])
const stats = ref({ total: 0, todo: 0, inProgress: 0, done: 0, overdue: 0 })

let searchTimeout = null

const today = new Date().toISOString().slice(0, 10)

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
async function fetchTasks() {
  loading.value = true
  try {
    const query = filterDate.value ? `?dueDate=${filterDate.value}` : ''
    const res = await fetch(`${API}${query}`)
    if (!res.ok) throw new Error('Could not load tasks')
    tasks.value = await res.json()
  } catch (err) {
    addToast(err.message, 'error')
  } finally {
    loading.value = false
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
    if (res.ok) stats.value = await res.json()
  } catch (_) {
    // silently fail
  }
}

async function saveTask(task) {
  const isEditing = Boolean(editingTask.value)
  const url = isEditing ? `${API}/${editingTask.value.id}` : API
  const method = isEditing ? 'PUT' : 'POST'

  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(task)
    })
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      throw new Error(data.message || 'Could not save task')
    }
    addToast(isEditing ? 'Task updated successfully' : 'Task created successfully')
    cancelForm()
    await fetchTasks()
    await refreshStats()
  } catch (err) {
    addToast(err.message, 'error')
  }
}

async function quickStatus(task, status) {
  try {
    const res = await fetch(`${API}/${task.id}/status?status=${status}`, { method: 'PATCH' })
    if (!res.ok) throw new Error('Could not update status')
    const label = status === 'DONE' ? 'marked as done' : status === 'IN_PROGRESS' ? 'moved to in progress' : 'moved to to-do'
    addToast(`Task ${label}`)
    await fetchTasks()
    await refreshStats()
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
    await fetchTasks()
    await refreshStats()
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
  fetchTasks()
  refreshStats()
})
</script>

<template>
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

  <!-- Header -->
  <header class="app-header">
    <div class="header-content">
      <h1>
        <span class="material-symbols-rounded" style="font-size:28px">task_alt</span>
        Task Manager
      </h1>
      <p>Stay organized and get things done.</p>
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

    <!-- Task List -->
    <TransitionGroup v-if="!loading" name="task" tag="div" class="task-list">
      <article
        v-for="task in filteredAndSorted"
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
              v-if="task.status !== 'DONE'"
              class="icon-btn done-btn"
              title="Mark done"
              @click="quickStatus(task, 'DONE')"
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

    <!-- Empty States -->
    <div
      v-if="!loading && filteredAndSorted.length === 0"
      class="empty-state"
    >
      <span class="material-symbols-rounded">
        {{ searchQuery || filterDate || filterStatus ? 'search_off' : 'checklist' }}
      </span>
      <h3 v-if="searchQuery || filterDate || filterStatus">No matching tasks</h3>
      <h3 v-else>No tasks yet</h3>
      <p v-if="searchQuery || filterDate || filterStatus">
        Try adjusting your search or filters.
      </p>
      <p v-else>
        Click "New Task" to create your first task.
      </p>
    </div>
  </main>
</template>
