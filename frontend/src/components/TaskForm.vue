<script setup>
import { reactive, watch } from 'vue'

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
  status: 'TODO'
})

watch(
  () => props.modelValue,
  (task) => {
    form.title = task?.title ?? ''
    form.description = task?.description ?? ''
    form.dueDate = task?.dueDate ?? ''
    form.status = task?.status ?? 'TODO'
  },
  { immediate: true }
)

function submitForm() {
  emit('save', { ...form })
}
</script>

<template>
  <form class="task-form" @submit.prevent="submitForm">
    <h2>{{ modelValue ? 'Edit Task' : 'Add New Task' }}</h2>
    <label>
      Title
      <input v-model="form.title" maxlength="120" required />
    </label>
    <label>
      Description
      <textarea v-model="form.description" maxlength="500" rows="3" />
    </label>
    <label>
      Due date
      <input v-model="form.dueDate" type="date" required />
    </label>
    <label>
      Status
      <select v-model="form.status">
        <option value="TODO">To do</option>
        <option value="IN_PROGRESS">In progress</option>
        <option value="DONE">Done</option>
      </select>
    </label>
    <div class="actions">
      <button type="submit">{{ modelValue ? 'Update' : 'Create' }}</button>
      <button type="button" class="secondary" @click="emit('cancel')">Cancel</button>
    </div>
  </form>
</template>
