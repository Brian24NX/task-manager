<script setup>
import { reactive, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['save', 'cancel'])

const form = reactive({
  title: '',
  description: '',
  dueDate: '',
  status: 'TODO',
  priority: 'MEDIUM'
})

const errors = reactive({
  title: '',
  dueDate: ''
})

const submitting = ref(false)

watch(
  () => props.modelValue,
  (task) => {
    form.title = task?.title ?? ''
    form.description = task?.description ?? ''
    form.dueDate = task?.dueDate ?? ''
    form.status = task?.status ?? 'TODO'
    form.priority = task?.priority ?? 'MEDIUM'
    errors.title = ''
    errors.dueDate = ''
  },
  { immediate: true }
)

function validate() {
  let valid = true
  errors.title = ''
  errors.dueDate = ''

  if (!form.title.trim()) {
    errors.title = 'Title is required'
    valid = false
  }
  if (!form.dueDate) {
    errors.dueDate = 'Due date is required'
    valid = false
  }
  return valid
}

async function submitForm() {
  if (!validate()) return
  submitting.value = true
  emit('save', { ...form })
  // Parent will handle closing the form
  setTimeout(() => { submitting.value = false }, 500)
}
</script>

<template>
  <div class="task-form-card">
    <div class="form-header">
      <span class="material-symbols-rounded">
        {{ modelValue ? 'edit_note' : 'add_task' }}
      </span>
      <h2>{{ modelValue ? 'Edit Task' : 'New Task' }}</h2>
    </div>

    <form class="form-body" @submit.prevent="submitForm">
      <div class="form-group">
        <label for="task-title">Title</label>
        <input
          id="task-title"
          v-model="form.title"
          type="text"
          maxlength="120"
          placeholder="What needs to be done?"
          :class="{ error: errors.title }"
        />
        <div v-if="errors.title" class="field-error">{{ errors.title }}</div>
      </div>

      <div class="form-group">
        <label for="task-desc">Description</label>
        <textarea
          id="task-desc"
          v-model="form.description"
          maxlength="500"
          rows="3"
          placeholder="Add more details..."
        />
      </div>

      <div class="form-row">
        <div class="form-group">
          <label for="task-date">Due Date</label>
          <input
            id="task-date"
            v-model="form.dueDate"
            type="date"
            :class="{ error: errors.dueDate }"
          />
          <div v-if="errors.dueDate" class="field-error">{{ errors.dueDate }}</div>
        </div>

        <div class="form-group">
          <label for="task-status">Status</label>
          <select id="task-status" v-model="form.status">
            <option value="TODO">To Do</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="DONE">Done</option>
          </select>
        </div>

        <div class="form-group">
          <label for="task-priority">Priority</label>
          <select id="task-priority" v-model="form.priority">
            <option value="LOW">&#x1F7E2; Low</option>
            <option value="MEDIUM">&#x1F535; Medium</option>
            <option value="HIGH">&#x1F7E0; High</option>
            <option value="URGENT">&#x1F534; Urgent</option>
          </select>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn-cancel" @click="emit('cancel')">
          Cancel
        </button>
        <button type="submit" class="btn-submit" :disabled="submitting">
          <span class="material-symbols-rounded" style="font-size:18px">
            {{ submitting ? 'hourglass_empty' : 'save' }}
          </span>
          {{ modelValue ? 'Update Task' : 'Create Task' }}
        </button>
      </div>
    </form>
  </div>
</template>
