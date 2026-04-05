<script setup>
import { computed, onMounted, ref } from 'vue'
import TaskForm from './components/TaskForm.vue'

const apiBaseUrl = 'http://localhost:8080/api/tasks'

const tasks = ref([])
const loading = ref(false)
const error = ref('')
const filterDate = ref('')
const editingTask = ref(null)
const showForm = ref(false)

const today = new Date().toISOString().slice(0, 10)

const sortedTasks = computed(() =>
  [...tasks.value].sort((a, b) => {
    if (a.dueDate === b.dueDate) {
      return a.id - b.id
    }
    return a.dueDate.localeCompare(b.dueDate)
  })
)

async function fetchTasks() {
  loading.value = true
  error.value = ''
  try {
    const query = filterDate.value ? `?dueDate=${filterDate.value}` : ''
    const response = await fetch(`${apiBaseUrl}${query}`)
    if (!response.ok) {
      throw new Error('Could not load tasks')
    }
    tasks.value = await response.json()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function saveTask(task) {
  error.value = ''
  const payload = {
    title: task.title,
    description: task.description,
    dueDate: task.dueDate,
    status: task.status
  }

  const isEditing = Boolean(editingTask.value)
  const url = isEditing ? `${apiBaseUrl}/${editingTask.value.id}` : apiBaseUrl
  const method = isEditing ? 'PUT' : 'POST'

  try {
    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      throw new Error('Could not save task')
    }

    await fetchTasks()
    cancelEditing()
  } catch (err) {
    error.value = err.message
  }
}

async function quickStatusUpdate(task, status) {
  try {
    const response = await fetch(`${apiBaseUrl}/${task.id}/status?status=${status}`, {
      method: 'PATCH'
    })
    if (!response.ok) {
      throw new Error('Could not update status')
    }
    await fetchTasks()
  } catch (err) {
    error.value = err.message
  }
}

async function removeTask(taskId) {
  try {
    const response = await fetch(`${apiBaseUrl}/${taskId}`, { method: 'DELETE' })
    if (!response.ok) {
      throw new Error('Could not delete task')
    }
    await fetchTasks()
  } catch (err) {
    error.value = err.message
  }
}

function startCreate() {
  editingTask.value = null
  showForm.value = true
}

function startEdit(task) {
  editingTask.value = { ...task }
  showForm.value = true
}

function cancelEditing() {
  editingTask.value = null
  showForm.value = false
}

onMounted(fetchTasks)
</script>

<template>
  <main class="container">
    <header>
      <h1>Daily Task Manager</h1>
      <p>Track your everyday tasks and stay consistent.</p>
    </header>

    <section class="controls">
      <div>
        <label>Filter by date</label>
        <input v-model="filterDate" type="date" @change="fetchTasks" />
        <button class="secondary" @click="filterDate = ''; fetchTasks()">Clear</button>
      </div>
      <button @click="startCreate">+ Add Task</button>
    </section>

    <TaskForm
      v-if="showForm"
      :model-value="editingTask"
      @save="saveTask"
      @cancel="cancelEditing"
    />

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">Loading tasks...</p>

    <section v-if="!loading" class="task-list">
      <article v-for="task in sortedTasks" :key="task.id" class="task-card">
        <div class="task-header">
          <h3>{{ task.title }}</h3>
          <span class="badge" :class="task.status.toLowerCase()">{{ task.status }}</span>
        </div>
        <p>{{ task.description || 'No description yet.' }}</p>
        <small>Due: {{ task.dueDate }} <span v-if="task.dueDate === today">(today)</span></small>

        <div class="actions">
          <button class="secondary" @click="startEdit(task)">Edit</button>
          <button class="danger" @click="removeTask(task.id)">Delete</button>
          <button
            v-if="task.status !== 'DONE'"
            class="success"
            @click="quickStatusUpdate(task, 'DONE')"
          >
            Mark done
          </button>
        </div>
      </article>

      <p v-if="!sortedTasks.length" class="empty">No tasks found for this view.</p>
    </section>
  </main>
</template>
